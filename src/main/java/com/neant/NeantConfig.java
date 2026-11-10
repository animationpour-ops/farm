package com.neant;

import net.minecraftforge.common.ForgeConfigSpec;

/** Reglages (fichier config/neant-common.toml). Pratique pour tourner une video : tout se coupe ou s'accelere. */
public final class NeantConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue AUTO_RIFTS;
    public static final ForgeConfigSpec.IntValue RIFT_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.IntValue SHARD_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue TERRAIN_DESTRUCTION;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("failles");
        AUTO_RIFTS = b.comment("Une faille du Neant s'ouvre toute seule pres d'un joueur (Overworld).")
                .define("autoRifts", true);
        RIFT_INTERVAL_SECONDS = b.comment("Delai entre deux failles automatiques, en secondes.")
                .defineInRange("riftIntervalSeconds", 180, 20, 36000);
        SHARD_MULTIPLIER = b.comment("Multiplie le nombre de fragments laches (pour farmer plus vite dans une video).")
                .defineInRange("shardMultiplier", 1, 1, 20);
        b.pop();
        b.push("pouvoirs");
        TERRAIN_DESTRUCTION = b.comment("Les pouvoirs des outils effacent le terrain (trou noir, foreuse, onde de l'epee...).")
                .define("terrainDestruction", true);
        b.pop();
        SPEC = b.build();
    }

    private NeantConfig() {}
}
