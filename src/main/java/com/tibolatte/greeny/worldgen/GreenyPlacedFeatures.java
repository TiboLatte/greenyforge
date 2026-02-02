package com.tibolatte.greeny.worldgen;

import com.tibolatte.greeny.Greeny;
import com.tibolatte.greeny.registry.BlockRegistry;
import com.tibolatte.greeny.worldgen.tree.GreenyTreeDecorator;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter; // Import this
import net.minecraft.core.HolderSet;    // Import this
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block; // Import this
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.RandomSpreadFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.ForkingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.UpwardsBranchingTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

public class GreenyPlacedFeatures {

    public static final ResourceKey<ConfiguredFeature<?, ?>> OAK_COMMON_KEY = createKey("whispering_oak_common");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OAK_RARE_KEY = createKey("whispering_oak_rare");
    public static final ResourceKey<ConfiguredFeature<?, ?>> OAK_SELECTOR_KEY = createKey("whispering_oak_selector");
    public static final ResourceKey<PlacedFeature> OAK_PLACED_KEY = createPlacedKey("whispering_oak_placed");

    public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
        // 1. GET THE REGISTRY LOOKUP (The Fix)
        // We need this to get the Log Block properly for the Mangrove Placer
        HolderGetter<Block> blockRegistry = context.lookup(Registries.BLOCK);

        // Prepare the "Can Grow Through" list (Just our log)
        Holder<Block> logHolder = blockRegistry.getOrThrow(BlockRegistry.WHISPERING_OAK_LOG.getKey());
        HolderSet<Block> canGrowThrough = HolderSet.direct(logHolder);

        // A. COMMON TREE
        FeatureUtils.register(context, OAK_COMMON_KEY, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(BlockRegistry.WHISPERING_OAK_LOG.get()),
                new ForkingTrunkPlacer(6, 4, 3),
                BlockStateProvider.simple(BlockRegistry.WHISPERING_OAK_LEAVES.get()),
                new RandomSpreadFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), ConstantInt.of(2), 70),
                new TwoLayersFeatureSize(1, 0, 2)
        )
                .ignoreVines()
                .dirt(BlockStateProvider.simple(BlockRegistry.ANCIENT_ROOT.get()))
                .build());

        // B. THE ELDER AXIOM TREE (Rare)
        FeatureUtils.register(context, OAK_RARE_KEY, Feature.TREE, new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(BlockRegistry.WHISPERING_OAK_LOG.get()),

                // SKELETON: Mangrove Shape
                // The last argument is now 'canGrowThrough' (Fixed)
                new UpwardsBranchingTrunkPlacer(10, 5, 4,
                        UniformInt.of(1, 3), 0.5f, UniformInt.of(0, 1),
                        canGrowThrough),

                BlockStateProvider.simple(BlockRegistry.WHISPERING_OAK_LEAVES.get()),

                // BASE FOLIAGE
                new RandomSpreadFoliagePlacer(ConstantInt.of(3), ConstantInt.of(0), ConstantInt.of(2), 60),

                new TwoLayersFeatureSize(2, 1, 2)
        )
                .ignoreVines()
                .dirt(BlockStateProvider.simple(BlockRegistry.ANCIENT_ROOT.get()))

                // DECORATOR
                .decorators(List.of(GreenyTreeDecorator.INSTANCE))

                .build());

        // C. SELECTOR
        FeatureUtils.register(context, OAK_SELECTOR_KEY, Feature.RANDOM_SELECTOR,
                new RandomFeatureConfiguration(
                        List.of(new WeightedPlacedFeature(
                                PlacementUtils.inlinePlaced(context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(OAK_RARE_KEY),
                                        PlacementUtils.filteredByBlockSurvival(BlockRegistry.WHISPERING_OAK_SAPLING.get())),
                                0.01f)),
                        PlacementUtils.inlinePlaced(context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(OAK_COMMON_KEY))
                ));
    }

    public static void bootstrapPlaced(BootstapContext<PlacedFeature> context) {
        var configuredFeature = context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(OAK_SELECTOR_KEY);
        List<PlacementModifier> modifiers = List.of(
                PlacementUtils.countExtra(3, 0.5f, 1),
                InSquarePlacement.spread(),
                SurfaceWaterDepthFilter.forMaxDepth(0),
                HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR),
                PlacementUtils.filteredByBlockSurvival(BlockRegistry.WHISPERING_OAK_SAPLING.get()),
                BiomeFilter.biome()
        );
        context.register(OAK_PLACED_KEY, new PlacedFeature(configuredFeature, modifiers));
    }

    private static ResourceKey<ConfiguredFeature<?, ?>> createKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation(Greeny.MODID, name));
    }
    private static ResourceKey<PlacedFeature> createPlacedKey(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation(Greeny.MODID, name));
    }
}