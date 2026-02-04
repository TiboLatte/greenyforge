package com.tibolatte.greeny.event;

import com.tibolatte.greeny.Greeny;
import com.tibolatte.greeny.blocks.entity.AxiomHeartBlockEntity;
import com.tibolatte.greeny.registry.BlockRegistry;
import com.tibolatte.greeny.registry.MobEffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Greeny.MODID)
public class SymbiosisEvents {

    // =============================================================
    //               THE ROOT STRIDER (Movement Speed)
    // =============================================================
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Run on Server (Phase.END) so the speed effect syncs correctly
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        Player player = event.player;

        // CHECK 1: Do they have the "Forest Mark" (Resonance > 50% or basic trust)?
        if (!player.hasEffect(MobEffectRegistry.FOREST_MARK.get())) return;

        // CHECK 2: Are they walking on our blocks?
        // We check the block directly under their feet (0.5 below pos)
        BlockPos belowPos = player.blockPosition().below();
        BlockState belowState = player.level().getBlockState(belowPos);

        if (belowState.is(BlockRegistry.ANCIENT_ROOT.get()) || belowState.is(BlockRegistry.AXIOM_SOIL.get())) {
            // APPLY BUFF: Speed II for 1 second (resets every tick while walking)
            // Duration 20 ticks (1s), Amplifier 1 (Speed II), Ambient (no particles), Visible (false)
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20, 1, true, false, false));
        }
    }

    // =============================================================
    //               ACTS OF SERVICE (Planting = Trust)
    // =============================================================
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        // CHECK 1: Did they plant the specific Sacred Sapling?
        if (event.getPlacedBlock().is(BlockRegistry.WHISPERING_OAK_SAPLING.get())) {

            // CHECK 2: Is there a Heart nearby to witness it?
            // We scan a small area (Radius 15) to find the nearest Heart.
            BlockPos pos = event.getPos();
            ServerLevel level = (ServerLevel) event.getLevel();

            // Optimization: Simple cubic scan
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-15, -10, -15), pos.offset(15, 10, 15))) {
                if (level.getBlockEntity(p) instanceof AxiomHeartBlockEntity heart) {

                    // FOUND HEART: Boost the player's score
                    // (We need to add this method to AxiomHeartBlockEntity, see below)
                    heart.boostResonance(player.getUUID(), 0.15f); // +15% Trust

                    // Visual Feedback (Bone Meal effect on the sapling)
                    level.globalLevelEvent(2005, pos, 0);
                    break; // Only boost one heart
                }
            }
        }
    }
}