package com.neant.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Primitives de dessin des effets : halos, rubans, anneaux, eclairs. Tout est en coordonnees monde. */
public final class VfxDraw {
    private final MultiBufferSource.BufferSource buffers;
    private final Matrix4f pose;
    public final Vec3 cam;
    public final Vec3 left;
    public final Vec3 up;
    public final Vec3 look;
    private VertexConsumer vc;

    public VfxDraw(MultiBufferSource.BufferSource buffers, Matrix4f pose, Camera camera) {
        this.buffers = buffers;
        this.pose = pose;
        this.cam = camera.getPosition();
        Vector3f l = camera.getLeftVector();
        Vector3f u = camera.getUpVector();
        Vector3f f = camera.getLookVector();
        this.left = new Vec3(l.x(), l.y(), l.z());
        this.up = new Vec3(u.x(), u.y(), u.z());
        this.look = new Vec3(f.x(), f.y(), f.z());
    }

    /** Choisit le type de rendu (et termine le lot precedent). */
    public VfxDraw type(RenderType type) {
        this.vc = buffers.getBuffer(type);
        return this;
    }

    public void flush() {
        buffers.endBatch(VfxRenderTypes.DISC_DARK);
        buffers.endBatch(VfxRenderTypes.GLOW_DARK);
        buffers.endBatch(VfxRenderTypes.GLOW_ADD);
        buffers.endBatch(VfxRenderTypes.LINE_ADD);
        buffers.endBatch(VfxRenderTypes.STAR_ADD);
        buffers.endBatch(VfxRenderTypes.GLOW_ADD_NODEPTH);
    }

