package com.tibolatte.greeny.blocks;

import com.tibolatte.greeny.registry.BlockRegistry;
import com.tibolatte.greeny.registry.ParticleRegistry; // Import Lodestone Registry
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level; // Import Level for animateTick
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder; // Import Builder
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;

import java.awt.Color; // Import Java AWT Color

public class PhasingSproutBlock extends BushBlock {
    protected static final VoxelShape SHAPE = box(2.0D, 0.0D, 2.0D, 14.0D, 6.0D, 14.0D);

    public PhasingSproutBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    // SERVER-SIDE LOGIC (Terraforming)
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

    // CLIENT-SIDE LOGIC (Particles)
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // Chance: Only spawn particles occasionally (20% chance per tick) to keep it "faint"
        if (random.nextFloat() < 0.1f) {

            // Random position inside the sprout
            double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4;
            double y = pos.getY() + 0.2 + (random.nextDouble() * 0.4);
            double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4;

            WorldParticleBuilder.create(ParticleRegistry.MANA_WISP.get()) // Use WISP for soft glow
                    .setTransparencyData(GenericParticleData.create(0.8f, 0.0f).build()) // Start faint, fade to 0
                    .setScaleData(GenericParticleData.create(0.2f, 0.0f).build()) // Tiny size
                    .setColorData(ColorParticleData.create(
                            new Color(132, 47, 147), // Your Color (#842F93)
                            new Color(80, 20, 100)   // Fade to darker purple
                    ).build())
                    .setLifetime(30 + random.nextInt(20))
                    .setMotion(0, 0.01, 0) // Slowly float up
                    .enableNoClip()
                    .spawn(level, x, y, z);
        }
    }
}