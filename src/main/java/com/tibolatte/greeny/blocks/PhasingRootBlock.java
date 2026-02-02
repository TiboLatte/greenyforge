package com.tibolatte.greeny.blocks;

import com.tibolatte.greeny.registry.BlockRegistry;
import com.tibolatte.greeny.registry.MobEffectRegistry;
import com.tibolatte.greeny.registry.ParticleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;

import java.awt.*;

public class PhasingRootBlock extends RotatedPillarBlock {

    public PhasingRootBlock(Properties properties) {
        super(properties);
    }

    // 1. MECHANIC: Collision (Server & Client Logic)
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext) {
            Entity entity = entityContext.getEntity();
            if (entity instanceof LivingEntity living) {
                // If they have the mark, it's solid wood.
                if (living.hasEffect(MobEffectRegistry.FOREST_MARK.get())) {
                    return super.getCollisionShape(state, level, pos, context);
                }
            }
        }
        return Shapes.empty(); // Ghost mode (Fall through)
    }

    // 2. VISUALS: The Smoke Logic
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Player localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;
        boolean isAttuned = localPlayer.hasEffect(MobEffectRegistry.FOREST_MARK.get());

        // DENSITY: 8 particles per tick creates a thick, continuous cloud.
        // (Note: If this causes lag with many blocks, try reducing to 4 or 5)
        int particleCount = 8;

        for(int i = 0; i < particleCount; i++) {
            // POSITION: Start roughly in the center but with random jitter
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.9;
            double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 0.9;
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.9;

            // MOTION: Calculate the spread (moving away from center or drifting)
            double xMotion = (random.nextDouble() - 0.5) * 0.04;
            double yMotion = (random.nextDouble() - 0.5) * 0.04;
            double zMotion = (random.nextDouble() - 0.5) * 0.04;

            if (!isAttuned) {
                // --- STATE: GHOST (Grey Smoke) ---
                WorldParticleBuilder.create(ParticleRegistry.SMOKE_PARTICLE.get())
                        .setColorData(ColorParticleData.create(new Color(50, 50, 50), new Color(0, 0, 0, 0)).build())
                        .setTransparencyData(GenericParticleData.create(0.5f, 0.0f).build())
                        .setScaleData(GenericParticleData.create(0.25f, 0.5f).build()) // Start small, puff up
                        .setLifetime(60 + random.nextInt(20))
                        // MOTION: Drift slowly + Rise up (Ghostly)
                        .setMotion(xMotion, 0.015, zMotion)
                        .spawn(level, x, y, z);
            } else {
                // --- STATE: SOLID (Cyan Smoke) ---
                WorldParticleBuilder.create(ParticleRegistry.SMOKE_PARTICLE.get())
                        // COLOR: Bright Cyan -> Deep Blue
                        .setColorData(ColorParticleData.create(new Color(0, 255, 255), new Color(0, 50, 200)).build())
                        .setTransparencyData(GenericParticleData.create(0.6f, 0.0f).build()) // Slightly more opaque
                        .setScaleData(GenericParticleData.create(0.25f, 0.1f).build()) // Start distinct, fade small
                        .setLifetime(40 + random.nextInt(20))
                        // MOTION: Jitter in place (Energy hum)
                        .setMotion(xMotion, yMotion, zMotion)
                        .spawn(level, x, y, z);
            }
        }
    }

    // 3. RENDERING: Allow Transparency
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentBlockState, net.minecraft.core.Direction side) {
        return adjacentBlockState.is(this) ? true : super.skipRendering(state, adjacentBlockState, side);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // LORE: The Root emits seeds/energy that become Sprouts.
        // 5% chance per tick (Slow spread)
        if (random.nextInt(20) == 0) {

            // Try to find a valid spot nearby (Radius 2)
            for (int i = 0; i < 3; i++) {
                BlockPos targetPos = pos.offset(random.nextInt(5) - 2, random.nextInt(3) - 1, random.nextInt(5) - 2);

                // Validation:
                // 1. Must be Air
                // 2. Block below must be valid soil (Grass, Dirt, or Axiom Soil)
                if (level.isEmptyBlock(targetPos)) {
                    BlockState belowState = level.getBlockState(targetPos.below());

                    if (belowState.is(Blocks.GRASS_BLOCK) || belowState.is(Blocks.DIRT) || belowState.is(BlockRegistry.AXIOM_SOIL.get())) {

                        // 3. DENSITY CHECK (Crucial for gameplay balance)
                        // Don't spawn if there are already sprouts nearby. We don't want a weed infestation.
                        int nearbySprouts = 0;
                        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -1, -3), pos.offset(3, 1, 3))) {
                            if (level.getBlockState(p).is(BlockRegistry.PHASING_SPROUT.get())) {
                                nearbySprouts++;
                            }
                        }

                        if (nearbySprouts < 2) { // Only grow if sparse
                            level.setBlock(targetPos, BlockRegistry.PHASING_SPROUT.get().defaultBlockState(), 3);
                            break; // Done
                        }
                    }
                }
            }
        }
    }
}