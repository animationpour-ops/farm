package com.neant.server;

import com.neant.item.NeantArmorItem;
import com.neant.net.VfxPacket;
import com.neant.net.VfxType;
import com.neant.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Armure du Neant : bonus par piece, et avec les 4 pieces : aura destructrice, sprint bulldozer,
 * immunite aux chutes et au vide (le Neant te ramene au dernier sol sur).
 */
@Mod.EventBusSubscriber(modid = "neant")
public final class ArmorEvents {
    private static final String SAFE_TAG = "neant_safe_pos";

    private static void give(ServerPlayer p, MobEffect effect, int duration, int amp) {
        p.addEffect(new MobEffectInstance(effect, duration, amp, true, false));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp)) {
            return;
        }
        int pieces = NeantArmorItem.piecesWorn(sp);
        if (pieces == 0) {
            return;
        }
        ServerLevel level = sp.serverLevel();
        int tick = sp.tickCount;

        if (tick % 10 == 0 && sp.onGround() && !sp.isInWaterOrBubble() && !sp.isInLava() && !sp.isPassenger()) {
            rememberSafe(level, sp);
        }

        if (tick % 20 == 0) {
            if (NeantArmorItem.wears(sp, EquipmentSlot.HEAD)) {
                give(sp, MobEffects.NIGHT_VISION, 260, 0);
                give(sp, MobEffects.WATER_BREATHING, 60, 0);
            }
            if (NeantArmorItem.wears(sp, EquipmentSlot.CHEST)) {
                give(sp, MobEffects.DAMAGE_RESISTANCE, 60, 0);
            }
            if (NeantArmorItem.wears(sp, EquipmentSlot.LEGS)) {
                give(sp, MobEffects.MOVEMENT_SPEED, 60, 0);
            }
            if (NeantArmorItem.wears(sp, EquipmentSlot.FEET)) {
                give(sp, MobEffects.JUMP, 60, 1);
            }
        }

        if (pieces < 4) {
            return;
        }

        // --- bonus d'ensemble ---
        if (tick % 20 == 0) {
            give(sp, MobEffects.FIRE_RESISTANCE, 60, 0);
            give(sp, MobEffects.DAMAGE_RESISTANCE, 60, 1);
            give(sp, MobEffects.REGENERATION, 60, 0);
            VfxPacket.of(VfxType.AURA, sp.position()).follow(sp).duration(40).param(0.0F).color(0xB06CFF).send(level);
        }

        // aura : brule et attire doucement tout ce qui est proche
        if (tick % 10 == 0) {
            Vec3 c = sp.position().add(0, 1.0D, 0);
            for (LivingEntity e : Abilities.around(level, sp, c, 6.5D)) {
                Abilities.hurt(level, e, sp, 3.0F);
                Abilities.pull(e, c, 0.18D, 0.0D);
            }
        }

        // sprint bulldozer
        if (sp.isSprinting()) {
            if (tick % 5 == 0) {
                give(sp, MobEffects.MOVEMENT_SPEED, 20, 2);
                Vec3 c = sp.position().add(0, 1.0D, 0);
                for (LivingEntity e : Abilities.around(level, sp, c, 2.6D)) {
                    Abilities.hurt(level, e, sp, 7.0F);
                    Abilities.knock(e, sp.position(), 1.4D, 0.5D);
                }
            }
            if (tick % 3 == 0) {
                VfxPacket.of(VfxType.TRAIL, sp.position()).duration(26).color(0xB06CFF).send(level);
            }
        }
    }

    private static void rememberSafe(ServerLevel level, ServerPlayer sp) {
        CompoundTag t = new CompoundTag();
        t.putDouble("x", sp.getX());
        t.putDouble("y", sp.getY());
        t.putDouble("z", sp.getZ());
        t.putString("dim", level.dimension().location().toString());
        sp.getPersistentData().put(SAFE_TAG, t);
    }

    /** Chutes et vide : annules pour qui porte l'armure complete. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || !NeantArmorItem.hasFullSet(sp)) {
            return;
        }
        DamageSource src = event.getSource();
        if (src.is(DamageTypes.FALL)) {
            event.setCanceled(true);
        } else if (src.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            event.setCanceled(true);
            blinkToSafety(sp);
        }
    }

    private static void blinkToSafety(ServerPlayer sp) {
        ServerLevel level = sp.serverLevel();
        double x;
        double y;
        double z;
        CompoundTag t = sp.getPersistentData().getCompound(SAFE_TAG);
        if (t.contains("x") && t.getString("dim").equals(level.dimension().location().toString())) {
            x = t.getDouble("x");
            y = t.getDouble("y");
            z = t.getDouble("z");
        } else {
            BlockPos spawn = level.getSharedSpawnPos();
            x = spawn.getX() + 0.5D;
            y = spawn.getY() + 1.0D;
            z = spawn.getZ() + 0.5D;
        }
        sp.teleportTo(x, y, z);
        sp.setDeltaMovement(Vec3.ZERO);
        sp.fallDistance = 0.0F;
        sp.hurtMarked = true;
        Vec3 pos = new Vec3(x, y, z);
        VfxPacket.of(VfxType.BLINK, pos).duration(34).color(0xB06CFF).send(level);
        ModSounds.play(level, pos, ModSounds.BLINK, 3.0F, 1.0F);
    }

    private ArmorEvents() {}
}
