package com.tibolatte.greeny.event;

import com.tibolatte.greeny.Greeny;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Greeny.MODID)
public class ForestProtectionEvents {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        LevelAccessor level = event.getLevel();
        BlockPos pos = event.getPos();

        if (isInsideAzureveil(level, pos) && !hasForestMark(event.getPlayer())) {
            event.setCanceled(true);
            applyBacklash(event.getPlayer());
        }
    }

    // --- Helper Methods (Stubs) ---

    private static boolean isInsideAzureveil(LevelAccessor level, BlockPos pos) {
        // TODO: Implement biome check. For now, return false so you can build.
        return false;
    }

    private static boolean hasForestMark(Player player) {
        // TODO: Implement capability check. For now, return true (admin mode).
        return true;
    }

    private static void applyBacklash(Player player) {
        // TODO: Apply potion effect or damage.
    }
}