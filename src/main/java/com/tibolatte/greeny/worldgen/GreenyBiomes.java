package com.tibolatte.greeny.worldgen;

import com.tibolatte.greeny.registry.BiomeRegistry;
// Ensure this import is here so we can find the tree key
import com.tibolatte.greeny.worldgen.GreenyPlacedFeatures;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.GenerationStep;

public class GreenyBiomes {

    public static void bootstrap(BootstapContext<Biome> context) {
        context.register(BiomeRegistry.AXIOM_GROVE, createAxiomGrove(context));
    }

    private static Biome createAxiomGrove(BootstapContext<Biome> context) {
        // 1. SPAWN SETTINGS (Mobs)
        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
        spawnBuilder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 5, 4, 4));
        spawnBuilder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3));

        // 2. VISUAL EFFECTS
        BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder()
                .waterColor(0x1B4D3E)       // Dark Emerald Water
                .waterFogColor(0x052b21)
                .fogColor(0x4F7A7A)         // Soft Cyan Fog
                .skyColor(0x526675)         // Muted Sky
                .grassColorOverride(0x409c6d) // Teal-ish Grass
                .foliageColorOverride(0x358f6c); // Teal-ish Leaves

        // Note: No ambientParticle here.
        // We handle particles in 'AxiomAmbienceHandler.java' for better performance/visuals.

        // 3. GENERATION SETTINGS
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(
                context.lookup(net.minecraft.core.registries.Registries.PLACED_FEATURE),
                context.lookup(net.minecraft.core.registries.Registries.CONFIGURED_CARVER)
        );

        // A. Standard Features (Caves, Ores, Lakes, etc.)
        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addForestFlowers(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);

        // B. CUSTOM VEGETATION
        // We add our new "Selector" tree key here.
        // It handles the choice between Common Trees and Rare Heart Trees.
        generation.addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, GreenyPlacedFeatures.OAK_PLACED_KEY);

        return new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .downfall(0.8f)
                .temperature(0.7f)
                .generationSettings(generation.build())
                .mobSpawnSettings(spawnBuilder.build())
                .specialEffects(effects.build())
                .build();
    }

    // Helper method for standard Overworld generation
    private static void globalOverworldGeneration(BiomeGenerationSettings.Builder builder) {
        BiomeDefaultFeatures.addDefaultCarversAndLakes(builder);
        BiomeDefaultFeatures.addDefaultCrystalFormations(builder);
        BiomeDefaultFeatures.addDefaultMonsterRoom(builder);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(builder);
        BiomeDefaultFeatures.addDefaultSprings(builder);
        BiomeDefaultFeatures.addSurfaceFreezing(builder);
    }
}