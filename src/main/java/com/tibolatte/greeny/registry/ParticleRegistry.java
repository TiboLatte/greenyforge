package com.tibolatte.greeny.registry;

import com.tibolatte.greeny.Greeny;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType; // IMPORT THIS
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.tibolatte.greeny.Greeny;
import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
// Import the Malum/Lodestone type
import team.lodestar.lodestone.systems.particle.world.type.LodestoneWorldParticleType;

public class ParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Greeny.MODID);

    // 1. The Good Magic (Soft Wisp)
    public static final RegistryObject<LodestoneWorldParticleType> MANA_WISP =
            PARTICLES.register("mana_wisp", LodestoneWorldParticleType::new);

    // 2. The Bad Magic (Sharp Star)
    public static final RegistryObject<LodestoneWorldParticleType> STAR_PARTICLE =
            PARTICLES.register("star", LodestoneWorldParticleType::new);

    // 3. The Atmosphere (Animated Smoke)
    public static final RegistryObject<LodestoneWorldParticleType> SMOKE_PARTICLE =
            PARTICLES.register("smoke", LodestoneWorldParticleType::new);

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }
}