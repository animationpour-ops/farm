package com.neant.item;

import com.neant.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/** Materiau de l'armure du Neant (textures : assets/neant/textures/models/armor/neant_layer_1/2.png). */
public enum NeantArmorMaterial implements ArmorMaterial {
    NEANT;

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return 3000;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 4;
            case CHESTPLATE -> 10;
            case LEGGINGS -> 8;
            case BOOTS -> 4;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 30;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.VOID_SHARD.get());
    }

    @Override
    public String getName() {
        return "neant:neant";
    }

    @Override
    public float getToughness() {
        return 4.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.3F;
    }
}
