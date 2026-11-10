package com.neant.client;

import com.neant.net.VfxPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

/** Effets de la faille du Neant, du rituel de la forge et des pulsations de choc. */
public final class FxRift {

    private static final int[] RIM = {0xFFFFFF, 0xE9D8FF, 0xC07CFF, 0x8A4DFF, 0x5A2CC8};

    // ------------------------------------------------------------------ faille

    /** Dechirure dans le ciel : disque noir, anneaux d'energie, fissures, eclairs vers le sol, spirale d'etoiles. */
    public static final class Rift extends Fx {
        private static final float OPEN = 60.0F;
        private static final double HEIGHT = 18.0D;

        public Rift(VfxPacket p) {
            super(p);
        }

        private float grow(float t) {
            return Rnd.smooth(Rnd.ramp(t, 0.0F, OPEN));
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            float g = grow(t);
            if (g <= 0.01F) {
                return;
            }
            Vec3 c = origin;
            Vec3 ground = origin.add(0, -HEIGHT, 0);
            float ts = t / 20.0F;
            float r = 6.5F * g * (1.0F + 0.03F * (float) Math.sin(t * 0.6F));

            // halo lointain
            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(c, r * 5.5F, 0x5A2CC8, 0.16F * g);
            d.glow(c, r * 3.2F, 0x8A4DFF, 0.26F * g);

            // faisceau violet qui descend jusqu'au sol
            d.pillar(ground, HEIGHT, 3.2F * g, 0xB06CFF, 0.5F * g, 0.06F * g);
            d.glow(ground.add(0, 0.8D, 0), 9.0F * g, 0xB06CFF, 0.35F * g);

            // anneaux d'energie qui tournent dans le plan de la camera
            d.type(VfxRenderTypes.LINE_ADD);
            for (int k = 0; k < 5; k++) {
                double rr = r * (1.05D + 0.34D * k);
                double base = ts * (k % 2 == 0 ? 1.2D : -0.9D) + k * 1.3D;
                for (int seg = 0; seg < 3; seg++) {
                    double a0 = base + seg * Math.PI * 2.0D / 3.0D;
                    d.arc(c, d.left, d.up, rr, r * (0.34D - 0.04D * k), a0, a0 + 1.5D - 0.15D * k, RIM[k],
                            (0.8F - 0.12F * k) * g, 28);
                }
            }

            // fissures qui partent du bord (elles scintillent : graine changee tous les 3 ticks)
            int cs = seed + (int) (t / 3.0F);
            for (int i = 0; i < 9; i++) {
                double ang = Rnd.f(seed, i + 50) * Math.PI * 2.0D;
                double len = r * Rnd.range(seed, i + 60, 0.7F, 1.7F);
                Vec3 dirv = d.left.scale(Math.cos(ang)).add(d.up.scale(Math.sin(ang)));
                Vec3 a = c.add(dirv.scale(r * 1.0D));
                Vec3 b = c.add(dirv.scale(r * 1.0D + len));
                d.lightning(a, b, cs + i * 13, 4, 0.2F, 0.07F, 0xC07CFF, 0.9F * g, 1);
            }

            // le coeur noir
            d.type(VfxRenderTypes.DISC_DARK);
            d.glow(c, r * 2.0F, 0x000000, g);
            d.type(VfxRenderTypes.LINE_ADD);
            d.ring(c, d.left, d.up, r * 1.02D, r * 0.16D, 0xFFFFFF, 0.9F * g, 64);
            d.ring(c, d.left, d.up, r * 1.1D, r * 0.5D, 0xB06CFF, 0.45F * g, 64);

            // galaxie : etoiles qui spiralent vers le centre
            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < 90; i++) {
                float lp = 1.6F + Rnd.f(seed, i + 100) * 1.4F;
                float phase = fract(ts / lp + Rnd.f(seed, i + 200));
                double rr = r * (0.2D + 2.6D * (1.0D - phase));
                double ang = Rnd.f(seed, i + 300) * Math.PI * 2.0D + phase * 6.0D;
                Vec3 pos = c.add(d.left.scale(Math.cos(ang) * rr)).add(d.up.scale(Math.sin(ang) * rr));
                d.glow(pos, 0.4F + 0.3F * phase, ts * 2.0F + i, phase > 0.5F ? 0xFFFFFF : 0xC9A0FF,
                        (float) Math.sin(phase * Math.PI) * g);
            }

