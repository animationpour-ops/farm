package com.neant.server;

import com.neant.net.VfxPacket;
import com.neant.net.VfxType;
import com.neant.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.ChatFormatting;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Faille du Neant : un portail s'ouvre dans le ciel, des sbires en tombent par vagues,
 * puis le Gardien apparait. Tuer tout le monde = des fragments de Neant.
 */
public final class RiftEvent implements ServerEffects.Timed {
    public static final String TAG_MINION = "neant_minion";
    public static final String TAG_GUARDIAN = "neant_guardian";

    private static final int OPEN_TICKS = 70;
    private static final int SPAWN_INTERVAL = 28;
    private static final int MAX_MINIONS = 14;
    private static final int TIMEOUT = 20 * 60 * 7;
    private static final double PORTAL_HEIGHT = 18.0D;

    private static RiftEvent current;

    private final Vec3 ground;
    private final Vec3 portal;
    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("boss.neant.rift"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private final List<UUID> minions = new ArrayList<>();
    private UUID guardian;
    private int age;
    private int spawned;
    private int alone;
    private boolean guardianSpawned;
    private boolean finished;

    private RiftEvent(Vec3 ground) {
        this.ground = ground;
        this.portal = ground.add(0, PORTAL_HEIGHT, 0);
        this.bar.setProgress(1.0F);
    }

    public static boolean isActive() {
        return current != null;
    }

    /** Ouvre une faille au sol (pres de x, z). Renvoie false s'il y en a deja une. */
    public static boolean open(ServerLevel level, double x, double z) {
        if (current != null) {
            return false;
        }
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(x, 0, z));
        current = new RiftEvent(new Vec3(x, top.getY(), z));
        ServerEffects.add(level, current);
        return true;
    }

    // --------------------------------------------------------------------- deroulement

