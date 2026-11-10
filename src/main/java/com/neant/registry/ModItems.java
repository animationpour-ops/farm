package com.neant.registry;

import com.neant.Neant;
import com.neant.item.NeantArmorItem;
import com.neant.item.NeantAxeItem;
import com.neant.item.NeantLighterItem;
import com.neant.item.NeantPickaxeItem;
import com.neant.item.NeantShovelItem;
import com.neant.item.NeantSwordItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Neant.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Neant.MODID);

    private static Item.Properties gear() {
        return new Item.Properties().rarity(Rarity.EPIC).fireResistant();
    }

    public static final RegistryObject<Item> VOID_SHARD = ITEMS.register("void_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()) {
                @Override
                public boolean isFoil(ItemStack stack) {
                    return true;
                }
            });

    public static final RegistryObject<Item> VOID_FORGE = ITEMS.register("void_forge",
            () -> new BlockItem(ModBlocks.VOID_FORGE.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> NEANT_HELMET = ITEMS.register("neant_helmet",
            () -> new NeantArmorItem(ArmorItem.Type.HELMET, gear()));
    public static final RegistryObject<Item> NEANT_CHESTPLATE = ITEMS.register("neant_chestplate",
            () -> new NeantArmorItem(ArmorItem.Type.CHESTPLATE, gear()));
    public static final RegistryObject<Item> NEANT_LEGGINGS = ITEMS.register("neant_leggings",
            () -> new NeantArmorItem(ArmorItem.Type.LEGGINGS, gear()));
    public static final RegistryObject<Item> NEANT_BOOTS = ITEMS.register("neant_boots",
            () -> new NeantArmorItem(ArmorItem.Type.BOOTS, gear()));

    public static final RegistryObject<Item> NEANT_SWORD = ITEMS.register("neant_sword",
            () -> new NeantSwordItem(gear()));
    public static final RegistryObject<Item> NEANT_PICKAXE = ITEMS.register("neant_pickaxe",
            () -> new NeantPickaxeItem(gear()));
    public static final RegistryObject<Item> NEANT_SHOVEL = ITEMS.register("neant_shovel",
            () -> new NeantShovelItem(gear()));
    public static final RegistryObject<Item> NEANT_AXE = ITEMS.register("neant_axe",
            () -> new NeantAxeItem(gear()));
    public static final RegistryObject<Item> NEANT_LIGHTER = ITEMS.register("neant_lighter",
            () -> new NeantLighterItem(gear()));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.neant"))
                    .icon(() -> new ItemStack(NEANT_SWORD.get()))
                    .displayItems((params, out) -> ITEMS.getEntries().forEach(e -> out.accept(e.get())))
                    .build());

    private ModItems() {}
}
