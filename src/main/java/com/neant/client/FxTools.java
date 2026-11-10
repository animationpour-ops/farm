package com.neant.client;

import com.neant.net.VfxPacket;
import net.minecraft.world.phys.Vec3;

/** Effets des outils : onde de l'epee, foreuse, tourbillon de lames, vague de feu, implosion des blocs. */
public final class FxTools {

    /** Deux vecteurs unitaires perpendiculaires a dir : [0] horizontal, [1] vers le haut. */
    private static Vec3[] basis(Vec3 dir) {
        Vec3 u = dir.cross(Fx.AXIS_Y);
        u = u.lengthSqr() < 1.0E-6D ? Fx.AXIS_X : u.normalize();
        Vec3 v = u.cross(dir).normalize();
        return new Vec3[] {u, v};
    }

    // ------------------------------------------------------------------ epee

    /** Croissant de Neant qui file en ligne droite en laissant des images rémanentes. */
    public static final class Slash extends Fx {
        public Slash(VfxPacket p) {
            super(p);
        }

        private float travel() {
            return Math.max(4.0F, duration * 0.5F);
        }

        private Vec3 head(Vec3 dir, double len, float t) {
            return origin.add(dir.scale(len * easeOut(t / travel())));
        }

        private void crescent(VfxDraw d, Vec3 c, Vec3 u, Vec3 v, float r, float alpha, int rgb) {
            Vec3 ac = c.subtract(v.scale(r * 0.55D));
            double a0 = Math.PI / 2.0D - 1.15D;
            double a1 = Math.PI / 2.0D + 1.15D;
            d.arc(ac, u, v, r, r * 0.55D, a0, a1, rgb, alpha * 0.35F, 28);
            d.arc(ac, u, v, r, r * 0.22D, a0 + 0.1D, a1 - 0.1D, rgb, alpha * 0.9F, 28);
            d.arc(ac, u, v, r, r * 0.07D, a0 + 0.28D, a1 - 0.28D, 0xFFFFFF, alpha, 28);
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            Vec3 line = target.subtract(origin);
            double len = line.length();
            if (len < 0.01D) {
                return;
            }
            Vec3 dir = line.scale(1.0D / len);
            Vec3[] b = basis(dir);
            float s = param;
            float r = 1.5F + 4.5F * s;
            float fade = 1.0F - Rnd.ramp(t, travel() * 0.6F, duration);
            double tilt = Math.toRadians(-28.0D + Rnd.s(seed, 1) * 14.0D);
            Vec3 u = b[0].scale(Math.cos(tilt)).add(b[1].scale(Math.sin(tilt)));
            Vec3 v = b[0].scale(-Math.sin(tilt)).add(b[1].scale(Math.cos(tilt)));
            Vec3 head = head(dir, len, t);

            d.type(VfxRenderTypes.LINE_ADD);
            for (int k = 5; k >= 0; k--) {
                float tk = t - k * 1.3F;
                if (tk <= 0.0F) {
                    continue;
                }
                Vec3 c = head(dir, len, tk);
                float a = fade * (k == 0 ? 1.0F : 0.5F * (1.0F - k / 6.0F));
                crescent(d, c, u, v, r * (1.0F - 0.03F * k), a, color);
            }
            // trainee entre le depart et la tete
            d.ribbon(origin, head, 0.0F, r * 0.35F, color, 0.0F, 0.5F * fade);

            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(head, r * 1.6F, color, 0.45F * fade);
            d.glow(head, r * 0.7F, 0xFFFFFF, 0.5F * fade);

            d.type(VfxRenderTypes.STAR_ADD);
            int n = (int) (10 + 26 * s);
            for (int i = 0; i < n; i++) {
                double along = Rnd.f(seed, i + 40) * len * easeOut(t / travel());
                float life = Rnd.range(seed, i + 80, 6.0F, 16.0F);
                float age = t - (float) (along / Math.max(0.1D, len)) * travel() * 0.5F;
                if (age < 0.0F || age > life) {
                    continue;
                }
                Vec3 pos = origin.add(dir.scale(along))
                        .add(u.scale(Rnd.s(seed, i + 120) * r * 0.9D))
                        .add(v.scale(Rnd.s(seed, i + 160) * r * 0.9D + age * 0.05D));
                d.glow(pos, 0.45F * (1.0F - age / life), t * 0.2F + i, 0xE9D8FF, 1.0F - age / life);
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            if (param < 0.7F) {
                return 0.0F;
            }
            Vec3 line = target.subtract(origin);
            double len = line.length();
            if (len < 0.01D) {
                return 0.0F;
            }
            Vec3 h = head(line.scale(1.0D / len), len, t);
            return 0.5F * param * (1.0F - Rnd.ramp(t, 0.0F, 16.0F)) * falloff(cam, h, 50.0F);
        }
    }

