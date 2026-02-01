package com.tibolatte.greeny.blocks;

import com.tibolatte.greeny.registry.MobEffectRegistry;
import com.tibolatte.greeny.registry.ParticleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
}