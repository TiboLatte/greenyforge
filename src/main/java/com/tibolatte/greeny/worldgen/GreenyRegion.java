package com.tibolatte.greeny.worldgen;

import com.mojang.datafixers.util.Pair;
import com.tibolatte.greeny.registry.BiomeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

public class GreenyRegion extends Region {

    public GreenyRegion(ResourceLocation name, int weight) {
        // RegionType.OVERWORLD = On ajoute ça au monde normal
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        // Cette méthode magique remplace certaines zones de "Forêt" par ton "Axiom Grove"
        this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
            // Remplace les forêts standard par ton biome
            builder.replaceBiome(Biomes.FOREST, BiomeRegistry.AXIOM_GROVE);

            // Si tu veux qu'il soit très fréquent pour tester, remplace aussi les plaines :
            // builder.replaceBiome(Biomes.PLAINS, BiomeRegistry.AXIOM_GROVE);
        });
    }
}