    // --------------------------------------------------------------- pioche

    /** Foreuse : un faisceau qui perce l'horizon, vis en helice et rings qui avancent. */
    public static final class Drill extends Fx {
        public Drill(VfxPacket p) {
            super(p);
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            Vec3 line = target.subtract(origin);
            double len = line.length();
            if (len < 0.01D) {
                return;
            }
            Vec3 dir = line.scale(1.0D / len);
            Vec3[] b = basis(dir);
            float life = Rnd.ramp(t, 0.0F, 3.0F) * (1.0F - Rnd.ramp(t, duration - 10.0F, duration));
            if (life <= 0.0F) {
                return;
            }
            double reach = len * easeOut(t / 7.0F);
            Vec3 end = origin.add(dir.scale(reach));
            float ts = t / 20.0F;

            d.type(VfxRenderTypes.LINE_ADD);
            d.ribbon(origin, end, 3.4F, 3.4F, color, 0.28F * life, 0.28F * life);
            d.ribbon(origin, end, 1.4F, 1.4F, color, 0.8F * life, 0.8F * life);
            d.ribbon(origin, end, 0.45F, 0.45F, 0xFFFFFF, life, life);

            // vis : deux brins qui tournent autour du faisceau
            for (int strand = 0; strand < 2; strand++) {
                Vec3 prev = null;
                for (double s = 0.0D; s <= reach; s += 0.7D) {
                    double ang = s * 1.1D - t * 0.9D + strand * Math.PI;
                    Vec3 p = origin.add(dir.scale(s)).add(b[0].scale(Math.cos(ang) * 1.15D))
                            .add(b[1].scale(Math.sin(ang) * 1.15D));
                    if (prev != null) {
                        float fadeEnd = (float) (1.0D - s / len * 0.6D);
                        d.ribbon(prev, p, 0.28F, 0.28F, strand == 0 ? 0xFFFFFF : color, 0.9F * life * fadeEnd,
                                0.9F * life * fadeEnd);
                    }
                    prev = p;
                }
            }

            // anneaux qui avancent le long du faisceau
            int rings = (int) (reach / 2.2D);
            for (int i = 0; i < rings; i++) {
                double s = i * 2.2D + fract(t * 0.3F) * 2.2D;
                if (s > reach) {
                    continue;
                }
                Vec3 p = origin.add(dir.scale(s));
                float a = 0.55F * life * (1.0F - (float) (s / len) * 0.7F);
                d.ring(p, b[0], b[1], 1.5D + 0.5D * Math.sin(i * 0.9D + ts * 6.0D), 0.4D, color, a, 18);
            }

            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(origin, 3.6F, color, 0.7F * life);
            d.glow(origin, 1.6F, 0xFFFFFF, life);
            d.glow(end, 6.0F + 1.2F * (float) Math.sin(t * 1.3F), color, 0.55F * life);
            d.glow(end, 2.6F, 0xFFFFFF, 0.9F * life);

            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < 90; i++) {
                float cyc = 8.0F + Rnd.f(seed, i + 10) * 8.0F;
                float phase = fract(t / cyc + Rnd.f(seed, i + 20));
                double along = Rnd.f(seed, i + 30) * reach;
                double ang = Rnd.f(seed, i + 40) * Math.PI * 2.0D;
                double rad = 1.0D + phase * 5.0D;
                Vec3 p = origin.add(dir.scale(along)).add(b[0].scale(Math.cos(ang) * rad))
                        .add(b[1].scale(Math.sin(ang) * rad - phase * 1.5D));
                d.glow(p, 0.5F * (1.0F - phase), t * 0.3F + i, i % 3 == 0 ? 0xFFFFFF : color,
                        (float) Math.sin(phase * Math.PI) * life);
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            float life = Rnd.ramp(t, 0.0F, 3.0F) * (1.0F - Rnd.ramp(t, duration - 10.0F, duration));
            return 0.5F * life * falloff(cam, origin, 60.0F);
        }
    }

    // ---------------------------------------------------------------- hache

    /** Tourbillon de lames autour du joueur. */
    public static final class Spin extends Fx {
        public Spin(VfxPacket p) {
            super(p);
        }