    @Override
    public boolean tick(ServerLevel level) {
        if (age % 200 == 0) {
            // renvoye regulierement : les joueurs qui arrivent plus tard le voient aussi (le client prolonge l'effet)
            VfxPacket.of(VfxType.RIFT, portal).duration(260).color(0xB06CFF).send(level);
        }
        if (age == 0) {
            ModSounds.play(level, portal, ModSounds.RIFT_OPEN, 12.0F, 1.0F);
            for (ServerPlayer p : level.players()) {
                if (p.position().distanceToSqr(ground) < 200.0D * 200.0D) {
                    p.sendSystemMessage(Component.translatable("message.neant.rift.open")
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
                }
            }
        }
        updateBar(level);

        // vagues de sbires
        if (age >= OPEN_TICKS && spawned < MAX_MINIONS && (age - OPEN_TICKS) % SPAWN_INTERVAL == 0) {
            spawnMinion(level);
        }
        if (age % 20 == 0) {
            for (UUID id : minions) {
                Entity e = level.getEntity(id);
                if (e != null && e.isAlive()) {
                    VfxPacket.of(VfxType.AURA, e.position()).follow(e).duration(40).param(1.0F).color(0xFF3A6A).send(level);
                }
            }
        }

        int alive = aliveMinions(level);
        if (!guardianSpawned && spawned >= MAX_MINIONS && (alive <= 3 || age > OPEN_TICKS + MAX_MINIONS * SPAWN_INTERVAL + 700)) {
            spawnGuardian(level);
        }

        if (guardianSpawned) {
            Entity g = guardian == null ? null : level.getEntity(guardian);
            if (g == null || !g.isAlive()) {
                finished = true;
                return true;
            }
            if (age % 100 == 60) {
                pulse(level, (LivingEntity) g);
            }
            if (age % 20 == 0) {
                VfxPacket.of(VfxType.AURA, g.position()).follow(g).duration(40).param(1.0F).color(0xFF3A6A).send(level);
            }
        }

        // abandon : personne a proximite, ou trop long
        boolean anyone = false;
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.position().distanceToSqr(ground) < 160.0D * 160.0D) {
                anyone = true;
                break;
            }
        }
        alone = anyone ? 0 : alone + 1;
        age++;
        if (alone > 400 || age > TIMEOUT) {
            discardAll(level);
            return true;
        }
        return false;
    }

    @Override
    public void end(ServerLevel level) {
        current = null;
        bar.removeAllPlayers();
        VfxPacket cancel = VfxPacket.of(VfxType.CANCEL, portal).param(VfxType.RIFT.ordinal());
        cancel.send(level);
        VfxPacket.of(VfxType.COLLAPSE, portal).duration(46).color(0xB06CFF).send(level);
        ModSounds.play(level, portal, ModSounds.BOOM, 8.0F, 0.6F);
        if (finished) {
            for (ServerPlayer p : level.players()) {
                if (p.position().distanceToSqr(ground) < 200.0D * 200.0D) {
                    p.sendSystemMessage(Component.translatable("message.neant.rift.closed")
                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
                }
            }
        }
    }

    // -------------------------------------------------------------------------- aides

    private int aliveMinions(ServerLevel level) {
        int n = 0;
        for (UUID id : minions) {
            Entity e = level.getEntity(id);
            if (e != null && e.isAlive()) {
                n++;
            }
        }
        return n;
    }

    private void updateBar(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            boolean near = !p.isSpectator() && p.position().distanceToSqr(ground) < 90.0D * 90.0D;
            boolean in = bar.getPlayers().contains(p);
            if (near && !in) {
                bar.addPlayer(p);
            } else if (!near && in) {
                bar.removePlayer(p);
            }
        }
        if (guardianSpawned) {
            Entity g = guardian == null ? null : level.getEntity(guardian);
            if (g instanceof LivingEntity le) {
                bar.setProgress(Math.max(0.0F, Math.min(1.0F, le.getHealth() / le.getMaxHealth())));
            }
        } else {
            int dead = spawned - aliveMinions(level);
            bar.setProgress(Math.max(0.0F, 1.0F - dead / (float) MAX_MINIONS));
        }
    }

    private void spawnMinion(ServerLevel level) {
        EntityType<?>[] types = {EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.ENDERMAN,
                EntityType.ZOMBIE, EntityType.SKELETON};
        EntityType<?> type = types[level.random.nextInt(types.length)];
        double ang = level.random.nextDouble() * Math.PI * 2.0D;
        double rad = 7.0D + level.random.nextDouble() * 9.0D;
        double x = ground.x + Math.cos(ang) * rad;
        double z = ground.z + Math.sin(ang) * rad;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(x, 0, z));
        Entity created = type.create(level);
        if (!(created instanceof Mob mob)) {
            return;
        }
        mob.moveTo(x, top.getY(), z, level.random.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(top), MobSpawnType.EVENT, null, null);
        mob.getPersistentData().putBoolean(TAG_MINION, true);
        scale(mob, Attributes.MAX_HEALTH, 2.5D);
        scale(mob, Attributes.ATTACK_DAMAGE, 1.6D);
        scale(mob, Attributes.MOVEMENT_SPEED, 1.15D);
        mob.setHealth(mob.getMaxHealth());
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, TIMEOUT, 0, false, false));
        mob.setCustomName(Component.translatable("entity.neant.minion").withStyle(ChatFormatting.LIGHT_PURPLE));
        mob.setCustomNameVisible(true);
        mob.setGlowingTag(true);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        Player target = level.getNearestPlayer(mob, 90.0D);
        if (target != null && !target.isSpectator() && !target.isCreative()) {
            mob.setTarget(target);
        }
        minions.add(mob.getUUID());
        spawned++;

        Vec3 at = new Vec3(x, top.getY() + 1.0D, z);
        VfxPacket.of(VfxType.ARC, portal).to(at).duration(18).color(0xC07CFF).send(level);
        VfxPacket.of(VfxType.SMITE, at).duration(34).param(0.6F).color(0xB06CFF).send(level);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 3.0F, 1.6F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    private void spawnGuardian(ServerLevel level) {
        guardianSpawned = true;
        Entity created = EntityType.RAVAGER.create(level);
        if (!(created instanceof Mob mob)) {
            finished = true;
            return;
        }
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(ground));
        mob.moveTo(ground.x, top.getY(), ground.z, 0.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(top), MobSpawnType.EVENT, null, null);
        mob.getPersistentData().putBoolean(TAG_GUARDIAN, true);
        scale(mob, Attributes.MAX_HEALTH, 8.0D);
        scale(mob, Attributes.ATTACK_DAMAGE, 1.4D);
        scale(mob, Attributes.MOVEMENT_SPEED, 1.25D);
        scale(mob, Attributes.KNOCKBACK_RESISTANCE, 1.0D);
        AttributeInstance kb = mob.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            kb.setBaseValue(1.0D);
        }
        mob.setHealth(mob.getMaxHealth());
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, TIMEOUT, 0, false, false));
        mob.setCustomName(Component.translatable("entity.neant.guardian").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        mob.setCustomNameVisible(true);
        mob.setGlowingTag(true);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
        Player target = level.getNearestPlayer(mob, 90.0D);
        if (target != null && !target.isSpectator() && !target.isCreative()) {
            mob.setTarget(target);
        }
        guardian = mob.getUUID();

        bar.setName(Component.translatable("entity.neant.guardian"));
        bar.setColor(BossEvent.BossBarColor.RED);
        bar.setProgress(1.0F);

        Vec3 at = new Vec3(ground.x, top.getY() + 1.0D, ground.z);
        VfxPacket.of(VfxType.SMITE, at).duration(34).param(1.8F).color(0xB06CFF).send(level);
        VfxPacket.of(VfxType.PULSE, at).duration(40).param(14.0F).color(0xFF3A6A).send(level);
        ModSounds.play(level, at, ModSounds.BOOM, 8.0F, 0.7F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 6.0F, 0.8F);
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceToSqr(ground) < 200.0D * 200.0D) {
                p.sendSystemMessage(Component.translatable("message.neant.rift.guardian")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            }
        }
    }

    /** Pulsation du gardien : onde de choc qui blesse et projette les joueurs proches. */
    private void pulse(ServerLevel level, LivingEntity guardianEntity) {
        Vec3 c = guardianEntity.position().add(0, 0.6D, 0);
        VfxPacket.of(VfxType.PULSE, c).duration(40).param(12.0F).color(0xFF3A6A).send(level);
        ModSounds.play(level, c, ModSounds.PULSE, 6.0F, 1.0F);
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.position().distanceToSqr(c) < 12.0D * 12.0D) {
                Abilities.hurt(level, p, guardianEntity, 10.0F);
                Abilities.knock(p, c, 1.8D, 0.8D);
            }
        }
    }

    private void discardAll(ServerLevel level) {
        for (UUID id : minions) {
            Entity e = level.getEntity(id);
            if (e != null) {
                e.discard();
            }
        }
        if (guardian != null) {
            Entity e = level.getEntity(guardian);
            if (e != null) {
                e.discard();
            }
        }
    }

    private static void scale(Mob mob, Attribute attribute, double factor) {
        AttributeInstance inst = mob.getAttribute(attribute);
        if (inst != null) {
            inst.setBaseValue(inst.getBaseValue() * factor);
        }
    }
}
