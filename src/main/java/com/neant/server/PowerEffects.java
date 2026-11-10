package com.neant.server;

import com.neant.net.VfxPacket;
import com.neant.net.VfxType;
import com.neant.registry.ModSounds;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Effets qui durent plusieurs ticks : onde de l'epee, foreuse, trou noir, tourbillon, vague de feu. */
public final class PowerEffects {

    // ------------------------------------------------------------ onde de l'epee

    public static final class Wave implements ServerEffects.Timed {
        private final ServerPlayer owner;
        private final Vec3 start;
        private final Vec3 dir;
        private final double length;
        private final Set<UUID> hit = new HashSet<>();
        private double dist;

        public Wave(ServerPlayer owner, Vec3 start, Vec3 dir, double length) {
            this.owner = owner;
            this.start = start;
            this.dir = dir;
            this.length = length;
        }

        @Override
        public boolean tick(ServerLevel level) {
            double stepEnd = Math.min(length, dist + 3.6D);
            for (double d = dist; d <= stepEnd; d += 1.2D) {
                Vec3 p = start.add(dir.scale(d));
                for (LivingEntity e : Abilities.around(level, owner, p, 3.4D)) {
                    if (!hit.add(e.getUUID())) {
                        continue;
                    }
                    Abilities.hurt(level, e, owner, 45.0F);
                    e.setDeltaMovement(e.getDeltaMovement().add(dir.x * 1.6D, 0.6D, dir.z * 1.6D));
                    e.hurtMarked = true;
                }
                if (Powers.terrainDestruction()) {
                    Abilities.eraseSphere(level, p, 2.2D);
                }
            }
            dist = stepEnd + 0.01D;
            return dist >= length;
        }
    }

    // ----------------------------------------------------------------- foreuse

    public static final class Drill implements ServerEffects.Timed {
        private final ServerPlayer owner;
        private final Vec3 start;
        private final Vec3 dir;
        private final double length;
        private final Set<UUID> hit = new HashSet<>();
        private double dist;

        public Drill(ServerPlayer owner, Vec3 start, Vec3 dir, double length) {
            this.owner = owner;
            this.start = start;
            this.dir = dir;
            this.length = length;
        }

