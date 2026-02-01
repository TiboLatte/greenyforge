package com.tibolatte.greeny.client.renderer;

import com.tibolatte.greeny.client.model.AxiomGuardianModel;
import com.tibolatte.greeny.mobs.AxiomGuardianEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AxiomGuardianRenderer extends GeoEntityRenderer<AxiomGuardianEntity> {
    public AxiomGuardianRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AxiomGuardianModel());
    }
}