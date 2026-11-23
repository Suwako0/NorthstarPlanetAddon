package com.suwako.northstarplanetaddon.mars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.entity.AresEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import javax.annotation.Nullable;

/**
 * 熔核巨像·阿瑞斯渲染：GeckoLib 自定义模型（约 3.4 格高的机械巨像，胸口有可暴露的星核）。
 *
 * <p>模型 / 动画仍按 GeckoLib 约定自动解析：{@code geo/entity/ares.geo.json}、
 * {@code animations/entity/ares.animation.json}；<b>贴图改为按阶段分档</b>
 * （见 {@link PhaseModel#getTextureResource}）：
 * phase 1 → {@code textures/entity/ares_phase1.png}，
 * phase 2 → {@code ares_phase2.png}，phase 3 → {@code ares_phase3.png}。
 * 三张图都是 128×128、与 {@code ares.png} 同一套 box UV 布局，只有配色 / 裂纹不同，
 * 所以换贴图不会改变任何 UV 映射。</p>
 *
 * <p>v1.9：核心温度 &gt; 1500℃ 时，{@link ShieldLayer} 用半透明 render type + 独立贴图
 * {@code textures/entity/ares_shield.png} 只重画 {@code shield} 这一根骨骼，形成半透明护盾壳；
 * 主体仍走 {@code entityCutoutNoCull} + 阶段贴图。</p>
 */
public class AresRenderer extends GeoEntityRenderer<AresEntity> {

    /** 阶段 1（基础形态）：与 {@code ares.png} 完全一致的拷贝。 */
    private static final ResourceLocation TEXTURE_PHASE_1 =
            MarsModule.id("textures/entity/ares_phase1.png");
    /** 阶段 2（炽热形态）：熔渣色系烧红提亮 + 橙色描边。 */
    private static final ResourceLocation TEXTURE_PHASE_2 =
            MarsModule.id("textures/entity/ares_phase2.png");
    /** 阶段 3（过载形态）：核心发白热、外壳压暗、大面裂纹。 */
    private static final ResourceLocation TEXTURE_PHASE_3 =
            MarsModule.id("textures/entity/ares_phase3.png");

    public AresRenderer(EntityRendererProvider.Context context) {
        super(context, new PhaseModel());
        this.shadowRadius = 1.8F;
        // v1.9：护盾外壳渲染层。挂在渲染器上常驻，是否真正画东西由 AresEntity#hasShield() 决定。
        this.addRenderLayer(new ShieldLayer(this));
    }

    /** v1.9 护盾外壳贴图（128×128；只有 shield 立方体那块 UV 有半透明像素）。 */
    private static final ResourceLocation SHIELD_TEXTURE =
            MarsModule.id("textures/entity/ares_shield.png");

    /** 阶段 → 贴图。越界值一律回退到阶段 1，避免出现 missing texture。 */
    static ResourceLocation textureFor(int phase) {
        if (phase >= 3) {
            return TEXTURE_PHASE_3;
        }
        if (phase == 2) {
            return TEXTURE_PHASE_2;
        }
        return TEXTURE_PHASE_1;
    }

    @Override
    public RenderType getRenderType(AresEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityCutoutNoCull(texture);
    }

    /**
     * 在**模型层**分档取贴图。
     *
     * <p>这里没有覆写渲染器的 {@code getTextureLocation}，而是覆写
     * {@code GeoModel#getTextureResource} —— 后者是 {@code GeoModel} 的抽象方法，
     * 签名一定存在；{@code DefaultedGeoModel} 原本就是用它把路径固定成
     * {@code <namespace>:textures/entity/<name>.png} 的，覆写它是这套 API 的既定扩展点。
     * 模型与动画资源仍继承 {@code DefaultedEntityGeoModel} 的默认路径。</p>
     */
    private static final class PhaseModel extends DefaultedEntityGeoModel<AresEntity> {

        private static final ResourceLocation ANIMATIONS =
                MarsModule.id("animations/entity/ares.animation.json");
        private static final java.util.Set<String> LOGGED = java.util.concurrent.ConcurrentHashMap.newKeySet();
        private static boolean cacheMissLogged;

        PhaseModel() {
            super(MarsModule.id("ares"), true);
        }

        @Override
        public ResourceLocation getTextureResource(AresEntity animatable) {
            // phase 来自 SynchedEntityData（DATA_PHASE），客户端能拿到实时值
            return textureFor(animatable.getPhase());
        }

