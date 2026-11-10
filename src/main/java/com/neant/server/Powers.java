package com.neant.server;

import com.neant.NeantConfig;
import com.neant.net.VfxPacket;
import com.neant.net.VfxType;
import com.neant.registry.ModSounds;
import com.neant.util.TargetUtil;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Les pouvoirs des cinq outils du Neant (point d'entree appele par les items). */
public final class Powers {
    public static final int VOID_COLOR = 0xB06CFF;

    // ------------------------------------------------------------------ epee

    public static void swordHit(ServerLevel level, LivingEntity attacker, LivingEntity target) {
        Vec3 t = target.position().add(0, target.getBbHeight() * 0.5, 0);
        Vec3 look = attacker.getLookAngle();
        VfxPacket.of(VfxType.SLASH, t.subtract(look.scale(2.4))).to(t.add(look.scale(2.4)))
                .duration(14).param(0.4F).color(VOID_COLOR).send(level);
        ModSounds.play(level, t, ModSounds.SLASH, 1.3F, 1.0F + level.random.nextFloat() * 0.4F);

        Abilities.hurt(level, target, attacker, 6.0F);
        int others = 0;
        for (LivingEntity e : Abilities.around(level, attacker, t, 6.0D)) {
            if (e == target) {
                continue;
            }
            others++;
            Abilities.hurt(level, e, attacker, 8.0F);
            Abilities.pull(e, t, 0.7D, 0.25D);
        }
        attacker.heal(1.5F + others);
    }

    public static boolean swordWave(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition().add(dir.scale(1.0D));
        double length = 36.0D;
        VfxPacket.of(VfxType.SLASH, start).to(start.add(dir.scale(length))).duration(28).param(1.0F)
                .color(VOID_COLOR).send(level);
        ModSounds.play(level, start, ModSounds.SLASH, 4.0F, 0.6F);
        ModSounds.play(level, start, ModSounds.BOOM, 2.5F, 1.4F);
        ServerEffects.add(level, new PowerEffects.Wave(player, start, dir, length));
        return true;
    }

    // ----------------------------------------------------------------- pioche

    public static boolean pickaxeBreak(ServerPlayer sp, BlockPos origin) {
        ServerLevel level = sp.serverLevel();
        BlockState first = level.getBlockState(origin);
        if (!first.is(BlockTags.MINEABLE_WITH_PICKAXE) || level.getBlockEntity(origin) != null) {
            return false;
        }
        Direction face = clickedFace(sp, level, origin);
        List<BlockPos> blocks = new ArrayList<>();
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                for (int depth = 0; depth < 3; depth++) {
                    int dx = -face.getStepX() * depth;
                    int dy = -face.getStepY() * depth;
                    int dz = -face.getStepZ() * depth;
                    BlockPos p = switch (face.getAxis()) {
                        case X -> origin.offset(dx, a, b);
                        case Y -> origin.offset(a, dy, b);
                        case Z -> origin.offset(a, b, dz);
                    };
                    BlockState s = level.getBlockState(p);
                    if (s.is(BlockTags.MINEABLE_WITH_PICKAXE) && Abilities.erasable(level, p, s)) {
                        blocks.add(p);
                    }
                }
            }
        }
        harvest(level, sp, blocks, Vec3.atCenterOf(origin));
        return true;
    }

    public static boolean drill(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 start = player.getEyePosition();
        double length = 46.0D;
        VfxPacket.of(VfxType.DRILL, start.add(dir.scale(1.5D))).to(start.add(dir.scale(length)))
                .duration(30).color(0x7FE8FF).send(level);
        ModSounds.play(level, start, ModSounds.DRILL, 4.0F, 1.0F);
        ModSounds.play(level, start, ModSounds.BOOM, 2.0F, 0.8F);
        ServerEffects.add(level, new PowerEffects.Drill(player, start, dir, length));
        return true;
    }

    // ------------------------------------------------------------------ pelle

    public static boolean shovelBreak(ServerPlayer sp, BlockPos origin) {
        ServerLevel level = sp.serverLevel();
        if (!level.getBlockState(origin).is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return false;
        }
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> blocks = new ArrayList<>();
        queue.add(origin);
        seen.add(origin);
        while (!queue.isEmpty() && blocks.size() < 180) {
            BlockPos p = queue.poll();
            BlockState s = level.getBlockState(p);
            if (!s.is(BlockTags.MINEABLE_WITH_SHOVEL) || !Abilities.erasable(level, p, s)) {
                continue;
            }
            blocks.add(p);
            for (BlockPos n : BlockPos.betweenClosed(p.offset(-1, -1, -1), p.offset(1, 1, 1))) {
                if (Math.abs(n.getX() - origin.getX()) <= 5 && Math.abs(n.getY() - origin.getY()) <= 5
                        && Math.abs(n.getZ() - origin.getZ()) <= 5 && seen.add(n.immutable())) {
                    queue.add(n.immutable());
                }
            }
        }
        harvest(level, sp, blocks, Vec3.atCenterOf(origin));
        return true;
    }

    public static boolean voidHole(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        Vec3 target = TargetUtil.targetPoint(player, 45.0D);
        Vec3 center = target.add(0, 1.0D, 0);
        VfxPacket.of(VfxType.BLACK_HOLE, center.add(0, 1.5D, 0)).duration(PowerEffects.VoidHole.DURATION)
                .param(2.4F).color(VOID_COLOR).send(level);
        ModSounds.play(level, center, ModSounds.VOID_HOLE, 5.0F, 0.9F);
        ServerEffects.add(level, new PowerEffects.VoidHole(player, center));
        return true;
    }

    // ------------------------------------------------------------------ hache

    public static boolean axeBreak(ServerPlayer sp, BlockPos origin) {
        ServerLevel level = sp.serverLevel();
        if (!level.getBlockState(origin).is(BlockTags.LOGS)) {
            return false;
        }
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> blocks = new ArrayList<>();
        queue.add(origin);
        seen.add(origin);
        while (!queue.isEmpty() && blocks.size() < 520) {
            BlockPos p = queue.poll();
            BlockState s = level.getBlockState(p);
            if (!s.is(BlockTags.LOGS) && !s.is(BlockTags.LEAVES)) {
                continue;
            }
            blocks.add(p);
            for (BlockPos n : BlockPos.betweenClosed(p.offset(-1, -1, -1), p.offset(1, 1, 1))) {
                if (Math.abs(n.getX() - origin.getX()) <= 10 && Math.abs(n.getZ() - origin.getZ()) <= 10
                        && n.getY() >= origin.getY() - 2 && n.getY() <= origin.getY() + 40 && seen.add(n.immutable())) {
                    queue.add(n.immutable());
                }
            }
        }
        harvest(level, sp, blocks, Vec3.atCenterOf(origin).add(0, 4, 0));
        return true;
    }

    public static boolean whirl(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        int duration = PowerEffects.Whirl.DURATION;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration + 10, 1, false, false));
        VfxPacket.of(VfxType.SPIN, player.position()).follow(player).duration(duration).color(VOID_COLOR).send(level);
        ModSounds.play(level, player.position(), ModSounds.WHIRL, 4.0F, 1.0F);
        ServerEffects.add(level, new PowerEffects.Whirl(player));
        return true;
    }

    // ---------------------------------------------------------------- briquet

    public static boolean lighterAir(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        fireWave(level, player, TargetUtil.targetPoint(player, 40.0D));
        return true;
    }

    public static void fireWave(ServerLevel level, ServerPlayer player, Vec3 center) {
        VfxPacket.of(VfxType.FIREWAVE, center).duration(PowerEffects.FireWave.DURATION + 8)
                .param(PowerEffects.FireWave.MAX_RADIUS).color(0xC07CFF).send(level);
        ModSounds.play(level, center, ModSounds.FLAME, 5.0F, 1.0F);
        ModSounds.play(level, center, ModSounds.BOOM, 2.0F, 1.1F);
        ServerEffects.add(level, new PowerEffects.FireWave(player, center));
    }

    // --------------------------------------------------------------- communs

    /** Casse les blocs avec leurs drops, aspire les objets vers le joueur, et joue l'implosion. */
    private static void harvest(ServerLevel level, ServerPlayer sp, List<BlockPos> blocks, Vec3 center) {
        if (blocks.isEmpty()) {
            return;
        }
        for (BlockPos p : blocks) {
            level.destroyBlock(p, true, sp);
        }
        VfxPacket.of(VfxType.IMPLODE, center).to(sp.position().add(0, 1.0D, 0)).duration(26)
                .param(Math.min(blocks.size(), 200)).color(VOID_COLOR).send(level);
        ModSounds.play(level, center, ModSounds.BOOM, 1.4F, 1.7F);
        AABB box = new AABB(center, center).inflate(14.0D);
        ServerEffects.delay(level, 2, () -> Abilities.magnet(level, sp, box));
        ServerEffects.delay(level, 8, () -> Abilities.magnet(level, sp, box));
    }

    private static Direction clickedFace(ServerPlayer sp, ServerLevel level, BlockPos origin) {
        Vec3 eye = sp.getEyePosition();
        Vec3 end = eye.add(sp.getLookAngle().scale(8.0D));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, sp));
        if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(origin)) {
            return hit.getDirection();
        }
        return sp.getDirection().getOpposite();
    }

    public static boolean terrainDestruction() {
        return NeantConfig.TERRAIN_DESTRUCTION.get();
    }

    private Powers() {}
}
