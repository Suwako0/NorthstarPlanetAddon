package com.suwako.northstarplanetaddon.venus.client.renderer;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.client.model.AcidJellyModel;
import com.suwako.northstarplanetaddon.venus.entity.AcidJellyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 酸潮水母渲染器。 */
public class AcidJellyRenderer extends MobRenderer<AcidJellyEntity, AcidJellyModel> {

    private static final ResourceLocation TEXTURE = VenusModule.id("textures/entity/acid_jelly.png");

    public AcidJellyRenderer(EntityRendererProvider.Context context) {
        super(context, new AcidJellyModel(context.bakeLayer(AcidJellyModel.LAYER)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(AcidJellyEntity entity) {
        return TEXTURE;
    }
}
