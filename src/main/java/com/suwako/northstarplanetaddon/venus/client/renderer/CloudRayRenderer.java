package com.suwako.northstarplanetaddon.venus.client.renderer;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.client.model.CloudRayModel;
import com.suwako.northstarplanetaddon.venus.entity.CloudRayEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 云帆鳐渲染器。 */
public class CloudRayRenderer extends MobRenderer<CloudRayEntity, CloudRayModel> {

    private static final ResourceLocation TEXTURE = VenusModule.id("textures/entity/cloud_ray.png");

    public CloudRayRenderer(EntityRendererProvider.Context context) {
        super(context, new CloudRayModel(context.bakeLayer(CloudRayModel.LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CloudRayEntity entity) {
        return TEXTURE;
    }
}
