package com.neant.client;

import com.neant.net.VfxPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Effets de l'armure : aura (aussi utilisee pour les creatures de la faille), trainee de sprint, teleportation. */
public final class FxArmor {

    // -------------------------------------------------------------------- aura

    /**
     * Aura qui suit une entite. param 0 = armure du Neant (violet, zone de degats visible au sol),
     * param 1 = creature de la faille (rouge, plus petite).
     */
    public static final class Aura extends Fx {
        public Aura(VfxPacket p) {
            super(p);
        }

        private float life(float t) {
            return Rnd.ramp(t, 0.0F, 12.0F) * (1.0F - Rnd.ramp(t, duration - 10.0F, duration));
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            float life = life(t);
            if (life <= 0.0F) {
                return;
            }
            boolean minion = param >= 0.5F;
            Entity e = followed();
            float height = e != null ? e.getBbHeight() : 1.8F;
            Vec3 feet = pos(partial);
            Vec3 mid = feet.add(0, height * 0.5D, 0);
            float ts = t / 20.0F;
            int main = minion ? 0xFF3A6A : 0xB06CFF;
            int soft = minion ? 0xFF8AA6 : 0xE9D8FF;

            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(mid, height * (minion ? 2.4F : 3.2F), main, (minion ? 0.2F : 0.17F) * life);
            if (!minion) {
                d.glow(mid.add(0, height * 0.55D, 0), 1.6F, 0xFFFFFF, 0.25F * life);
            }

            d.type(VfxRenderTypes.LINE_ADD);
            // anneaux qui montent le long du corps
            for (int j = 0; j < 3; j++) {
                float phase = fract(ts * 0.8F + j / 3.0F);
                double rad = (minion ? 0.7D : 0.95D) + 0.35D * Math.sin(phase * Math.PI);
                d.ring(feet.add(0, phase * (height + 0.4D) - 0.2D, 0), AXIS_X, AXIS_Z, rad, 0.16D, main,
                        (float) Math.sin(phase * Math.PI) * 0.8F * life, 28);
            }
            if (!minion) {
                // arcs sombres en orbite
                for (int k = 0; k < 3; k++) {
                    double a0 = ts * (2.2D + 0.5D * k) + k * 2.1D;
                    d.arc(feet.add(0, height * (0.25D + 0.3D * k), 0), AXIS_X, AXIS_Z, 1.2D + 0.15D * k, 0.22D, a0,
                            a0 + 1.6D, soft, 0.75F * life, 16);
                }
                // zone d'aura : le rayon des degats est visible au sol, et pulse comme les degats
                double zone = 6.5D;
                Vec3 ground = feet.add(0, 0.1D, 0);
                d.ring(ground, AXIS_X, AXIS_Z, zone, 0.3D, main, 0.4F * life, 72);
                float pp = fract(t / 10.0F);
                d.ring(ground, AXIS_X, AXIS_Z, zone * pp, 0.4D * (1.0F - pp) + 0.06D, main, 0.5F * (1.0F - pp) * life, 56);
                for (int k = 0; k < 6; k++) {
                    double a0 = -ts * 0.7D + k * Math.PI / 3.0D;
                    d.arc(ground, AXIS_X, AXIS_Z, zone - 0.5D, 0.5D, a0, a0 + 0.5D, soft, 0.5F * life, 10);
                }
            } else {
                d.ring(feet.add(0, 0.1D, 0), AXIS_X, AXIS_Z, 1.2D + 0.2D * Math.sin(ts * 5.0D), 0.14D, main,
                        0.6F * life, 24);
            }

            // particules qui s'elevent en spirale autour de l'entite
            d.type(VfxRenderTypes.STAR_ADD);
            int n = minion ? 18 : 34;
            for (int i = 0; i < n; i++) {
                float lp = 1.1F + Rnd.f(seed, i + 10) * 1.0F;
                float phase = fract(ts / lp + Rnd.f(seed, i + 20));
                double ang = Rnd.f(seed, i + 30) * Math.PI * 2.0D + phase * 4.0D;
                double rad = (minion ? 0.6D : 0.9D) * (0.7D + Rnd.f(seed, i + 40) * 0.6D);
                Vec3 p = feet.add(Math.cos(ang) * rad, phase * (height + 0.8D), Math.sin(ang) * rad);
                d.glow(p, 0.32F * (1.0F - phase * 0.4F), ts * 3.0F + i, i % 3 == 0 ? 0xFFFFFF : main,
                        (float) Math.sin(phase * Math.PI) * life);
            }
        }
    }

