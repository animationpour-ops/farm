package com.neant.client;

import com.neant.net.VfxPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/** Effets du vide : trou noir avec disque d'accretion, puis effondrement. */
public final class FxVoid {

    private static final int[] HOT = {0xFFF1C9, 0xFFC27A, 0xFF7A4A, 0xC04DFF, 0x6A3CFF, 0x3A2A9A};

    public static final class BlackHole extends Fx {
        public BlackHole(VfxPacket p) {
            super(p);
        }

        private float life(float t) {
            return Rnd.ramp(t, 0.0F, 10.0F) * (1.0F - Rnd.ramp(t, duration - 6.0F, duration));
        }

        private float radius(float t) {
            float grow = Rnd.smooth(Rnd.ramp(t, 0.0F, 16.0F));
            float shrink = 1.0F - Rnd.smooth(Rnd.ramp(t, duration - 8.0F, duration));
            return 1.35F * Math.max(0.3F, param) * grow * shrink * (1.0F + 0.04F * (float) Math.sin(t * 0.7F));
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            float r = radius(t);
            float life = life(t);
            if (r <= 0.02F || life <= 0.0F) {
                return;
            }
            Vec3 c = origin;
            Vec3 w = d.cam.subtract(c);
            w = w.lengthSqr() < 1.0E-6D ? AXIS_Y : w.normalize();
            // Le disque s'incline vers la camera pour rester lisible sous tous les angles.
            Vec3 n = AXIS_Y.scale(0.62D).add(w.scale(0.55D)).normalize();
            Vec3 u = n.cross(AXIS_Y);
            u = u.lengthSqr() < 1.0E-6D ? AXIS_X : u.normalize();
            Vec3 v = n.cross(u).normalize();
            double phi = Math.atan2(v.dot(w), u.dot(w));
            float ts = t / 20.0F;

            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(c, r * 9.0F, 0x6A3CFF, 0.16F * life);
            d.glow(c, r * 5.0F, 0x8A4DFF, 0.22F * life);

            d.type(VfxRenderTypes.LINE_ADD);
            for (int k = 0; k < 6; k++) {
                d.arc(c, u, v, r * (1.7D + 0.55D * k), r * (0.55D + 0.14D * k), phi + Math.PI / 2.0D,
                        phi + Math.PI * 1.5D, HOT[k], (0.62F - 0.07F * k) * life, 40);
            }

            d.type(VfxRenderTypes.DISC_DARK);
            d.glow(c, r * 2.0F, 0x000000, life);

            d.type(VfxRenderTypes.LINE_ADD);
            for (int k = 0; k < 6; k++) {
                d.arc(c, u, v, r * (1.7D + 0.55D * k), r * (0.55D + 0.14D * k), phi - Math.PI / 2.0D,
                        phi + Math.PI / 2.0D, HOT[k], (0.62F - 0.07F * k) * life, 40);
            }
            d.ring(c, d.left, d.up, r * 1.05D, r * 0.26D, 0xFFE9FF, 0.9F * life, 48);
            d.ring(c, d.left, d.up, r * 1.12D, r * 0.6D, 0xB06CFF, 0.5F * life, 48);

            for (int i = 0; i < 60; i++) {
                float f = Rnd.f(seed, i);
                float rr = r * (1.6F + 5.6F * f * f);
                float omega = 2.6F / (float) Math.pow(rr / r, 1.15D);
                double theta = Rnd.f(seed, i + 100) * Math.PI * 2.0D + omega * ts;
                double len = Rnd.range(seed, i + 150, 0.3F, 1.0F);
                double wd = r * Rnd.range(seed, i + 180, 0.05F, 0.16F);
                int ci = Math.min(5, (int) ((rr / r - 1.6F) / 5.6F * 5.99F));
                float a = Rnd.range(seed, i + 210, 0.35F, 0.8F) * life
                        * (0.55F + 0.45F * (float) Math.sin(ts * 3.0F + i));
                Vec3 cc = c.add(n.scale(Rnd.s(seed, i + 240) * 0.12D * r));
                d.arc(cc, u, v, rr, wd, theta, theta + len, HOT[Math.max(0, ci)], a, 8);
            }

            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < 80; i++) {
                float lp = 1.4F + Rnd.f(seed, i + 300) * 1.2F;
                float phase = fract(ts / lp + Rnd.f(seed, i + 400));
                double rr = r + 19.0D * Math.max(0.3F, param) * Math.pow(1.0F - phase, 1.7D);
                double ang = Rnd.f(seed, i + 500) * Math.PI * 2.0D + phase * 5.5D;
                Vec3 pos = c.add(u.scale(Math.cos(ang) * rr)).add(v.scale(Math.sin(ang) * rr))
                        .add(n.scale(Rnd.s(seed, i + 600) * rr * 0.35D));
                float a = (float) Math.sin(phase * Math.PI) * life;
                d.glow(pos, 0.3F + 0.25F * phase, ts * 2.0F + i, phase > 0.6F ? 0xFFD9A0 : 0xC9A0FF, a);
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            return 0.28F * falloff(cam, origin, 32.0F) * life(t);
        }

