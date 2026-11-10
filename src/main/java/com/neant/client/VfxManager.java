package com.neant.client;

import com.neant.net.VfxPacket;
import com.neant.net.VfxType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;

/** Liste des effets actifs cote client. */
public final class VfxManager {
    public static final List<Fx> ACTIVE = new ArrayList<>();

    public static void receive(VfxPacket p) {
        if (Minecraft.getInstance().level == null) {
            return;
        }
        switch (p.type) {
            case CANCEL -> {
                VfxType wanted = VfxType.byId((int) p.param);
                for (Fx fx : ACTIVE) {
                    if (fx.entityId == p.entityId && fx.type == wanted) {
                        fx.age = fx.duration;
                    }
                }
            }
            case AURA -> {
                // une aura deja active est simplement prolongee : l'animation ne saute pas
                for (Fx fx : ACTIVE) {
                    if (fx.type == VfxType.AURA && fx.entityId == p.entityId) {
                        fx.duration = Math.max(fx.duration, fx.age + p.duration);
                        return;
                    }
                }
                ACTIVE.add(new FxArmor.Aura(p));
            }
            case SMITE -> ACTIVE.add(new FxLight.Smite(p));
            case ARC -> ACTIVE.add(new FxLight.Arc(p));
            case BLACK_HOLE -> ACTIVE.add(new FxVoid.BlackHole(p));
            case COLLAPSE -> ACTIVE.add(new FxVoid.Collapse(p));
            case RIFT -> {
                for (Fx fx : ACTIVE) {
                    if (fx.type == VfxType.RIFT && fx.entityId == p.entityId) {
                        fx.duration = Math.max(fx.duration, fx.age + p.duration);
                        return;
                    }
                }
                ACTIVE.add(new FxRift.Rift(p));
            }
            case PULSE -> ACTIVE.add(new FxRift.Pulse(p));
            case RITUAL -> ACTIVE.add(new FxRift.Ritual(p));
            case SLASH -> ACTIVE.add(new FxTools.Slash(p));
            case DRILL -> ACTIVE.add(new FxTools.Drill(p));
            case SPIN -> ACTIVE.add(new FxTools.Spin(p));
            case FIREWAVE -> ACTIVE.add(new FxTools.FireWave(p));
            case IMPLODE -> ACTIVE.add(new FxTools.Implode(p));
            case TRAIL -> ACTIVE.add(new FxArmor.Trail(p));
            case BLINK -> ACTIVE.add(new FxArmor.Blink(p));
        }
    }

    public static void tick() {
        if (Minecraft.getInstance().level == null) {
            ACTIVE.clear();
            return;
        }
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        Iterator<Fx> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Fx fx = it.next();
            fx.age++;
            if (fx.finished()) {
                it.remove();
            }
        }
    }

    private VfxManager() {}
}
