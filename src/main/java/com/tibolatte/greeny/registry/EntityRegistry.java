package com.tibolatte.greeny.registry;

import com.tibolatte.greeny.Greeny;
import com.tibolatte.greeny.mobs.AxiomGuardianEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = Greeny.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EntityRegistry {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Greeny.MODID);

    public static final RegistryObject<EntityType<AxiomGuardianEntity>> AXIOM_GUARDIAN =
            ENTITIES.register("axiom_guardian",
                    () -> EntityType.Builder.of(AxiomGuardianEntity::new, MobCategory.MISC)
                            .sized(0.9f, 1.4f) // Size of a small horse/deer
                            .build(new ResourceLocation(Greeny.MODID, "axiom_guardian").toString()));

    // REQUIRED: Bind the attributes (Health, Speed) to the entity
    @SubscribeEvent
    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(AXIOM_GUARDIAN.get(), AxiomGuardianEntity.createAttributes().build());
    }
}