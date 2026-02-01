package com.tibolatte.greeny.datagen;

import com.tibolatte.greeny.registry.BlockRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.Set;

public class GreenyLootTableProvider {
    // This is the method you call in DataGenerators.java
    public static LootTableProvider create(PackOutput output) {
        return new LootTableProvider(output, Set.of(), List.of(
                new LootTableProvider.SubProviderEntry(AzureveilBlockLoot::new, LootContextParamSets.BLOCK)
        ));
    }

    // Inner class to handle block drops
    public static class AzureveilBlockLoot extends BlockLootSubProvider {
        public AzureveilBlockLoot() {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags());
        }

        @Override
        protected void generate() {
            // Drop itself when broken
            this.dropSelf(BlockRegistry.AXIOM_HEART_CORE.get());
            this.dropSelf(BlockRegistry.ANCIENT_ROOT.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            // Tell the provider which blocks belong to us
            return BlockRegistry.BLOCKS.getEntries().stream().map(RegistryObject::get).toList();
        }
    }
}