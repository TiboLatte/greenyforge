package com.tibolatte.greeny.worldgen.tree;

import com.tibolatte.greeny.worldgen.GreenyPlacedFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.jetbrains.annotations.Nullable;

public class WhisperingOakTreeGrower extends AbstractTreeGrower {
    @Nullable
    @Override
    protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource pRandom, boolean pHasFlowers) {
        // CORRECTION : On pointe vers le SELECTOR.
        // Ainsi, quand le joueur fait pousser un arbre, il a aussi 1% de chance d'avoir l'arbre rare.
        return GreenyPlacedFeatures.OAK_SELECTOR_KEY;
    }
}