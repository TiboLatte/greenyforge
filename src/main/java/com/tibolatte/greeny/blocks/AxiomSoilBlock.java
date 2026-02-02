package com.tibolatte.greeny.blocks;

import com.tibolatte.greeny.registry.MobEffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.IPlantable;

public class AxiomSoilBlock extends Block {
    public AxiomSoilBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canSustainPlant(BlockState state, BlockGetter world, BlockPos pos, net.minecraft.core.Direction facing, IPlantable plantable) {
        return true; // Highly fertile
    }

    // GAMEPLAY: Subtle buff for Attuned players standing on the "Pure" soil
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            // Only if they are Attuned (have the mark)
            if (living.hasEffect(MobEffectRegistry.FOREST_MARK.get())) {
                // Very short Regen (so it only lasts while standing on it)
                if (level.getGameTime() % 20 == 0) { // Optimize: Don't apply every tick
                    living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false, false));
                }
            }
        }
        super.stepOn(level, pos, state, entity);
    }
}