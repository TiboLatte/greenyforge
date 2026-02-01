package com.tibolatte.greeny.datagen;

import com.tibolatte.greeny.Greeny;
import com.tibolatte.greeny.registry.BlockRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class GreenyItemModelProvider extends ItemModelProvider {

    public GreenyItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        // "azureveil" must match your MOD_ID
        super(output, Greeny.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // 1. Axiom Heart Core
        // This helper tells the game: "The item model is exactly the same as the block model."
        simpleBlockItem(BlockRegistry.AXIOM_HEART_CORE);

        // 2. Ancient Root
        // Logs are special because they rotate. For the item, we force it to look like
        // the default (upright) block model.
        // Note: 'ancient_root' must match the name generated in your BlockStateProvider.
        withExistingParent(BlockRegistry.ANCIENT_ROOT.getId().getPath(),
                modLoc("block/ancient_root"));
    }

    // A helper method to reduce code repetition
    private void simpleBlockItem(RegistryObject<Block> block) {
        withExistingParent(block.getId().getPath(),
                modLoc("block/" + block.getId().getPath()));
    }
}