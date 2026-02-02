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
        super(output, Greeny.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // --- EXISTING ---
        withExistingParent(BlockRegistry.AXIOM_HEART_CORE.getId().getPath(), modLoc("block/axiom_heart_core_active"));        withExistingParent(BlockRegistry.ANCIENT_ROOT.getId().getPath(), modLoc("block/ancient_root"));

        // --- NEW TREE ITEMS ---

        // 1. Log: Looks like the block
        withExistingParent(BlockRegistry.WHISPERING_OAK_LOG.getId().getPath(), modLoc("block/whispering_oak_log"));

        // 2. Leaves: Handled by BlockStateProvider's "simpleBlockWithItem", but good to be safe:
        // (If you get duplicate errors, remove this line, but usually it's fine)
        // withExistingParent(BlockRegistry.WHISPERING_OAK_LEAVES.getId().getPath(), modLoc("block/whispering_oak_leaves"));

        // 3. Sapling: Looks flat (2D) in hand, like a vanilla sapling
        // We use "item/generated" parent so it looks like an item, not a 3D block
        getBuilder("whispering_oak_sapling")
                .parent(getExistingFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("block/whispering_oak_sapling"));
        // --- NEW SOIL & SPROUT ---

        // 1. Axiom Soil (Block Item)
        withExistingParent(BlockRegistry.AXIOM_SOIL.getId().getPath(), modLoc("block/axiom_soil"));

        // 2. Phasing Sprout (Flat 2D Item)
        getBuilder("phasing_sprout")
                .parent(getExistingFile(mcLoc("item/generated")))
                .texture("layer0", modLoc("block/phasing_sprout"));

    }

    private void simpleBlockItem(RegistryObject<Block> block) {
        withExistingParent(block.getId().getPath(), modLoc("block/" + block.getId().getPath()));
    }
}