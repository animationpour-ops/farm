package com.neant.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Piece de l'armure du Neant : texture de portail de l'End, incassable, bonus propre + bonus d'ensemble. */
public class NeantArmorItem extends ArmorItem {

    public NeantArmorItem(Type type, Properties properties) {
        super(NeantArmorMaterial.NEANT, type, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canBeDepleted() {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.LIGHT_PURPLE));
        tip.add(Component.empty());
        tip.add(Component.translatable("tooltip.neant.set_title").withStyle(ChatFormatting.DARK_PURPLE));
        tip.add(Component.translatable("tooltip.neant.set1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.neant.set2").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.neant.set3").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.neant.set4").withStyle(ChatFormatting.GRAY));
    }

    public static boolean isNeant(ItemStack stack) {
        return stack.getItem() instanceof NeantArmorItem;
    }

    /** Nombre de pieces portees (0 a 4). */
    public static int piecesWorn(LivingEntity e) {
        int n = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET}) {
            if (isNeant(e.getItemBySlot(slot))) {
                n++;
            }
        }
        return n;
    }

    public static boolean hasFullSet(LivingEntity e) {
        return piecesWorn(e) == 4;
    }

    public static boolean wears(LivingEntity e, EquipmentSlot slot) {
        return isNeant(e.getItemBySlot(slot));
    }
}