        private float life(float t) {
            return Rnd.ramp(t, 0.0F, 6.0F) * (1.0F - Rnd.ramp(t, duration - 8.0F, duration));
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            float life = life(t);
            if (life <= 0.0F) {
                return;
            }
            Vec3 c = pos(partial).add(0, 1.0D, 0);
            float ts = t / 20.0F;

            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(c, 5.0F, color, 0.22F * life);

            d.type(VfxRenderTypes.LINE_ADD);
            // trois etages de lames qui tournent a des vitesses differentes
            for (int layer = 0; layer < 3; layer++) {
                double radius = 3.2D + 1.5D * layer;
                double h = -0.7D + 0.7D * layer;
                double speed = (0.55D - 0.12D * layer) * (layer == 1 ? -1.0D : 1.0D);
                for (int k = 0; k < 4; k++) {
                    double th = t * speed + k * Math.PI / 2.0D + layer * 0.6D;
                    Vec3 cc = c.add(0, h, 0);
                    d.arc(cc, AXIS_X, AXIS_Z, radius, 0.9D, th, th + 1.25D, color, 0.45F * life, 16);
                    d.arc(cc, AXIS_X, AXIS_Z, radius, 0.36D, th + 0.1D, th + 1.2D, 0xFFFFFF, 0.9F * life, 16);
                }
            }
            // ondes qui s'ecartent
            for (int j = 0; j < 4; j++) {
                float phase = fract(ts * 1.6F + j * 0.25F);
                d.ring(c.add(0, -0.6D + phase * 1.6D, 0), AXIS_X, AXIS_Z, 1.0D + 6.5D * phase,
                        0.5D * (1.0F - phase) + 0.1D, color, (1.0F - phase) * 0.7F * life, 40);
            }

            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < 80; i++) {
                float h = fract(ts * 0.9F + Rnd.f(seed, i)) * 7.0F - 1.5F;
                double rad = 1.0D + (h + 1.5D) * 0.55D;
                double ang = Rnd.f(seed, i + 100) * Math.PI * 2.0D + t * 0.45D + h * 0.5D;
                Vec3 p = c.add(Math.cos(ang) * rad, h, Math.sin(ang) * rad);
                d.glow(p, 0.42F, t * 0.2F + i, i % 4 == 0 ? 0xFFFFFF : 0xC9A0FF,
                        0.85F * life * (1.0F - Math.abs(h - 2.0F) / 4.0F));
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            return 0.16F * life(t) * falloff(cam, origin, 30.0F);
        }
    }

    // --------------------------------------------------------------- briquet

    /** Vague de flammes du Neant : un mur de feu violet qui s'etend depuis l'impact. */
    public static final class FireWave extends Fx {
        private static final float TRAVEL = 44.0F;

        public FireWave(VfxPacket p) {
            super(p);
        }

        private double radiusAt(float t) {
            return param * easeOut(t / TRAVEL);
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            Vec3 c = origin.add(0, 0.15D, 0);
            double radius = radiusAt(t);
            float fade = 1.0F - Rnd.ramp(t, TRAVEL * 0.55F, duration);

            d.type(VfxRenderTypes.LINE_ADD);
            d.ring(c, AXIS_X, AXIS_Z, radius, 3.2D, color, 0.55F * fade, 96);
            d.ring(c, AXIS_X, AXIS_Z, radius, 1.0D, 0xFFFFFF, 0.8F * fade, 96);
            d.ring(c, AXIS_X, AXIS_Z, radius * 0.72D, 0.9D, color, 0.35F * fade, 72);

            // mur de flammes : colonnes qui vacillent le long de l'anneau
            d.type(VfxRenderTypes.GLOW_ADD);
            int cols = 72;
            for (int i = 0; i < cols; i++) {
                double ang = i * Math.PI * 2.0D / cols + Rnd.s(seed, i) * 0.05D;
                float flick = 0.6F + 0.4F * (float) Math.sin(t * 0.9F + i * 1.7F);
                double rr = radius - Rnd.f(seed, i + 100) * 2.4D;
                if (rr <= 0.2D) {
                    continue;
                }
                Vec3 base = c.add(Math.cos(ang) * rr, 0, Math.sin(ang) * rr);
                float h = (1.6F + 2.8F * Rnd.f(seed, i + 200)) * flick * (1.0F + 0.5F * param / 17.0F);
                d.pillar(base, h, 1.7F, color, 0.55F * fade, 0.0F);
                d.pillar(base, h * 0.6F, 0.7F, 0xFFFFFF, 0.5F * fade, 0.0F);
            }
            float blast = 1.0F - Rnd.ramp(t, 0.0F, 14.0F);
            d.glow(c.add(0, 1.0D, 0), 16.0F * blast, color, 0.8F * blast);
            d.glow(c.add(0, 1.0D, 0), 7.0F * blast, 0xFFFFFF, blast);

            // braises laissees dans le sillage de l'onde
            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < 140; i++) {
                float born = Rnd.f(seed, i + 300) * TRAVEL;
                float age = t - born;
                float life = Rnd.range(seed, i + 400, 14.0F, 30.0F);
                if (age < 0.0F || age > life) {
                    continue;
                }
                double ang = Rnd.f(seed, i + 500) * Math.PI * 2.0D;
                double rr = radiusAt(born) * Rnd.range(seed, i + 600, 0.7F, 1.0F);
                Vec3 p = c.add(Math.cos(ang) * rr, 0.3D + age * 0.12D + Math.sin(age * 0.4D + i) * 0.2D,
                        Math.sin(ang) * rr);
                d.glow(p, 0.55F * (1.0F - age / life), t * 0.2F + i, i % 3 == 0 ? 0xFFFFFF : 0xC9A0FF,
                        1.0F - age / life);
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            return 0.55F * (1.0F - Rnd.ramp(t, 0.0F, 24.0F)) * falloff(cam, origin, 55.0F);
        }

