package com.neant.server;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.neant.NeantConfig;
import com.neant.net.VfxPacket;
import com.neant.net.VfxType;
import com.neant.registry.ModItems;
import com.neant.registry.ModSounds;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

/**
 * Gestion des failles : ouverture automatique, fragments laches par les creatures du Neant,
 * effets de mort et commandes pratiques pour tourner une video.
 */
@Mod.EventBusSubscriber(modid = "neant")
public final class RiftEvents {
    /** Premier delai apres le lancement du monde : court, pour ne pas attendre devant la camera. */
    private static final int FIRST_DELAY_TICKS = 20 * 45;
    private static int countdown = FIRST_DELAY_TICKS;

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (!NeantConfig.AUTO_RIFTS.get() || RiftEvent.isActive()) {
            return;
        }
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }
        if (--countdown > 0) {
            return;
        }
        countdown = NeantConfig.RIFT_INTERVAL_SECONDS.get() * 20;
        ServerPlayer target = players.get(level.random.nextInt(players.size()));
        double ang = level.random.nextDouble() * Math.PI * 2.0D;
        double dist = 28.0D + level.random.nextDouble() * 22.0D;
        RiftEvent.open(level, target.getX() + Math.cos(ang) * dist, target.getZ() + Math.sin(ang) * dist);
    }

    // ---------------------------------------------------------------- butin et morts

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level)) {
            return;
        }
        boolean guardian = e.getPersistentData().getBoolean(RiftEvent.TAG_GUARDIAN);
        boolean minion = e.getPersistentData().getBoolean(RiftEvent.TAG_MINION);
        if (!guardian && !minion) {
            return;
        }
        // les creatures du Neant ne laissent que des fragments
        event.getDrops().clear();
        int base = guardian ? 14 + level.random.nextInt(8) : 1 + level.random.nextInt(2);
        int count = base * NeantConfig.SHARD_MULTIPLIER.get();
        while (count > 0) {
            int n = Math.min(count, 64);
            count -= n;
            ItemEntity drop = new ItemEntity(level, e.getX(), e.getY() + 0.5D, e.getZ(),
                    new ItemStack(ModItems.VOID_SHARD.get(), n));
            drop.setDeltaMovement((level.random.nextDouble() - 0.5D) * 0.4D, 0.35D,
                    (level.random.nextDouble() - 0.5D) * 0.4D);
            drop.setUnlimitedLifetime();
            drop.setInvulnerable(true);
            event.getDrops().add(drop);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel level)) {
            return;
        }
        boolean guardian = e.getPersistentData().getBoolean(RiftEvent.TAG_GUARDIAN);
        boolean minion = e.getPersistentData().getBoolean(RiftEvent.TAG_MINION);
        if (!guardian && !minion) {
            return;
        }
        VfxPacket.of(VfxType.CANCEL, e.position()).follow(e).param(VfxType.AURA.ordinal()).send(level);
        Vec3 c = e.position().add(0, e.getBbHeight() * 0.5D, 0);
        if (guardian) {
            VfxPacket.of(VfxType.COLLAPSE, c).duration(46).color(0xB06CFF).send(level);
            ModSounds.play(level, c, ModSounds.BOOM, 8.0F, 0.7F);
        } else {
            VfxPacket.of(VfxType.PULSE, c).duration(20).param(4.5F).color(0xB06CFF).send(level);
            ModSounds.play(level, c, ModSounds.PULSE, 1.6F, 1.5F);
        }
    }

    // ----------------------------------------------------------------------- commandes

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("neant")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("faille").executes(ctx -> openRift(ctx.getSource())))
                .then(Commands.literal("fragments")
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 2000))
                                .executes(ctx -> giveShards(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "n")))))
                .then(Commands.literal("kit").executes(ctx -> giveKit(ctx.getSource()))));
    }

    private static int openRift(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        Vec3 p = src.getPosition();
        double ang = level.random.nextDouble() * Math.PI * 2.0D;
        double dist = 22.0D;
        if (RiftEvent.open(level, p.x + Math.cos(ang) * dist, p.z + Math.sin(ang) * dist)) {
            return 1;
        }
        src.sendFailure(Component.translatable("command.neant.rift_already"));
        return 0;
    }

    private static int giveShards(CommandSourceStack src, int n) {
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            return 0;
        }
        give(player, ModItems.VOID_SHARD, n);
        src.sendSuccess(() -> Component.translatable("command.neant.shards", n).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return n;
    }

    /** Tout l'equipement d'un coup : pratique pour tester les pouvoirs ou lancer la video. */
    private static int giveKit(CommandSourceStack src) {
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            return 0;
        }
        List<RegistryObject<Item>> kit = List.of(ModItems.NEANT_SWORD, ModItems.NEANT_PICKAXE, ModItems.NEANT_SHOVEL,
                ModItems.NEANT_AXE, ModItems.NEANT_LIGHTER, ModItems.NEANT_HELMET, ModItems.NEANT_CHESTPLATE,
                ModItems.NEANT_LEGGINGS, ModItems.NEANT_BOOTS);
        for (RegistryObject<Item> item : kit) {
            give(player, item, 1);
        }
        src.sendSuccess(() -> Component.translatable("command.neant.kit").withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return kit.size();
    }

    private static void give(ServerPlayer player, RegistryObject<Item> item, int count) {
        int left = count;
        while (left > 0) {
            int n = Math.min(left, item.get().getMaxStackSize());
            left -= n;
            ItemStack stack = new ItemStack(item.get(), n);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    private RiftEvents() {}
}