    // ------------------------------------------------------------------- trainee

    /** Empreinte de sprint : un eclat de lumiere au sol, un pilier qui s'evanouit, des etincelles. */
    public static final class Trail extends Fx {
        public Trail(VfxPacket p) {
            super(p);
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            float x = Rnd.clamp01(t / duration);
            float a = (1.0F - x) * (1.0F - x);
            Vec3 c = origin;

            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(c.add(0, 0.15D, 0), 2.6F * (1.0F - 0.4F * x), color, 0.55F * a);
            d.pillar(c, 2.2D * (1.0F - 0.3F * x), 1.1F * (1.0F - x) + 0.1F, color, 0.5F * a, 0.0F);
            d.glow(c.add(0, 1.0D, 0), 1.6F * (1.0F - x), 0xFFFFFF, 0.25F * a);

            d.type(VfxRenderTypes.LINE_ADD);
            d.ring(c.add(0, 0.08D, 0), AXIS_X, AXIS_Z, 0.5D + 1.9D * easeOut(x), 0.3D * (1.0F - x) + 0.05D, color,
                    0.7F * a, 28);

            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < 8; i++) {
                double ang = Rnd.f(seed, i) * Math.PI * 2.0D;
                double rad = 0.3D + Rnd.f(seed, i + 10) * 0.7D;
                Vec3 p = c.add(Math.cos(ang) * rad, 0.2D + x * (1.0D + Rnd.f(seed, i + 20) * 1.6D),
                        Math.sin(ang) * rad);
                d.glow(p, 0.3F * (1.0F - x), t * 0.4F + i, i % 2 == 0 ? 0xFFFFFF : 0xC9A0FF, a);
            }
        }
    }

    // ------------------------------------------------------------------ teleportation

    /** Le Neant te recrache au sol : colonne de lumiere, particules qui se condensent puis explosent. */
    public static final class Blink extends Fx {
        public Blink(VfxPacket p) {
            super(p);
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            Vec3 c = origin;
            Vec3 mid = c.add(0, 1.0D, 0);
            float x = Rnd.clamp01(t / duration);
            float fade = 1.0F - Rnd.ramp(t, 8.0F, duration);
            float ts = t / 20.0F;

            d.type(VfxRenderTypes.GLOW_ADD);
            float burst = 1.0F - Rnd.ramp(t, 0.0F, 10.0F);
            d.glow(mid, 10.0F * burst, color, 0.8F * burst);
            d.glow(mid, 4.0F * burst, 0xFFFFFF, burst);
            d.pillar(c, 30.0D, 2.4F * fade, color, 0.65F * fade, 0.0F);
            d.pillar(c, 18.0D, 0.9F * fade, 0xFFFFFF, 0.6F * fade, 0.0F);

            d.type(VfxRenderTypes.LINE_ADD);
            for (int k = 0; k < 3; k++) {
                float tk = t - k * 3.0F;
                if (tk < 0.0F) {
                    continue;
                }
                double r = 0.5D + 6.0D * easeOut(tk / 22.0F);
                float a = (1.0F - Rnd.ramp(tk, 4.0F, 26.0F)) * 0.9F;
                d.ring(c.add(0, 0.1D + k * 0.05D, 0), AXIS_X, AXIS_Z, r, 0.5D * (1.0F - tk / 30.0F) + 0.08D,
                        k == 0 ? 0xFFFFFF : color, a, 48);
            }
            d.ring(mid, d.left, d.up, 1.0D + 2.5D * easeOut(x), 0.3D * (1.0F - x) + 0.05D, 0xE9D8FF, 0.8F * fade, 40);

            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < 60; i++) {
                Vec3 dir = sphere(seed, i);
                double speed = 3.0D + Rnd.f(seed, i + 100) * 9.0D;
                Vec3 p = mid.add(dir.scale(speed * easeOut(ts / 1.1F)));
                float life = 1.0F - Rnd.ramp(ts, 0.3F, 1.1F);
                d.glow(p, 0.45F * life, t * 0.3F + i, i % 3 == 0 ? 0xFFFFFF : color, life);
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            return 0.35F * (1.0F - Rnd.ramp(t, 0.0F, 14.0F)) * falloff(cam, origin, 24.0F);
        }

        @Override
        public float flash(Vec3 cam, float t) {
            return 0.7F * (1.0F - Rnd.ramp(t, 0.0F, 9.0F)) * falloff(cam, origin, 6.0F);
        }

        @Override
        public int flashColor() {
            return 0xE9D8FF;
        }
    }

    private FxArmor() {}
}