        @Override
        public float flash(Vec3 cam, float t) {
            return 0.35F * (1.0F - Rnd.ramp(t, 0.0F, 6.0F)) * falloff(cam, origin, 30.0F);
        }

        @Override
        public int flashColor() {
            return 0xE0B8FF;
        }
    }

    // --------------------------------------------------------------- implosion

    /** Les blocs effaces partent en eclats vers le joueur : ca donne l'impression que le Neant les aspire. */
    public static final class Implode extends Fx {
        public Implode(VfxPacket p) {
            super(p);
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            int count = (int) Math.max(14.0F, Math.min(70.0F, param * 0.8F));
            Vec3 to = target;
            float shock = 1.0F - Rnd.ramp(t, 0.0F, 10.0F);

            d.type(VfxRenderTypes.LINE_ADD);
            d.ring(origin, d.left, d.up, 0.8D + 3.2D * (1.0F - shock), 0.5D * shock + 0.08D, color, 0.8F * shock, 32);

            for (int i = 0; i < count; i++) {
                float delay = Rnd.f(seed, i + 10) * 8.0F;
                float flight = Rnd.range(seed, i + 20, 9.0F, 16.0F);
                float p = Rnd.clamp01((t - delay) / flight);
                if (p <= 0.0F || p >= 1.0F) {
                    continue;
                }
                Vec3 start = origin.add(sphere(seed, i).scale(Rnd.range(seed, i + 30, 0.6F, 2.8F)));
                Vec3 mid = start.add(to).scale(0.5D).add(0, 1.4D + Rnd.f(seed, i + 40) * 1.6D, 0);
                float pe = easeIn(p) * 0.6F + p * 0.4F;
                Vec3 pos = bez(start, mid, to, pe);
                Vec3 prev = bez(start, mid, to, Math.max(0.0F, pe - 0.1F));
                d.ribbon(prev, pos, 0.0F, 0.22F, color, 0.0F, 0.9F * (1.0F - p * 0.5F));
            }

            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < count; i++) {
                float delay = Rnd.f(seed, i + 10) * 8.0F;
                float flight = Rnd.range(seed, i + 20, 9.0F, 16.0F);
                float p = Rnd.clamp01((t - delay) / flight);
                if (p <= 0.0F || p >= 1.0F) {
                    continue;
                }
                Vec3 start = origin.add(sphere(seed, i).scale(Rnd.range(seed, i + 30, 0.6F, 2.8F)));
                Vec3 mid = start.add(to).scale(0.5D).add(0, 1.4D + Rnd.f(seed, i + 40) * 1.6D, 0);
                float pe = easeIn(p) * 0.6F + p * 0.4F;
                d.glow(bez(start, mid, to, pe), 0.5F * (1.0F - p * 0.4F), t * 0.3F + i,
                        i % 3 == 0 ? 0xFFFFFF : color, 1.0F - p * p);
            }

            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(origin, 5.0F * shock, color, 0.7F * shock);
            d.glow(origin, 2.2F * shock, 0xFFFFFF, shock);
            float arrive = Rnd.ramp(t, 8.0F, 16.0F) * (1.0F - Rnd.ramp(t, 16.0F, duration));
            d.glow(to, 2.4F * arrive, color, 0.6F * arrive);
        }

        private static Vec3 bez(Vec3 a, Vec3 b, Vec3 c, float t) {
            double k = 1.0D - t;
            return a.scale(k * k).add(b.scale(2.0D * k * t)).add(c.scale(t * t));
        }

        @Override
        public float shake(Vec3 cam, float t) {
            return Math.min(0.2F, param / 400.0F) * (1.0F - Rnd.ramp(t, 0.0F, 8.0F)) * falloff(cam, origin, 24.0F);
        }
    }

    private FxTools() {}
}
