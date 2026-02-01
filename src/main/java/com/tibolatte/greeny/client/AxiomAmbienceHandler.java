package com.tibolatte.greeny.client;

import com.tibolatte.greeny.Greeny;
import com.tibolatte.greeny.registry.BiomeRegistry;
import com.tibolatte.greeny.registry.ParticleRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import team.lodestar.lodestone.systems.particle.builder.WorldParticleBuilder;
import team.lodestar.lodestone.systems.particle.data.GenericParticleData;
import team.lodestar.lodestone.systems.particle.data.color.ColorParticleData;

import java.awt.Color;
import java.util.Random;

@Mod.EventBusSubscriber(modid = Greeny.MODID, value = Dist.CLIENT)
public class AxiomAmbienceHandler {

    private static final Random random = new Random();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // On vérifie qu'on est en jeu et pas en pause
        Minecraft mc = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || mc.isPaused() || mc.player == null || mc.level == null) return;

        Player player = mc.player;
        BlockPos pos = player.blockPosition();

        // CHECK: Est-on dans l'Axiom Grove ?
        if (mc.level.getBiome(pos).is(BiomeRegistry.AXIOM_GROVE)) {
            spawnAxiomParticles(mc, player);
        }
    }

    private static void spawnAxiomParticles(Minecraft mc, Player player) {
        // 1. DENSITY CHECK
        // We run this loop 2 times per tick.
        // Before we had a 10% chance. Now we force 2 particles EVERY tick.
        // This is 20x more particles.
        for (int i = 0; i < 2; i++) {

            // Random position around player (16x16 area)
            double x = player.getX() + (random.nextDouble() - 0.5) * 16;
            double y = player.getY() + (random.nextDouble() - 0.5) * 12 + 2; // Bias upwards slightly
            double z = player.getZ() + (random.nextDouble() - 0.5) * 16;

            // 2. BRIGHTER COLORS
            // More Neon Green, less dark
            Color c1 = new Color(50, 255, 120);
            Color c2 = new Color(0, 180, 80);

            WorldParticleBuilder.create(ParticleRegistry.MANA_WISP.get())
                    .setColorData(ColorParticleData.create(c1, c2).build())

                    // 3. VISIBILITY BOOST
                    // Transparency: Starts at 1.0 (Fully Visible) -> Fades to 0
                    .setTransparencyData(GenericParticleData.create(1.0f, 0.0f).build())

                    // Scale: Starts at 0.25 (Much bigger) -> Shrinks to 0
                    .setScaleData(GenericParticleData.create(0.25f, 0.0f).build())

                    // Physics
                    .setMotion(0, 0.015, 0) // Slow float up
                    .setLifetime(60 + random.nextInt(60)) // Lives 3-6 seconds
                    .enableNoClip()
                    .spawn(mc.level, x, y, z);
        }
    }
}