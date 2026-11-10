package com.neant.registry;

import com.neant.Neant;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Sons synthetises du mod (assets/neant/sounds/*.ogg). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Neant.MODID);

    public static final RegistryObject<SoundEvent> RIFT_OPEN = reg("rift_open");
    public static final RegistryObject<SoundEvent> BOOM = reg("boom");
    public static final RegistryObject<SoundEvent> SLASH = reg("slash");
    public static final RegistryObject<SoundEvent> DRILL = reg("drill");
    public static final RegistryObject<SoundEvent> VOID_HOLE = reg("void_hole");
    public static final RegistryObject<SoundEvent> FORGE_CHARGE = reg("forge_charge");
    public static final RegistryObject<SoundEvent> FORGE_DONE = reg("forge_done");
    public static final RegistryObject<SoundEvent> WHIRL = reg("whirl");
    public static final RegistryObject<SoundEvent> FLAME = reg("flame");
    public static final RegistryObject<SoundEvent> PULSE = reg("pulse");
    public static final RegistryObject<SoundEvent> BLINK = reg("blink");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Neant.MODID, name)));
    }

    /** Joue un son a une position. Un volume > 1 augmente la portee d'ecoute. */
    public static void play(ServerLevel level, Vec3 pos, RegistryObject<SoundEvent> sound, float volume, float pitch) {
        level.playSound(null, pos.x, pos.y, pos.z, sound.get(), SoundSource.PLAYERS, volume, pitch);
    }

    private ModSounds() {}
}
