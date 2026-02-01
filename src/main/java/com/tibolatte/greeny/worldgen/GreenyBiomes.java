package com.tibolatte.greeny.worldgen;

import com.tibolatte.greeny.registry.BiomeRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.*;

public class GreenyBiomes {

    public static void bootstrap(BootstapContext<Biome> context) {
        context.register(BiomeRegistry.AXIOM_GROVE, createAxiomGrove(context));
    }

    private static Biome createAxiomGrove(BootstapContext<Biome> context) {
        // 1. MOBS (Loups et Lapins pour l'instant)
        MobSpawnSettings.Builder spawnBuilder = new MobSpawnSettings.Builder();
        spawnBuilder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.WOLF, 5, 4, 4));
        spawnBuilder.addSpawn(MobCategory.CREATURE, new MobSpawnSettings.SpawnerData(EntityType.RABBIT, 4, 2, 3));

        // 2. EFFETS VISUELS
        BiomeSpecialEffects.Builder effects = new BiomeSpecialEffects.Builder()
                .waterColor(0x1B4D3E)
                .waterFogColor(0x052b21)

                // FIX: Lightened the fog and sky slightly to prevent rendering glitches
                .fogColor(0x4F7A7A)         // A bit lighter Cyan/Green
                .skyColor(0x526675)         // Lighter, less "black", avoids flickering

                .grassColorOverride(0x409c6d)
                .foliageColorOverride(0x358f6c);

                // LA SOLUTION : SPORE_BLOSSOM_AIR
                // Ce sont des particules vertes qui flottent naturellement.
                // C'est très "Lodestone-like" visuellement, mais compatible Biome.

        // 3. GENERATION
        BiomeGenerationSettings.Builder generation = new BiomeGenerationSettings.Builder(
                context.lookup(net.minecraft.core.registries.Registries.PLACED_FEATURE),
                context.lookup(net.minecraft.core.registries.Registries.CONFIGURED_CARVER)
        );

        globalOverworldGeneration(generation);
        BiomeDefaultFeatures.addForestFlowers(generation);
        BiomeDefaultFeatures.addDefaultOres(generation);
        BiomeDefaultFeatures.addOtherBirchTrees(generation);

        return new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .downfall(0.8f)
                .temperature(0.7f)
                .generationSettings(generation.build())
                .mobSpawnSettings(spawnBuilder.build())
                .specialEffects(effects.build())
                .build();
    }

    private static void globalOverworldGeneration(BiomeGenerationSettings.Builder builder) {
        BiomeDefaultFeatures.addDefaultCarversAndLakes(builder);
        BiomeDefaultFeatures.addDefaultCrystalFormations(builder);
        BiomeDefaultFeatures.addDefaultMonsterRoom(builder);
        BiomeDefaultFeatures.addDefaultUndergroundVariety(builder);
        BiomeDefaultFeatures.addDefaultSprings(builder);
        BiomeDefaultFeatures.addSurfaceFreezing(builder);
    }
}