        /**
         * 与 {@code HephaestusModel} 同样的兜底：整合包里 GeckoLib 的动画缓存扫不到本模组文件，
         * {@code super.getAnimation} 会抛 {@code Unable to find animation file} —— 表现就是
         * 「模型在、只有头会跟着玩家转（那是 lookAt，不是动画），四肢一动不动」。
         */
        @Override
        public software.bernie.geckolib.animation.Animation getAnimation(AresEntity animatable, String name) {
            software.bernie.geckolib.animation.Animation animation = null;
            try {
                animation = super.getAnimation(animatable, name);
            } catch (RuntimeException exception) {
                if (!cacheMissLogged) {
                    cacheMissLogged = true;
                    com.suwako.northstarplanetaddon.NorthstarPlanetAddon.LOGGER.warn(
                            "[ares] GeckoLib 动画缓存里没有 {}（{}），改由模型自读", ANIMATIONS, exception.getMessage());
                }
            }
            if (animation == null) {
                var baked = com.suwako.northstarplanetaddon.client.GeckoSelfLoad.load(ANIMATIONS);
                animation = baked == null ? null : baked.getAnimation(name);
            }
            if (LOGGED.add(name)) {
                com.suwako.northstarplanetaddon.NorthstarPlanetAddon.LOGGER.info("[ares] 请求动画 {} → {}", name,
                        animation == null ? "未解析"
                                : "OK " + animation.length() + " tick / " + animation.boneAnimations().length + " 骨骼");
            }
            return animation;
        }
    }

    /**
     * v1.9 护盾外壳渲染层：coreTemperature &gt; 1500℃ 时，在 Boss 躯干外画一层半透明壳。
     *
     * <h2>为什么用 {@code renderForBone}</h2>
     * <p>护盾不是独立实体，而是 {@code ares.geo.json} 里 {@code body} 下的一个新骨骼
     * {@code shield}（一个把躯干包住的大立方体）。GeckoLib 4.9.3 的
     * {@code GeoRenderer#applyRenderLayersForBone}（由 {@code GeoEntityRenderer#renderRecursively}
     * 在 {@code isReRender == false} 时调用）会为**每根骨骼**回调 {@code renderForBone}，
     * 所以这里能只重画 {@code shield} 这一根骨骼：用半透明 render type + 独立贴图，
     * 主体通道（{@code entityCutoutNoCull} + {@code ares_phase*.png}）完全不受影响。</p>
     *
     * <h2>为什么要独立贴图</h2>
     * <p>护盾立方体是模型的一部分，主通道永远会画它一遍；本体贴图在护盾那块 UV 上保持全透明
     * （{@code art/tools/ShieldTexture.java} 与 {@code UvCheck} 都会自检这一点），cutout 会把这些像素
     * 直接丢弃 —— 于是护盾只在 {@link #renderForBone} 里出现，收起时不可见。</p>
     *
     * <p>用的是 {@code RenderType.entityTranslucent}（1.21.1 里为 NO_CULL + TRANSLUCENT，
     * 双面可见），所以从壳外能看到内部的 Boss。</p>
     */
    private static final class ShieldLayer extends GeoRenderLayer<AresEntity> {

        /** 护盾外壳骨骼名（必须与 geo/entity/ares.geo.json 一致）。 */
        private static final String SHIELD_BONE = "shield";

        /** 半透明 render type。{@code Util.memoize} 缓存，同一个贴图只会造一份。 */
        private static final RenderType SHIELD_RENDER_TYPE = RenderType.entityTranslucent(SHIELD_TEXTURE);

        ShieldLayer(GeoRenderer<AresEntity> renderer) {
            super(renderer);
        }

        @Override
        public void renderForBone(PoseStack poseStack, AresEntity animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick,
                                  int packedLight, int packedOverlay) {
            // 护盾没展开 → 一个顶点都不产生；不是护盾骨骼 → 留给本体通道
            if (!animatable.hasShield() || !SHIELD_BONE.equals(bone.getName())) {
                return;
            }
            // 只重画这一根骨骼的立方体，顶点颜色给不透明白（0xFFFFFFFF），alpha 完全来自贴图
            VertexConsumer shieldBuffer = bufferSource.getBuffer(SHIELD_RENDER_TYPE);
            this.renderer.renderCubesOfBone(poseStack, bone, shieldBuffer, packedLight, packedOverlay, 0xFFFFFFFF);
        }
    }
}
