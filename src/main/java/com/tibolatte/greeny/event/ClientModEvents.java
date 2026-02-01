package com.tibolatte.greeny.event;

import com.tibolatte.greeny.Greeny;
import com.tibolatte.greeny.client.renderer.AxiomGuardianRenderer;
import com.tibolatte.greeny.registry.EntityRegistry;
import com.tibolatte.greeny.registry.ParticleRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import team.lodestar.lodestone.systems.particle.world.type.LodestoneWorldParticleType;

// *** CHECK THESE 3 PARAMETERS ***
@Mod.EventBusSubscriber(modid = Greeny.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        // Register all of them using the Lodestone Factory
        event.registerSpriteSet(ParticleRegistry.MANA_WISP.get(), LodestoneWorldParticleType.Factory::new);
        event.registerSpriteSet(ParticleRegistry.STAR_PARTICLE.get(), LodestoneWorldParticleType.Factory::new);
        event.registerSpriteSet(ParticleRegistry.SMOKE_PARTICLE.get(), LodestoneWorldParticleType.Factory::new);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        // Force the game to render transparency for the Roots
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                com.tibolatte.greeny.registry.BlockRegistry.ANCIENT_ROOT.get(),
                net.minecraft.client.renderer.RenderType.translucent() // <--- THIS IS KEY
        );

        // Also set it for the Heart if you want that to glow/be transparent
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                com.tibolatte.greeny.registry.BlockRegistry.AXIOM_HEART_CORE.get(),
                net.minecraft.client.renderer.RenderType.translucent()
        );
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.AXIOM_GUARDIAN.get(), AxiomGuardianRenderer::new);
    }


}