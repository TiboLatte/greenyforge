package com.tibolatte.greeny.registry;

import com.tibolatte.greeny.Greeny;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Greeny.MODID);

    // Helper to register a BlockItem for every Block
    public static final RegistryObject<Item> AXIOM_HEART_CORE_ITEM = ITEMS.register("axiom_heart_core",
            () -> new BlockItem(BlockRegistry.AXIOM_HEART_CORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> ANCIENT_ROOT_ITEM = ITEMS.register("ancient_root",
            () -> new BlockItem(BlockRegistry.ANCIENT_ROOT.get(), new Item.Properties()));
}