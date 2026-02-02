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

    public static LootTableProvider create(PackOutput output) {
        return new LootTableProvider(output, Set.of(), List.of(
                // Renamed from AzureveilBlockLoot to GreenyBlockLoot
                new LootTableProvider.SubProviderEntry(GreenyBlockLoot::new, LootContextParamSets.BLOCK)
        ));
    }

    // Inner class to handle block drops
    public static class GreenyBlockLoot extends BlockLootSubProvider {
        public GreenyBlockLoot() {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags());
        }

        @Override
        protected void generate() {
            // --- EXISTING ---
            this.dropSelf(BlockRegistry.AXIOM_HEART_CORE.get());
            this.dropSelf(BlockRegistry.ANCIENT_ROOT.get());

            // --- NEW TREE DROPS ---

            // 1. Log drops Log
            this.dropSelf(BlockRegistry.WHISPERING_OAK_LOG.get());

            // 2. Sapling drops Sapling
            this.dropSelf(BlockRegistry.WHISPERING_OAK_SAPLING.get());

            // 3. Leaves drop Saplings (standard vanilla rates)
            this.add(BlockRegistry.WHISPERING_OAK_LEAVES.get(),
                    block -> createLeavesDrops(block, BlockRegistry.WHISPERING_OAK_SAPLING.get(), NORMAL_LEAVES_SAPLING_CHANCES));
            this.dropSelf(BlockRegistry.AXIOM_SOIL.get());
            this.dropSelf(BlockRegistry.PHASING_SPROUT.get());

        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            // Tell the provider which blocks belong to us
            return BlockRegistry.BLOCKS.getEntries().stream().map(RegistryObject::get).toList();
        }
    }
}