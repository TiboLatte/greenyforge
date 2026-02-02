package com.tibolatte.greeny;

import com.mojang.logging.LogUtils;
import com.tibolatte.greeny.registry.*;
import com.tibolatte.greeny.worldgen.GreenyRegion;
import com.tibolatte.greeny.worldgen.tree.GreenyTreeDecorator;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import terrablender.api.Regions;

@Mod(Greeny.MODID)
public class Greeny {
    public static final String MODID = "greeny";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Greeny() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 1. Register Blocks
        BlockRegistry.BLOCKS.register(modEventBus);
        // 2. Register Items
        ItemRegistry.ITEMS.register(modEventBus);
        // 3. Register Block Entities
        BlockEntityRegistry.BLOCK_ENTITIES.register(modEventBus);
        ParticleRegistry.PARTICLES.register(modEventBus);
        MobEffectRegistry.EFFECTS.register(modEventBus);
        // Inside your Greeny() constructor:
        EntityRegistry.ENTITIES.register(modEventBus);
        GreenyTreeDecorator.DECORATOR_TYPES.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // On enregistre notre région.
            // Le poids "10" signifie qu'il sera assez fréquent.
            Regions.register(new GreenyRegion(new ResourceLocation(MODID, "axiom_region"), 10));
        });
    }
}

