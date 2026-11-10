package com.neant.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class TargetUtil {

    /** Point vise par l'entite (bloc ou entite), ou le bout de la portee si rien n'est touche. */
    public static Vec3 targetPoint(LivingEntity source, double range) {
        Level level = source.level();
        Vec3 eye = source.getEyePosition();
        Vec3 look = source.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
        Vec3 blockEnd = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        AABB box = source.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(level, source, eye, blockEnd, box,
                e -> !e.isSpectator() && e.isPickable());
        return entity != null ? entity.getLocation() : blockEnd;
    }

    /** Descend depuis un point jusqu'au premier sol solide (max 96 blocs). */
    public static Vec3 groundBelow(Level level, Vec3 from, Entity context) {
        Vec3 end = from.add(0, -96, 0);
        BlockHitResult hit = level.clip(new ClipContext(from, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context));
        return hit.getType() == HitResult.Type.MISS ? from : hit.getLocation();
    }

    /** Vise le sol : si le regard touche l'air, on projette vers le bas. */
    public static Vec3 groundTarget(LivingEntity source, double range) {
        Vec3 p = targetPoint(source, range);
        BlockPos pos = BlockPos.containing(p);
        if (source.level().getBlockState(pos).isAir() && source.level().getBlockState(pos.below()).isAir()) {
            return groundBelow(source.level(), p.add(0, 1, 0), source);
        }
        return p;
    }

    private TargetUtil() {}
}
