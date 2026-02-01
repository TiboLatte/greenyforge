package com.tibolatte.greeny.registry;

import com.tibolatte.greeny.Greeny;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MobEffectRegistry {

    // Create the DeferredRegister for MobEffects
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Greeny.MODID);

    // REGISTER: The Forest Mark Effect
    // Color: 0x00FFFF (Cyan/Mana Blue)
    public static final RegistryObject<MobEffect> FOREST_MARK = EFFECTS.register("forest_mark",
            () -> new ForestMarkEffect(MobEffectCategory.BENEFICIAL, 0x00FFFF));

    // Method to register the registry to the event bus in Greeny.java
    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }

    // Inner class defining the effect behavior
    public static class ForestMarkEffect extends MobEffect {
        protected ForestMarkEffect(MobEffectCategory category, int color) {
            super(category, color);
        }

        // We don't need special tick logic here because the logic is handled
        // by the Blocks (checking if player HAS the effect), not the effect itself.
        @Override
        public boolean isDurationEffectTick(int duration, int amplifier) {
            return false;
        }
    }
}