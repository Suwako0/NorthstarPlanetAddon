package com.suwako.northstarplanetaddon.venus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.suwako.northstarplanetaddon.venus.entity.boss.HephaestusEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * 护甲板独立破碎的表现层。
 *
 * <p>模型里 6 块大甲片各占一根骨（{@code plate_1} 右肩 / {@code plate_2} 左肩 / {@code plate_3} 胸 /
 * {@code plate_4} 腹 / {@code plate_5} 背 / {@code plate_6} 头），骨骼的静止姿态与原甲片完全一致，
 * 所以不打碎时外形没有任何变化；一旦服务端把对应的位写进 {@code DATA_PLATE_MASK}，
 * 这里就把那根骨隐藏 —— 玩家看到的是「这块甲片被打飞了」。</p>
 *
 * <p>必须注册在 {@link software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer} <b>之前</b>：
 * 自发光层会重新画一遍模型，隐藏要先生效，否则碎掉的甲片还会发一帧光。</p>
 */
public class HephaestusPlateLayer extends GeoRenderLayer<HephaestusEntity> {

    /** 顺序必须与 {@code HephaestusEntity#PLATE_DEBRIS} 一致。 */
    private static final String[] PLATES = {
            "plate_1", "plate_2", "plate_3", "plate_4", "plate_5", "plate_6"
    };

    public HephaestusPlateLayer(GeoRenderer<HephaestusEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, HephaestusEntity entity, BakedGeoModel model, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                       int packedLight, int packedOverlay) {
        int mask = entity.getPlateMask();
        for (int i = 0; i < PLATES.length; i++) {
            boolean broken = (mask & (1 << i)) != 0;
            model.getBone(PLATES[i]).ifPresent(bone -> bone.setHidden(broken));
        }
    }
}