        @Override
        public void overlay(GuiGraphics g, int w, int h, float t, Vec3 cam) {
            double dist = cam.distanceTo(origin);
            if (dist >= 16.0D) {
                return;
            }
            float k = (float) (1.0D - dist / 16.0D);
            int alpha = (int) (k * k * 0.5F * life(t) * 255.0F);
            if (alpha > 0) {
                g.fill(0, 0, w, h, (alpha << 24) | 0x120020);
            }
        }
    }

    /** Effondrement : onde de choc spherique violette, eclat blanc, gerbe d'etincelles. */
    public static final class Collapse extends Fx {
        public Collapse(VfxPacket p) {
            super(p);
        }

        private Vec3 spark(int i, float ts) {
            double speed = 8.0D + Rnd.f(seed, i + 500) * 18.0D;
            return origin.add(sphere(seed, i).scale(speed * ts));
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            Vec3 c = origin;
            float ts = t / 20.0F;

            d.type(VfxRenderTypes.GLOW_ADD);
            float burst = easeOut(t / 6.0F) * (1.0F - Rnd.ramp(t, 4.0F, 22.0F));
            d.glow(c, 42.0F * burst, 0xB06CFF, 0.8F * burst);
            d.glow(c, 18.0F * burst, 0xFFFFFF, burst);

            d.type(VfxRenderTypes.LINE_ADD);
            double radius = 26.0D * easeOut(t / 30.0F);
            double width = 3.2D * (1.0F - Rnd.ramp(t, 0.0F, 40.0F)) + 0.4D;
            float a = (1.0F - Rnd.ramp(t, 10.0F, duration)) * 0.9F;
            d.ring(c, AXIS_X, AXIS_Z, radius, width, 0xB06CFF, a, 64);
            d.ring(c, AXIS_X, AXIS_Y, radius, width, 0xB06CFF, a, 64);
            d.ring(c, AXIS_Z, AXIS_Y, radius, width, 0xB06CFF, a, 64);
            d.ring(c, d.left, d.up, radius, width, 0xE9D8FF, a, 64);
            d.ring(c, AXIS_X, AXIS_Z, radius * 0.6D, width * 0.5D, 0xFFFFFF, a * 0.7F, 48);

            float life = 1.3F;
            if (ts < life) {
                for (int i = 0; i < 110; i++) {
                    float k = 1.0F - ts / life;
                    d.ribbon(spark(i, Math.max(0.0F, ts - 0.06F)), spark(i, ts), 0.0F, 0.2F, 0xC9A0FF, 0.0F, k);
                }
                d.type(VfxRenderTypes.STAR_ADD);
                for (int i = 0; i < 110; i++) {
                    d.glow(spark(i, ts), 0.55F * (1.0F - ts / life), t * 0.15F + i, 0xFFFFFF, 1.0F - ts / life);
                }
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            return 1.0F * falloff(cam, origin, 90.0F) * (1.0F - Rnd.ramp(t, 0.0F, 30.0F));
        }

        @Override
        public float flash(Vec3 cam, float t) {
            return 0.85F * (1.0F - Rnd.ramp(t, 0.0F, 10.0F)) * falloff(cam, origin, 40.0F);
        }

        @Override
        public int flashColor() {
            return 0xE9D8FF;
        }
    }

    private FxVoid() {}
}
