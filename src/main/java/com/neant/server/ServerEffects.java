package com.neant.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Petit planificateur serveur : effets qui durent plusieurs ticks et actions differees. */
@Mod.EventBusSubscriber(modid = "neant")
public final class ServerEffects {

    public interface Timed {
        /** Appele chaque tick. Renvoie true quand l'effet est termine. */
        boolean tick(ServerLevel level);

        /** Appele une fois a la fin (ou a l'arret du serveur). */
        default void end(ServerLevel level) {}
    }

    private static final Map<ServerLevel, List<Timed>> ACTIVE = new HashMap<>();
    private static final Map<ServerLevel, List<Timed>> PENDING = new HashMap<>();

    public static void add(ServerLevel level, Timed timed) {
        PENDING.computeIfAbsent(level, l -> new ArrayList<>()).add(timed);
    }

    /** Execute une action dans N ticks. */
    public static void delay(ServerLevel level, int ticks, Runnable action) {
        add(level, new Timed() {
            int left = ticks;

            @Override
            public boolean tick(ServerLevel l) {
                if (--left <= 0) {
                    action.run();
                    return true;
                }
                return false;
            }
        });
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        List<Timed> pending = PENDING.remove(level);
        List<Timed> list = ACTIVE.computeIfAbsent(level, l -> new ArrayList<>());
        if (pending != null) {
            list.addAll(pending);
        }
        Iterator<Timed> it = list.iterator();
        while (it.hasNext()) {
            Timed t = it.next();
            boolean done;
            try {
                done = t.tick(level);
            } catch (RuntimeException e) {
                e.printStackTrace();
                done = true;
            }
            if (done) {
                try {
                    t.end(level);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (Map.Entry<ServerLevel, List<Timed>> e : ACTIVE.entrySet()) {
            for (Timed t : e.getValue()) {
                try {
                    t.end(e.getKey());
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                }
            }
        }
        ACTIVE.clear();
        PENDING.clear();
    }

    private ServerEffects() {}
}
