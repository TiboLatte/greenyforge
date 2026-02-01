package com.tibolatte.greeny.registry;

import com.tibolatte.greeny.Greeny;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public class BiomeRegistry {

    // This defines the unique ID: "greeny:axiom_grove"
    public static final ResourceKey<Biome> AXIOM_GROVE = ResourceKey.create(
            Registries.BIOME,
            new ResourceLocation(Greeny.MODID, "axiom_grove")
    );

    public static void register() {
        // Just used to trigger class loading if needed later
    }
}