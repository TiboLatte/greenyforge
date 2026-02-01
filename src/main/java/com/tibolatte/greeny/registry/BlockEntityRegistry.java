package com.tibolatte.greeny.registry;

import com.tibolatte.greeny.Greeny;
import com.tibolatte.greeny.blocks.entity.AxiomHeartBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Greeny.MODID);

    public static final RegistryObject<BlockEntityType<AxiomHeartBlockEntity>> AXIOM_HEART =
            BLOCK_ENTITIES.register("axiom_heart", () ->
                    BlockEntityType.Builder.of(AxiomHeartBlockEntity::new, BlockRegistry.AXIOM_HEART_CORE.get())
                            .build(null));
}