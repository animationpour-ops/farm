package com.neant.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Logique commune aux pouvoirs actives par clic droit : recharge, execution cote serveur, infobulle. */
public final class PowerUse {

    @FunctionalInterface
    public interface Activator {
        boolean activate(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand);
    }

    /** Lance le pouvoir si l'item est charge. Renvoie true si le pouvoir a ete declenche (cote serveur) ou tente (cote client). */
    public static boolean tryActivate(Item item, Activator activator, int cooldownTicks,
                                      Level level, Player player, InteractionHand hand) {
        if (player.getCooldowns().isOnCooldown(item)) {
            return false;
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            if (!activator.activate(sl, sp, player.getItemInHand(hand), hand)) {
                return false;
            }
            player.getCooldowns().addCooldown(item, cooldownTicks);
        }
        player.swing(hand);
        return true;
    }

    public static InteractionResultHolder<ItemStack> use(Item item, Activator activator, int cooldownTicks,
                                                         Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean ok = tryActivate(item, activator, cooldownTicks, level, player, hand);
        return ok ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide())
                : InteractionResultHolder.fail(stack);
    }

    public static void tooltip(Item item, int cooldownTicks, List<Component> tip) {
        for (int i = 1; i <= 3; i++) {
            String key = item.getDescriptionId() + ".desc" + i;
            if (i > 1 && !net.minecraft.locale.Language.getInstance().has(key)) {
                break;
            }
            tip.add(Component.translatable(key).withStyle(i == 1 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
        }
        if (cooldownTicks > 20) {
            tip.add(Component.translatable("tooltip.neant.cooldown", cooldownTicks / 20).withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    private PowerUse() {}
}
