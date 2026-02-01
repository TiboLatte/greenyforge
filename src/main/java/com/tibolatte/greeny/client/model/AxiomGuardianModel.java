package com.tibolatte.greeny.client.model;

import com.tibolatte.greeny.Greeny;
import com.tibolatte.greeny.mobs.AxiomGuardianEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AxiomGuardianModel extends GeoModel<AxiomGuardianEntity> {

    @Override
    public ResourceLocation getModelResource(AxiomGuardianEntity object) {
        // Point to: src/main/resources/assets/greeny/geo/axiom_guardian.geo.json
        return new ResourceLocation(Greeny.MODID, "geo/axiom_guardian.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AxiomGuardianEntity object) {
        // Point to: src/main/resources/assets/greeny/textures/entity/axiom_guardian.png
        return new ResourceLocation(Greeny.MODID, "textures/entity/axiom_guardian.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AxiomGuardianEntity object) {
        // Point to: src/main/resources/assets/greeny/animations/axiom_guardian.animation.json
        return new ResourceLocation(Greeny.MODID, "animations/axiom_guardian.animation.json");
    }
}