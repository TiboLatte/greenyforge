package com.tibolatte.greeny.blocks;

import com.tibolatte.greeny.blocks.entity.AxiomHeartBlockEntity;
import com.tibolatte.greeny.registry.BlockRegistry;
import com.tibolatte.greeny.registry.ItemRegistry;
import com.tibolatte.greeny.registry.ParticleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;

import javax.annotation.Nullable;
import java.awt.Color;

public class PhasingSproutBlock extends BushBlock {

    protected static final VoxelShape SHAPE = box(2.0D, 0.0D, 2.0D, 14.0D, 6.0D, 14.0D);

    public PhasingSproutBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // =============================================================
    //               INTERACTION LOGIC (The Fix)
    // =============================================================
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {

        // 1. Ensure Server Side
        if (!level.isClientSide) {

            // 2. CHECK TOOL
            if (tool.is(Items.SHEARS)) {
                // --- PRUNING (GOOD) ---
                player.playSound(SoundEvents.SHEEP_SHEAR, 1.0f, 1.2f);
                popResource(level, pos, new ItemStack(ItemRegistry.AXIOM_DUST.get()));

                // --- THIS WAS MISSING IN YOUR CODE ---
                rewardNearbyHeart(level, pos, player);
                // ------------------------------------

            } else {
                // --- BRUTE FORCE (BAD) ---
                notifyNearbyHeart(level, pos);
            }
        }

        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    // =============================================================
    //               HELPER METHODS (Scanning)
    // =============================================================

    private void rewardNearbyHeart(Level level, BlockPos pos, Player player) {
        // Scan 30 blocks (Increased range as discussed)
        int radius = 30;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-radius, -10, -radius), pos.offset(radius, 10, radius))) {
            if (level.getBlockEntity(p) instanceof AxiomHeartBlockEntity heart) {
                // Call the boost (+5%)
                heart.boostResonance(player.getUUID(), 0.05f);

                // Visual/Audio Feedback so you know it worked
                level.playSound(null, p, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0f, 2.0f);
                level.globalLevelEvent(2005, p, 0);
                return; // Stop after finding one
            }
        }
    }

    private void notifyNearbyHeart(Level level, BlockPos pos) {
        // Scan 20 blocks for a Heart to anger
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-15, -10, -15), pos.offset(15, 10, 15))) {
            if (level.getBlockEntity(p) instanceof AxiomHeartBlockEntity heart) {
                heart.triggerAnger();
                return;
            }
        }
    }

    // =============================================================
    //               VISUALS & GROWTH
    // =============================================================

    @Override
    public boolean isRandomlyTicking(BlockState state) { return true; }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            BlockPos below = pos.below();
            BlockState soil = level.getBlockState(below);
            if (soil.is(Blocks.GRASS_BLOCK) || soil.is(Blocks.DIRT)) {
                level.setBlock(below, BlockRegistry.AXIOM_SOIL.get().defaultBlockState(), 3);
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() < 0.1f) {
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            double y = pos.getY() + 0.2 + (random.nextDouble() * 0.4);
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;

            WorldParticleBuilder.create(ParticleRegistry.MANA_WISP.get())
                    .setTransparencyData(GenericParticleData.create(0.8f, 0.0f).build())
                    .setScaleData(GenericParticleData.create(0.2f, 0.0f).build())
                    .setColorData(ColorParticleData.create(new Color(132, 47, 147), new Color(80, 20, 100)).build())
                    .setLifetime(30 + random.nextInt(20))
                    .setMotion(0, 0.01, 0)
                    .enableNoClip()
                    .spawn(level, x, y, z);
        }
    }
}