package com.neant.client;

import com.neant.net.VfxPacket;
import com.neant.net.VfxType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Un effet visuel actif cote client. Il se dessine entierement en fonction de son age (aucun etat de particule). */
public abstract class Fx {
    public final VfxType type;
    public final int entityId;
    public int duration;
    public final int seed;
    public final int color;
    public final float param;
    protected final Vec3 origin;
    protected final Vec3 target;
    public int age;
    private Vec3 lastFollowed;

    protected Fx(VfxPacket p) {
        this.type = p.type;
        this.entityId = p.entityId;
        this.duration = Math.max(1, p.duration);
        this.seed = p.seed;
        this.color = p.color;
        this.param = p.param;
        this.origin = new Vec3(p.x, p.y, p.z);
        this.target = new Vec3(p.x2, p.y2, p.z2);
        this.lastFollowed = origin;
    }

    public boolean finished() {
        return age >= duration;
    }

    /** t = age en ticks avec la fraction de tick (partial). */
    public abstract void render(VfxDraw d, float t, float partial);

    /** Intensite de tremblement de camera (0..1). */
    public float shake(Vec3 cam, float t) {
        return 0.0F;
    }

    /** Intensite d'un flash plein ecran (0..1). */
    public float flash(Vec3 cam, float t) {
        return 0.0F;
    }

    public int flashColor() {
        return 0xFFFFFF;
    }

    /** Dessin optionnel sur l'ecran (teinte, vignette). */
    public void overlay(GuiGraphics g, int w, int h, float t, Vec3 cam) {}

    /** Position courante : suit l'entite si l'effet en a une. */
    protected Vec3 pos(float partial) {
        if (entityId >= 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Entity e = mc.level.getEntity(entityId);
                if (e != null) {
                    lastFollowed = e.getPosition(partial);
                }
            }
            return lastFollowed;
        }
        return origin;
    }

    protected Entity followed() {
        Minecraft mc = Minecraft.getInstance();
        return entityId >= 0 && mc.level != null ? mc.level.getEntity(entityId) : null;
    }

    // ---- utilitaires de forme ----

    protected static float falloff(Vec3 cam, Vec3 p, float range) {
        double dist = cam.distanceTo(p);
        float f = 1.0F - (float) Math.min(1.0D, dist / range);
        return f * f;
    }

    protected static float easeOut(float x) {
        x = Rnd.clamp01(x);
        float inv = 1.0F - x;
        return 1.0F - inv * inv * inv;
    }

    protected static float easeIn(float x) {
        x = Rnd.clamp01(x);
        return x * x;
    }

    protected static float fract(float x) {
        return x - (float) Math.floor(x);
    }

    protected static final Vec3 AXIS_X = new Vec3(1, 0, 0);
    protected static final Vec3 AXIS_Y = new Vec3(0, 1, 0);
    protected static final Vec3 AXIS_Z = new Vec3(0, 0, 1);

    /** Direction aleatoire uniforme sur la sphere, deterministe. */
    protected static Vec3 sphere(int seed, int i) {
        double z = Rnd.s(seed, i * 3);
        double a = Rnd.f(seed, i * 3 + 1) * Math.PI * 2.0D;
        double r = Math.sqrt(1.0D - z * z);
        return new Vec3(r * Math.cos(a), z, r * Math.sin(a));
    }
}
