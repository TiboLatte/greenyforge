package com.tibolatte.greeny.datagen;

import com.tibolatte.greeny.Greeny;
import com.tibolatte.greeny.blocks.AxiomHeartBlock;
import com.tibolatte.greeny.blocks.HeartState;
import com.tibolatte.greeny.registry.BlockRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.common.data.ExistingFileHelper;

public class GreenyBlockStateProvider extends BlockStateProvider {
    public GreenyBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, Greeny.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {

        // --- 1. AXIOM HEART (The Complex State Logic) ---
        getVariantBuilder(BlockRegistry.AXIOM_HEART_CORE.get()).forAllStates(state -> {
            HeartState heartState = state.getValue(AxiomHeartBlock.STATE);

            String textureName;
            // Map states to your actual .png file names
            // CRITICAL: Ensure your files are named "axiom_heart_core_dormant.png" etc.
            switch (heartState) {
                case DORMANT -> textureName = "axiom_heart_core_dormant";
                default -> textureName = "axiom_heart_core_active"; // Maps Active AND Angry to Blue
            }

            return ConfiguredModel.builder()
                    .modelFile(models().cubeAll(textureName, modLoc("block/" + textureName)))
                    .build();
        });

        // --- 2. ROOTS & LOGS ---
        logBlock((RotatedPillarBlock) BlockRegistry.ANCIENT_ROOT.get());
        logBlock((RotatedPillarBlock) BlockRegistry.WHISPERING_OAK_LOG.get());

        // --- 3. LEAVES ---
        simpleBlockWithItem(BlockRegistry.WHISPERING_OAK_LEAVES.get(),
                models().cubeAll("whispering_oak_leaves", modLoc("block/whispering_oak_leaves")));

        // --- 4. PLANTS (Cutout) ---

        // Sapling
        simpleBlock(BlockRegistry.WHISPERING_OAK_SAPLING.get(),
                models().cross("whispering_oak_sapling", modLoc("block/whispering_oak_sapling")).renderType("cutout"));

        // Sprout
        simpleBlock(BlockRegistry.PHASING_SPROUT.get(),
                models().cross("phasing_sprout", modLoc("block/phasing_sprout")).renderType("cutout"));

        // --- 5. SOIL ---
        simpleBlockWithItem(BlockRegistry.AXIOM_SOIL.get(),
                models().cubeBottomTop("axiom_soil",
                        modLoc("block/axiom_soil_side"),   // Side
                        modLoc("block/axiom_soil_bottom"), // Bottom
                        modLoc("block/axiom_soil_top")     // Top
                ));
    }
}