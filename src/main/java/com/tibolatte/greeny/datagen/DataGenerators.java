package com.tibolatte.greeny.datagen;

import com.tibolatte.greeny.Greeny;
import com.tibolatte.greeny.worldgen.GreenyBiomes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = Greeny.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // Block States & Models
        generator.addProvider(event.includeClient(), new GreenyBlockStateProvider(packOutput, existingFileHelper));
        // Item Models
        generator.addProvider(event.includeClient(), new GreenyItemModelProvider(packOutput, existingFileHelper));
        //LootTable
        generator.addProvider(event.includeServer(), GreenyLootTableProvider.create(packOutput));
        // Register the "WorldGen" provider
        generator.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(
                packOutput,
                lookupProvider,
                BUILDER,
                Set.of(Greeny.MODID)
        ));
    }

        // This tells the DataGen: "Look in GreenyBiomes.java to find what to generate"
        private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
                .add(Registries.BIOME, GreenyBiomes::bootstrap);
}