package com.tibolatte.greeny.registry;

import com.tibolatte.greeny.Greeny;
import com.tibolatte.greeny.blocks.AxiomHeartBlock;
import com.tibolatte.greeny.blocks.PhasingRootBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Greeny.MODID);

    // The core of the heart - emissive and pulsing
    public static final RegistryObject<Block> AXIOM_HEART_CORE = BLOCKS.register("axiom_heart_core",
            () -> new AxiomHeartBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE)
                    .strength(-1.0F, 3600000.0F) // Unbreakable
                    .lightLevel((state) -> 15)
                    .noOcclusion()));

    // The twisted roots
    public static final RegistryObject<Block> ANCIENT_ROOT = BLOCKS.register("ancient_root",
            () -> new PhasingRootBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion() // Vital for transparency
                    .isViewBlocking((state, level, pos) -> false) // Vital for lighting
            ));
}