            // eclairs reguliers vers le sol une fois ouverte
            if (t > OPEN) {
                int period = 18;
                int step = (int) (t / period);
                float within = t - step * period;
                if (within < 6.0F && Rnd.f(seed, step + 700) < 0.6F) {
                    float a = 1.0F - within / 6.0F;
                    int sd = seed + step * 31;
                    Vec3 end = ground.add(Rnd.s(sd, 1) * 14.0D, 0, Rnd.s(sd, 2) * 14.0D);
                    d.type(VfxRenderTypes.LINE_ADD);
                    d.lightning(c.add(d.left.scale(Rnd.s(sd, 3) * r * 0.6D)), end, sd, 6, 0.13F, 0.22F, 0xC07CFF, a, 3);
                }
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            // la faille tremble en s'ouvrant, puis se calme
            return 0.5F * (1.0F - Rnd.ramp(t, 0.0F, OPEN + 20.0F)) * falloff(cam, origin, 90.0F);
        }

        @Override
        public float flash(Vec3 cam, float t) {
            return 0.3F * (1.0F - Rnd.ramp(t, 0.0F, 8.0F)) * falloff(cam, origin, 70.0F);
        }

        @Override
        public int flashColor() {
            return 0xD8C0FF;
        }

        @Override
        public void overlay(GuiGraphics g, int w, int h, float t, Vec3 cam) {
            // legere teinte violette quand on est juste dessous
            double dx = cam.x - origin.x;
            double dz = cam.z - origin.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist >= 14.0D) {
                return;
            }
            float k = (float) (1.0D - dist / 14.0D) * grow(t);
            int alpha = (int) (k * 0.16F * 255.0F);
            if (alpha > 0) {
                g.fill(0, 0, w, h, (alpha << 24) | 0x2A0A55);
            }
        }
    }

    // ------------------------------------------------------------------- pulsation

    /** Onde de choc : anneaux sur le sol et dans l'air, eclat central, poussiere. param = rayon maximal. */
    public static final class Pulse extends Fx {
        public Pulse(VfxPacket p) {
            super(p);
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            float maxR = Math.max(2.0F, param);
            float x = t / duration;
            double radius = maxR * easeOut(x);
            float a = (1.0F - Rnd.ramp(x, 0.2F, 1.0F)) * 0.9F;
            double width = maxR * 0.12D * (1.0F - x) + 0.25D;
            Vec3 c = origin;

            d.type(VfxRenderTypes.LINE_ADD);
            d.ring(c, AXIS_X, AXIS_Z, radius, width, color, a, 72);
            d.ring(c, AXIS_X, AXIS_Z, radius * 0.97D, width * 0.35D, 0xFFFFFF, a, 72);
            d.ring(c, AXIS_X, AXIS_Y, radius * 0.8D, width * 0.6D, color, a * 0.6F, 56);
            d.ring(c, AXIS_Z, AXIS_Y, radius * 0.8D, width * 0.6D, color, a * 0.6F, 56);
            d.ring(c, d.left, d.up, radius * 0.9D, width * 0.5D, 0xFFFFFF, a * 0.5F, 56);

            d.type(VfxRenderTypes.GLOW_ADD);
            float burst = (1.0F - Rnd.ramp(t, 0.0F, 8.0F));
            d.glow(c, maxR * 1.3F * burst, color, 0.7F * burst);
            d.glow(c, maxR * 0.5F * burst, 0xFFFFFF, burst);

            d.type(VfxRenderTypes.STAR_ADD);
            int n = (int) Math.min(80.0F, 14.0F + maxR * 4.0F);
            for (int i = 0; i < n; i++) {
                double ang = Rnd.f(seed, i) * Math.PI * 2.0D;
                double rr = radius * Rnd.range(seed, i + 100, 0.55F, 1.05F);
                float life = 1.0F - x;
                Vec3 p = c.add(Math.cos(ang) * rr, 0.2D + x * 2.5D * Rnd.f(seed, i + 200), Math.sin(ang) * rr);
                d.glow(p, 0.4F * life + 0.1F, t * 0.2F + i, i % 3 == 0 ? 0xFFFFFF : color, life);
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            return Math.min(0.9F, param / 14.0F) * (1.0F - Rnd.ramp(t, 0.0F, 18.0F)) * falloff(cam, origin, 12.0F + param * 3.0F);
        }
    }

    // ------------------------------------------------------------------- rituel

    /** Rituel de la forge : cercle runique qui s'allume, energie qui converge, montee en puissance. */
    public static final class Ritual extends Fx {
        private static final float CHARGE = 70.0F;

        public Ritual(VfxPacket p) {
            super(p);
        }

        @Override
        public void render(VfxDraw d, float t, float partial) {
            float charge = Rnd.smooth(Rnd.ramp(t, 0.0F, CHARGE));
            float fade = 1.0F - Rnd.ramp(t, CHARGE, duration);
            float life = Rnd.ramp(t, 0.0F, 6.0F) * fade;
            if (life <= 0.0F) {
                return;
            }
            Vec3 c = origin;
            Vec3 floor = new Vec3(c.x, c.y - 0.35D, c.z);
            float ts = t / 20.0F;
            int purple = 0xB06CFF;

            d.type(VfxRenderTypes.GLOW_ADD);
            d.glow(c.add(0, 0.4D, 0), 2.0F + 5.0F * charge, purple, (0.25F + 0.5F * charge) * life);
            d.glow(c.add(0, 0.4D, 0), 0.8F + 2.0F * charge, 0xFFFFFF, 0.6F * charge * life);
            d.pillar(floor, 2.0D + 12.0D * charge, 0.6F + 1.4F * charge, purple, 0.55F * charge * life, 0.0F);

            d.type(VfxRenderTypes.LINE_ADD);
            // cercle runique au sol : deux anneaux et des arcs qui tournent en sens inverse
            d.ring(floor, AXIS_X, AXIS_Z, 1.9D, 0.12D, 0xFFFFFF, 0.8F * life, 56);
            d.ring(floor, AXIS_X, AXIS_Z, 1.5D, 0.1D, purple, 0.7F * life, 56);
            for (int k = 0; k < 4; k++) {
                double a0 = ts * (1.0D + charge) + k * Math.PI / 2.0D;
                d.arc(floor, AXIS_X, AXIS_Z, 1.2D, 0.2D, a0, a0 + 0.9D, purple, 0.9F * life * charge, 14);
                double a1 = -ts * (0.8D + charge) + k * Math.PI / 2.0D;
                d.arc(floor, AXIS_X, AXIS_Z, 2.3D, 0.16D, a1, a1 + 0.7D, 0xE9D8FF, 0.7F * life * charge, 14);
            }
            // anneau vertical qui s'eleve et se resserre
            for (int j = 0; j < 3; j++) {
                float phase = fract(ts * 0.9F + j / 3.0F);
                d.ring(floor.add(0, phase * 3.5D, 0), AXIS_X, AXIS_Z, 1.8D * (1.0D - phase * 0.6D), 0.12D, purple,
                        (1.0F - phase) * 0.8F * life * (0.3F + 0.7F * charge), 32);
            }
            // filets d'energie qui convergent vers le centre
            for (int i = 0; i < 12; i++) {
                double ang = Rnd.f(seed, i) * Math.PI * 2.0D + ts * 0.5D;
                float lp = 0.8F + Rnd.f(seed, i + 50) * 0.8F;
                float phase = fract(ts / lp + Rnd.f(seed, i + 90));
                double r0 = 3.2D * (1.0D - phase);
                Vec3 p0 = floor.add(Math.cos(ang) * r0 * 1.25D, 0.1D + phase * 0.5D, Math.sin(ang) * r0 * 1.25D);
                Vec3 p1 = floor.add(Math.cos(ang) * r0, 0.15D + phase * 0.6D, Math.sin(ang) * r0);
                d.ribbon(p0, p1, 0.0F, 0.12F, purple, 0.0F, 0.85F * life * (0.3F + charge * 0.7F));
            }
            if (charge > 0.6F) {
                int sd = seed + (int) (t / 2.0F);
                d.lightning(c.add(0, 0.4D, 0), c.add(Rnd.s(sd, 1) * 2.6D, 2.8D, Rnd.s(sd, 2) * 2.6D), sd, 4, 0.2F,
                        0.1F, 0xC07CFF, (charge - 0.6F) * 2.0F * life, 1);
            }

            d.type(VfxRenderTypes.STAR_ADD);
            for (int i = 0; i < 60; i++) {
                float lp = 1.2F + Rnd.f(seed, i + 100) * 1.0F;
                float phase = fract(ts / lp + Rnd.f(seed, i + 200));
                double ang = Rnd.f(seed, i + 300) * Math.PI * 2.0D + phase * 5.0D;
                double rr = 2.8D * (1.0D - phase);
                Vec3 p = floor.add(Math.cos(ang) * rr, 0.2D + phase * 2.2D, Math.sin(ang) * rr);
                d.glow(p, 0.3F + 0.2F * phase, ts * 2.0F + i, i % 3 == 0 ? 0xFFFFFF : 0xC9A0FF,
                        (float) Math.sin(phase * Math.PI) * life);
            }
        }

        @Override
        public float shake(Vec3 cam, float t) {
            float charge = Rnd.ramp(t, 20.0F, CHARGE);
            return 0.22F * charge * (1.0F - Rnd.ramp(t, CHARGE, duration)) * falloff(cam, origin, 16.0F);
        }
    }

    private FxRift() {}
}