    private void v(Vec3 p, float u, float v, float r, float g, float b, float a) {
        vc.vertex(pose, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z))
                .uv(u, v).color(r, g, b, a).endVertex();
    }

    private static float red(int rgb) {
        return ((rgb >> 16) & 0xFF) / 255.0F;
    }

    private static float green(int rgb) {
        return ((rgb >> 8) & 0xFF) / 255.0F;
    }

    private static float blue(int rgb) {
        return (rgb & 0xFF) / 255.0F;
    }

    /** Halo qui regarde toujours la camera. */
    public void glow(Vec3 p, float size, int rgb, float alpha) {
        glow(p, size, 0.0F, rgb, alpha);
    }

    public void glow(Vec3 p, float size, float rot, int rgb, float alpha) {
        if (alpha <= 0.002F || size <= 0.0F) {
            return;
        }
        float hs = size * 0.5F;
        double c = Math.cos(rot) * hs;
        double s = Math.sin(rot) * hs;
        Vec3 ax = left.scale(c).add(up.scale(s));
        Vec3 ay = left.scale(-s).add(up.scale(c));
        float r = red(rgb);
        float g = green(rgb);
        float b = blue(rgb);
        v(p.subtract(ax).subtract(ay), 0, 0, r, g, b, alpha);
        v(p.add(ax).subtract(ay), 1, 0, r, g, b, alpha);
        v(p.add(ax).add(ay), 1, 1, r, g, b, alpha);
        v(p.subtract(ax).add(ay), 0, 1, r, g, b, alpha);
    }

    /** Ruban entre a et b qui se tourne vers la camera (eclairs, faisceaux, plumes). */
    public void ribbon(Vec3 a, Vec3 b, float widthA, float widthB, int rgb, float alphaA, float alphaB) {
        if (alphaA <= 0.002F && alphaB <= 0.002F) {
            return;
        }
        Vec3 dir = b.subtract(a);
        Vec3 mid = a.add(b).scale(0.5D);
        Vec3 side = dir.cross(cam.subtract(mid));
        side = side.lengthSqr() < 1.0E-10D ? left : side.normalize();
        Vec3 sa = side.scale(widthA * 0.5D);
        Vec3 sb = side.scale(widthB * 0.5D);
        float r = red(rgb);
        float g = green(rgb);
        float bl = blue(rgb);
        v(a.subtract(sa), 0, 0, r, g, bl, alphaA);
        v(a.add(sa), 1, 0, r, g, bl, alphaA);
        v(b.add(sb), 1, 1, r, g, bl, alphaB);
        v(b.subtract(sb), 0, 1, r, g, bl, alphaB);
    }

    /** Pilier vertical de lumiere : un ruban qui tourne autour de l'axe Y pour faire face a la camera. */
    public void pillar(Vec3 base, double height, float width, int rgb, float alphaBottom, float alphaTop) {
        Vec3 toCam = new Vec3(cam.x - base.x, 0, cam.z - base.z);
        Vec3 side = toCam.lengthSqr() < 1.0E-8D ? new Vec3(1, 0, 0)
                : new Vec3(-toCam.z, 0, toCam.x).normalize();
        Vec3 s = side.scale(width * 0.5D);
        Vec3 top = base.add(0, height, 0);
        float r = red(rgb);
        float g = green(rgb);
        float b = blue(rgb);
        v(base.subtract(s), 0, 0, r, g, b, alphaBottom);
        v(base.add(s), 1, 0, r, g, b, alphaBottom);
        v(top.add(s), 1, 1, r, g, b, alphaTop);
        v(top.subtract(s), 0, 1, r, g, b, alphaTop);
    }

    /** Anneau plat dans le plan (u, v), u et v unitaires et perpendiculaires. */
    public void ring(Vec3 c, Vec3 u, Vec3 vv, double radius, double width, int rgb, float alpha, int segs) {
        arc(c, u, vv, radius, width, 0.0D, Math.PI * 2.0D, rgb, alpha, segs);
    }

    /** Arc d'anneau entre deux angles (radians), l'epaisseur est lissee sur la largeur par la texture. */
    public void arc(Vec3 c, Vec3 u, Vec3 vv, double radius, double width, double a0, double a1,
                    int rgb, float alpha, int segs) {
        if (alpha <= 0.002F || radius <= 0.0D) {
            return;
        }
        float r = red(rgb);
        float g = green(rgb);
        float b = blue(rgb);
        double in = Math.max(0.0D, radius - width * 0.5D);
        double out = radius + width * 0.5D;
        double step = (a1 - a0) / segs;
        Vec3 prevIn = null;
        Vec3 prevOut = null;
        for (int i = 0; i <= segs; i++) {
            double ang = a0 + step * i;
            Vec3 dir = u.scale(Math.cos(ang)).add(vv.scale(Math.sin(ang)));
            Vec3 pin = c.add(dir.scale(in));
            Vec3 pout = c.add(dir.scale(out));
            if (prevIn != null) {
                v(prevIn, 0, 0, r, g, b, alpha);
                v(prevOut, 1, 0, r, g, b, alpha);
                v(pout, 1, 1, r, g, b, alpha);
                v(pin, 0, 1, r, g, b, alpha);
            }
            prevIn = pin;
            prevOut = pout;
        }
    }

    /** Eclair : deplacement du point milieu, en trois couches (halo, couleur, coeur blanc). */
    public void lightning(Vec3 a, Vec3 b, int seed, int levels, float spread, float width, int rgb, float alpha,
                          int branches) {
        Vec3[] pts = bolt(a, b, seed, levels, spread);
        drawBolt(pts, width, rgb, alpha);
        for (int k = 0; k < branches; k++) {
            int idx = 1 + (int) (Rnd.f(seed, 900 + k) * (pts.length - 3));
            Vec3 from = pts[idx];
            Vec3 main = b.subtract(a);
            Vec3 rnd = new Vec3(Rnd.s(seed, 910 + k), Rnd.s(seed, 920 + k) * 0.6D - 0.4D, Rnd.s(seed, 930 + k));
            Vec3 dir = main.normalize().add(rnd.scale(1.1D)).normalize();
            double len = main.length() * Rnd.range(seed, 940 + k, 0.12F, 0.3F);
            Vec3[] sub = bolt(from, from.add(dir.scale(len)), seed + 77 * (k + 1), Math.max(2, levels - 2), spread);
            drawBolt(sub, width * 0.55F, rgb, alpha * 0.85F);
        }
    }

    private static Vec3[] bolt(Vec3 a, Vec3 b, int seed, int levels, float spread) {
        int n = 2;
        Vec3[] pts = new Vec3[(1 << levels) + 1];
        pts[0] = a;
        pts[1] = b;
        double amp = a.distanceTo(b) * spread;
        int salt = 0;
        for (int l = 0; l < levels; l++) {
            Vec3[] next = new Vec3[(n - 1) * 2 + 1];
            for (int i = 0; i < n - 1; i++) {
                Vec3 p0 = pts[i];
                Vec3 p1 = pts[i + 1];
                Vec3 mid = p0.add(p1).scale(0.5D);
                Vec3 off = new Vec3(Rnd.s(seed, salt++), Rnd.s(seed, salt++), Rnd.s(seed, salt++)).scale(amp);
                next[i * 2] = p0;
                next[i * 2 + 1] = mid.add(off);
            }
            next[next.length - 1] = pts[n - 1];
            System.arraycopy(next, 0, pts, 0, next.length);
            n = next.length;
            amp *= 0.55D;
        }
        Vec3[] out = new Vec3[n];
        System.arraycopy(pts, 0, out, 0, n);
        return out;
    }

    private void drawBolt(Vec3[] pts, float width, int rgb, float alpha) {
        for (int i = 0; i < pts.length - 1; i++) {
            ribbon(pts[i], pts[i + 1], width * 5.0F, width * 5.0F, rgb, alpha * 0.35F, alpha * 0.35F);
        }
        for (int i = 0; i < pts.length - 1; i++) {
            ribbon(pts[i], pts[i + 1], width * 2.2F, width * 2.2F, rgb, alpha * 0.9F, alpha * 0.9F);
        }
        for (int i = 0; i < pts.length - 1; i++) {
            ribbon(pts[i], pts[i + 1], width, width, 0xFFFFFF, alpha, alpha);
        }
    }
}
