package com.suwako.northstarplanetaddon.moon.client.renderer;

import com.suwako.northstarplanetaddon.moon.entity.MoonlightBoltEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The bolt is rendered by server-synchronised GLOW/END_ROD particles, so its entity renderer only
 * supplies the mandatory client registration and deliberately draws no extra textured geometry.
 */
public final class MoonlightBoltRenderer extends EntityRenderer<MoonlightBoltEntity> {

    public MoonlightBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(MoonlightBoltEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
