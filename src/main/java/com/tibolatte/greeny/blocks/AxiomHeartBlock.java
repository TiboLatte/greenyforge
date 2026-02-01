package com.tibolatte.greeny.blocks;

import com.tibolatte.greeny.blocks.entity.AxiomHeartBlockEntity;
import com.tibolatte.greeny.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AxiomHeartBlock extends Block implements EntityBlock {
    public AxiomHeartBlock(Properties pProperties) {
        super(pProperties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AxiomHeartBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // OLD CODE might have said: return level.isClientSide ? null : ...
        // FIX: Remove the "level.isClientSide" check! We WANT it to run on the client for particles!

        return createTickerHelper(type, BlockEntityRegistry.AXIOM_HEART.get(), AxiomHeartBlockEntity::tick);
    }

    // Helper method to type-check the ticker
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType, BlockEntityType<E> targetType, BlockEntityTicker<E> ticker) {
        return targetType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }
}