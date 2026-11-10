package com.neant.server;

import com.neant.registry.ModItems;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Recettes de la Forge du Neant : un objet de base vanilla + des fragments de Neant. */
public final class ForgeRecipes {

    public record Recipe(Supplier<Item> base, int shards, Supplier<Item> result) {}

    public static List<Recipe> all() {
        return List.of(
                new Recipe(() -> Items.DIAMOND_HELMET, 20, ModItems.NEANT_HELMET::get),
                new Recipe(() -> Items.DIAMOND_CHESTPLATE, 32, ModItems.NEANT_CHESTPLATE::get),
                new Recipe(() -> Items.DIAMOND_LEGGINGS, 28, ModItems.NEANT_LEGGINGS::get),
                new Recipe(() -> Items.DIAMOND_BOOTS, 16, ModItems.NEANT_BOOTS::get),
                new Recipe(() -> Items.DIAMOND_SWORD, 24, ModItems.NEANT_SWORD::get),
                new Recipe(() -> Items.DIAMOND_PICKAXE, 24, ModItems.NEANT_PICKAXE::get),
                new Recipe(() -> Items.DIAMOND_SHOVEL, 16, ModItems.NEANT_SHOVEL::get),
                new Recipe(() -> Items.DIAMOND_AXE, 24, ModItems.NEANT_AXE::get),
                new Recipe(() -> Items.FLINT_AND_STEEL, 20, ModItems.NEANT_LIGHTER::get));
    }

    private ForgeRecipes() {}
}
