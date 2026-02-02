package com.tibolatte.greeny.registry;

import com.tibolatte.greeny.Greeny;
import com.tibolatte.greeny.blocks.AxiomHeartBlock;
import com.tibolatte.greeny.blocks.AxiomSoilBlock;
import com.tibolatte.greeny.blocks.PhasingRootBlock;
import com.tibolatte.greeny.blocks.PhasingSproutBlock;
import com.tibolatte.greeny.worldgen.tree.WhisperingOakTreeGrower; // Ensure this import is here
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus; // Missing import
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockRegistry {
    // 1. REGISTRIES
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Greeny.MODID);
    // Added this line so we can register BlockItems automatically
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Greeny.MODID);

    // --- EXISTING BLOCKS ---
    public static final RegistryObject<Block> AXIOM_HEART_CORE = BLOCKS.register("axiom_heart_core",
            () -> new AxiomHeartBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE)
                    .strength(-1.0F, 3600000.0F)
                    .lightLevel((state) -> 15)
                    .noOcclusion()));

    public static final RegistryObject<Block> ANCIENT_ROOT = BLOCKS.register("ancient_root",
            () -> new PhasingRootBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .isViewBlocking((state, level, pos) -> false)
            ));

    // --- NEW TREE BLOCKS ---

    // 1. THE LOG (RotatedPillarBlock allows it to be placed sideways)
    public static final RegistryObject<Block> WHISPERING_OAK_LOG = registerBlock("whispering_oak_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG).mapColor(MapColor.COLOR_CYAN)));

    // 2. THE LEAVES (LeavesBlock handles transparency and decay)
    public static final RegistryObject<Block> WHISPERING_OAK_LEAVES = registerBlock("whispering_oak_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES).noOcclusion()));

    // 3. THE SAPLING (SaplingBlock needs the TreeGrower we created)
    public static final RegistryObject<Block> WHISPERING_OAK_SAPLING = registerBlock("whispering_oak_sapling",
            () -> new SaplingBlock(new WhisperingOakTreeGrower(), BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING)));


    // --- HELPERS ---

    // Registers the Block AND the Item for it at the same time
    private static <T extends Block> RegistryObject<T> registerBlock(String name, java.util.function.Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        // This was failing because ITEMS wasn't defined at the top
        ITEMS.register(name, () -> new BlockItem(toReturn.get(), new Item.Properties()));
        return toReturn;
    }

    public static final RegistryObject<Block> AXIOM_SOIL = BLOCKS.register("axiom_soil",
            () -> new AxiomSoilBlock(BlockBehaviour.Properties.copy(Blocks.DIRT).strength(0.8f).mapColor(MapColor.COLOR_CYAN)));

    public static final RegistryObject<Block> PHASING_SPROUT = BLOCKS.register("phasing_sprout",
            () -> new PhasingSproutBlock(BlockBehaviour.Properties.copy(Blocks.GRASS).noCollission().instabreak().sound(SoundType.GRASS)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }
}