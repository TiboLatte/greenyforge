package com.tibolatte.greeny.blocks;

import com.tibolatte.greeny.blocks.entity.AxiomHeartBlockEntity;
import com.tibolatte.greeny.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class AxiomHeartBlock extends Block implements EntityBlock {

    // 1. DEFINE THE PROPERTY
    public static final EnumProperty<HeartState> STATE = EnumProperty.create("state", HeartState.class);

    public AxiomHeartBlock(Properties pProperties) {
        super(pProperties
                // 2. DYNAMIC LIGHTING LOGIC
                // Active = Bright (15), Angry = Dim (10), Dormant = Dark (0)
                .lightLevel(state -> {
                    if (!state.hasProperty(STATE)) return 15; // Safety check
                    return switch (state.getValue(STATE)) {
                        case ACTIVE -> 15;
                        case ANGRY -> 10;
                        case DORMANT -> 0;
                    };
                })
        );
        // 3. SET DEFAULT STATE
        this.registerDefaultState(this.stateDefinition.any().setValue(STATE, HeartState.ACTIVE));
    }

    // 4. REGISTER THE PROPERTY
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATE);
    }

    // Standard Block Entity Stuff (You already had most of this)
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AxiomHeartBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // We run on both CLIENT (particles) and SERVER (logic)
        return createTickerHelper(type, BlockEntityRegistry.AXIOM_HEART.get(), AxiomHeartBlockEntity::tick);
    }

    // This helper prevents crashes if the wrong tile entity type is loaded
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actualType, BlockEntityType<E> targetType, BlockEntityTicker<E> ticker) {
        return targetType == actualType ? (BlockEntityTicker<A>) ticker : null;
    }

    // OPTIONAL: Ensure RenderShape is MODEL (Should be default for Block, but good safety)
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}