        @Override
        public boolean tick(ServerLevel level) {
            double stepEnd = Math.min(length, dist + 3.4D);
            for (double d = dist; d <= stepEnd; d += 1.0D) {
                Vec3 p = start.add(dir.scale(d));
                for (LivingEntity e : Abilities.around(level, owner, p, 3.0D)) {
                    if (hit.add(e.getUUID())) {
                        Abilities.hurt(level, e, owner, 30.0F);
                        e.setDeltaMovement(e.getDeltaMovement().add(dir.x * 1.2D, 0.5D, dir.z * 1.2D));
                        e.hurtMarked = true;
                    }
                }
                if (Powers.terrainDestruction()) {
                    BlockPos c = BlockPos.containing(p);
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dy = -2; dy <= 2; dy++) {
                            for (int dz = -2; dz <= 2; dz++) {
                                if (dx * dx + dy * dy + dz * dz > 5.6D) {
                                    continue;
                                }
                                BlockPos bp = c.offset(dx, dy, dz);
                                BlockState s = level.getBlockState(bp);
                                if (s.getFluidState().isEmpty() && Abilities.erasable(level, bp, s)) {
                                    level.destroyBlock(bp, true, owner);
                                }
                            }
                        }
                    }
                }
            }
            Abilities.magnet(level, owner, new AABB(start, start.add(dir.scale(stepEnd))).inflate(7.0D));
            dist = stepEnd + 0.01D;
            return dist >= length;
        }

        @Override
        public void end(ServerLevel level) {
            AABB tunnel = new AABB(start, start.add(dir.scale(length))).inflate(7.0D);
            ServerEffects.delay(level, 3, () -> Abilities.magnet(level, owner, tunnel));
        }
    }

    // ----------------------------------------------------------------- trou noir

    public static final class VoidHole implements ServerEffects.Timed {
        public static final int DURATION = 70;
        private static final double MAX_RADIUS = 7.5D;
        private static final double PULL_RADIUS = 20.0D;

        private final ServerPlayer owner;
        private final Vec3 center;
        private int age;
        private double prevRadius;
        private boolean finished;

        public VoidHole(ServerPlayer owner, Vec3 center) {
            this.owner = owner;
            this.center = center;
        }

        private boolean affects(Entity e) {
            if (e == owner || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand) {
                return false;
            }
            return !(e instanceof Player p && p.isCreative() && p.getAbilities().flying);
        }

        @Override
        public boolean tick(ServerLevel level) {
            double k = Math.min(1.0D, age / 40.0D);
            double radius = MAX_RADIUS * k * k * (3.0D - 2.0D * k);

            if (Powers.terrainDestruction() && radius > prevRadius) {
                int r = (int) Math.ceil(radius);
                BlockPos c = BlockPos.containing(center);
                double lo = prevRadius * prevRadius;
                double hi = radius * radius;
                for (int dx = -r; dx <= r; dx++) {
                    for (int dy = -r; dy <= r; dy++) {
                        for (int dz = -r; dz <= r; dz++) {
                            double d2 = dx * dx + dy * dy + dz * dz;
                            if (d2 <= lo || d2 > hi) {
                                continue;
                            }
                            BlockPos p = c.offset(dx, dy, dz);
                            BlockState s = level.getBlockState(p);
                            if (Abilities.erasable(level, p, s)) {
                                Abilities.erase(level, p);
                            }
                        }
                    }
                }
            }
            prevRadius = radius;

            AABB box = new AABB(center, center).inflate(PULL_RADIUS);
            for (Entity e : level.getEntities((Entity) null, box, this::affects)) {
                double dist = e.position().distanceTo(center);
                if (dist > PULL_RADIUS || dist < 0.7D) {
                    continue;
                }
                Abilities.pull(e, center, Math.min(1.1D, 0.25D + 1.0D * (1.0D - dist / PULL_RADIUS)), 0.0D);
                if (e instanceof LivingEntity le && dist < 8.0D && age % 10 == 0) {
                    Abilities.hurt(level, le, owner, 8.0F);
                }
            }
            age++;
            finished = age >= DURATION;
            return finished;
        }

        @Override
        public void end(ServerLevel level) {
            if (!finished) {
                return;
            }
            VfxPacket.of(VfxType.COLLAPSE, center).duration(46).color(Powers.VOID_COLOR).send(level);
            ModSounds.play(level, center, ModSounds.BOOM, 8.0F, 0.5F);
            for (LivingEntity e : Abilities.around(level, owner, center, 14.0D)) {
                Abilities.hurt(level, e, owner, 60.0F);
                Abilities.knock(e, center, 2.6D, 1.0D);
            }
            AABB box = new AABB(center, center).inflate(14.0D);
            for (Entity e : level.getEntities((Entity) null, box, x -> affects(x) && !(x instanceof LivingEntity))) {
                Abilities.knock(e, center, 1.8D, 0.6D);
            }
        }
    }

    // ---------------------------------------------------------------- tourbillon

    public static final class Whirl implements ServerEffects.Timed {
        public static final int DURATION = 60;
        private final ServerPlayer owner;
        private int age;

        public Whirl(ServerPlayer owner) {
            this.owner = owner;
        }

        @Override
        public boolean tick(ServerLevel level) {
            if (owner.isRemoved() || !owner.isAlive()) {
                return true;
            }
            Vec3 c = owner.position().add(0, 1.0D, 0);
            if (age % 3 == 0) {
                for (LivingEntity e : Abilities.around(level, owner, c, 7.0D)) {
                    Abilities.hurt(level, e, owner, 10.0F);
                    Vec3 d = e.position().subtract(owner.position());
                    Vec3 tangent = new Vec3(-d.z, 0, d.x);
                    tangent = tangent.lengthSqr() < 1.0E-4D ? Vec3.ZERO : tangent.normalize().scale(0.9D);
                    Vec3 inward = d.lengthSqr() < 1.0E-4D ? Vec3.ZERO : d.normalize().scale(-0.25D);
                    e.setDeltaMovement(tangent.add(inward).add(0, 0.4D, 0));
                    e.hurtMarked = true;
                }
            }
            if (age % 4 == 0 && Powers.terrainDestruction()) {
                BlockPos o = owner.blockPosition();
                for (int dx = -5; dx <= 5; dx++) {
                    for (int dy = -3; dy <= 5; dy++) {
                        for (int dz = -5; dz <= 5; dz++) {
                            if (dx * dx + dy * dy + dz * dz > 30) {
                                continue;
                            }
                            BlockPos p = o.offset(dx, dy, dz);
                            BlockState s = level.getBlockState(p);
                            if (s.is(BlockTags.LEAVES) || s.getBlock() instanceof BushBlock) {
                                level.destroyBlock(p, true, owner);
                            }
                        }
                    }
                }
            }
            if (age % 10 == 0) {
                Abilities.magnet(level, owner, new AABB(c, c).inflate(9.0D));
            }
            age++;
            return age >= DURATION;
        }
    }

    // ------------------------------------------------------------- vague de feu

    public static final class FireWave implements ServerEffects.Timed {
        public static final int DURATION = 44;
        public static final float MAX_RADIUS = 17.0F;
        private static final double BAND = 3.0D;

        private final ServerPlayer owner;
        private final Vec3 center;
        private final Set<UUID> hit = new HashSet<>();
        private int age;

        public FireWave(ServerPlayer owner, Vec3 center) {
            this.owner = owner;
            this.center = center;
        }

        @Override
        public boolean tick(ServerLevel level) {
            float x = Math.min(1.0F, age / (float) DURATION);
            float inv = 1.0F - x;
            double radius = MAX_RADIUS * (1.0F - inv * inv * inv);

            for (LivingEntity e : Abilities.around(level, owner, center, radius + 1.0D)) {
                double dh = Math.hypot(e.getX() - center.x, e.getZ() - center.z);
                if (dh >= radius - BAND && dh <= radius + 1.0D && Math.abs(e.getY() - center.y) <= 8.0D
                        && hit.add(e.getUUID())) {
                    Abilities.hurt(level, e, owner, 12.0F);
                    e.setSecondsOnFire(15);
                }
            }

            if (Powers.terrainDestruction()) {
                int r = (int) Math.ceil(radius + 1.0D);
                int cy = (int) Math.floor(center.y) + 4;
                for (int ix = -r; ix <= r; ix++) {
                    for (int iz = -r; iz <= r; iz++) {
                        double dh = Math.hypot(ix, iz);
                        if (dh < radius - BAND || dh > radius + 1.0D || level.random.nextFloat() > 0.35F) {
                            continue;
                        }
                        BlockPos column = new BlockPos((int) Math.floor(center.x) + ix, cy, (int) Math.floor(center.z) + iz);
                        for (int dy = 0; dy < 12; dy++) {
                            BlockPos q = column.below(dy);
                            BlockState s = level.getBlockState(q);
                            if (s.is(Blocks.TNT)) {
                                level.removeBlock(q, false);
                                TntBlock.explode(level, q);
                                break;
                            }
                            if (level.isEmptyBlock(q)) {
                                BlockState fire = Blocks.FIRE.defaultBlockState();
                                if (fire.canSurvive(level, q)) {
                                    level.setBlock(q, fire, 11);
                                    break;
                                }
                            } else if (s.getFluidState().isEmpty()) {
                                break;
                            }
                        }
                    }
                }
            }
            age++;
            return age >= DURATION;
        }
    }

    private PowerEffects() {}
}
