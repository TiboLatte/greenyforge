package com.tibolatte.greeny.datagen;

import com.tibolatte.greeny.Greeny;
import com.tibolatte.greeny.registry.BlockRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class GreenyBlockStateProvider extends BlockStateProvider {
    public GreenyBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Greeny.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // Simple cube for the core
        simpleBlock(BlockRegistry.AXIOM_HEART_CORE.get());
        // Pillar for roots
        logBlock((RotatedPillarBlock) BlockRegistry.ANCIENT_ROOT.get());
    }
}