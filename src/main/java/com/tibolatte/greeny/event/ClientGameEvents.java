package com.tibolatte.greeny.event;

import com.tibolatte.greeny.Greeny;
import com.tibolatte.greeny.blocks.entity.AxiomHeartBlockEntity;
import com.tibolatte.greeny.registry.MobEffectRegistry;
import com.tibolatte.greeny.registry.ParticleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;

import java.awt.*;

// Note: bus = Bus.FORGE because we are listening to game ticks, not registration
@Mod.EventBusSubscriber(modid = Greeny.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientGameEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Only run on Client, during the END phase (so position is updated), and for the actual player
        if (event.phase == TickEvent.Phase.END && event.player.level().isClientSide) {
            Player player = event.player;

            // CHECK: Does player have the Forest Mark?
            if (player.hasEffect(MobEffectRegistry.FOREST_MARK.get())) {
                spawnHarmonyTrail(player);
            }
        }
    }

    private static void spawnHarmonyTrail(Player player) {
        // Optimization: Don't spawn every single tick (too dense). 30% chance is enough.
        if (player.getRandom().nextFloat() > 0.3f) return;

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Random offset near feet
        double offsetX = (player.getRandom().nextDouble() - 0.5) * 0.5;
        double offsetZ = (player.getRandom().nextDouble() - 0.5) * 0.5;

        // VISUAL: A soft, lingering dust that stays behind
        WorldParticleBuilder.create(ParticleRegistry.MANA_WISP.get())
                // Cyan to Transparent
                .setColorData(ColorParticleData.create(new Color(0, 255, 255), new Color(0, 50, 100, 0)).build())
                .setTransparencyData(GenericParticleData.create(0.5f, 0.0f).build()) // Starts visible, fades out
                .setScaleData(GenericParticleData.create(0.08f, 0.0f).build()) // Small
                .setLifetime(30) // Stays for 1.5 seconds
                .enableNoClip()
                // Stationary! It stays where you stepped.
                .setMotion(0, 0.005, 0)
                .spawn(player.level(), x + offsetX, y, z + offsetZ);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // 1. Only care if it happens on Server
        if (event.getEntity().level().isClientSide) return;

        // 2. Only care if an ANIMAL died (Sheep, Cow, Pig, etc.)
        // (You can also add check for "Player" damage source if you want to allow nature to kill nature)
        if (event.getEntity() instanceof Animal) {

            Level level = event.getEntity().level();
            BlockPos deathPos = event.getEntity().blockPosition();

            // 3. Scan for the Heart nearby (Radius 10)
            // Optimization: We scan a cube 10x10x10 centered on the death
            int radius = 10;
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        BlockPos checkPos = deathPos.offset(x, y, z);

                        // Check if block entity exists here
                        if (level.getBlockEntity(checkPos) instanceof AxiomHeartBlockEntity heart) {

                            // 4. TRIGGER THE ANGER
                            heart.triggerAnger();

                            // Optional: Break out after finding one (so we don't trigger multiple hearts)
                            return;
                        }
                    }
                }
            }
        }
    }
}