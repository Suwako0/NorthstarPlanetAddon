package com.suwako.northstarplanetaddon.moon.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.client.model.MoonShellCrabModel;
import com.suwako.northstarplanetaddon.moon.entity.MoonShellCrabEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders 月壳蟹 with {@link MoonShellCrabModel}. */
public class MoonShellCrabRenderer extends MobRenderer<MoonShellCrabEntity, MoonShellCrabModel> {

    /** 64x64 sheet; the UV slots are described in {@link MoonShellCrabModel}. */
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MoonModule.NAMESPACE, "textures/entity/moon_shell_crab.png");

    public MoonShellCrabRenderer(EntityRendererProvider.Context context) {
        super(context, new MoonShellCrabModel(context.bakeLayer(MoonShellCrabModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(MoonShellCrabEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(MoonShellCrabEntity entity, PoseStack poseStack, float partialTick) {
        if (entity.isBaby()) {
            poseStack.scale(0.6F, 0.6F, 0.6F);
        }
    }
}
