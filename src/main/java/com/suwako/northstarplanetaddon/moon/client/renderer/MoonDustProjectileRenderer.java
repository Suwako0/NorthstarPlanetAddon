package com.suwako.northstarplanetaddon.moon.client.renderer;

import com.suwako.northstarplanetaddon.moon.entity.MoonDustProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Moon dust is rendered by its synchronised particle trail, so this entity has no textured mesh. */
public final class MoonDustProjectileRenderer extends EntityRenderer<MoonDustProjectileEntity> {

    public MoonDustProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(MoonDustProjectileEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
