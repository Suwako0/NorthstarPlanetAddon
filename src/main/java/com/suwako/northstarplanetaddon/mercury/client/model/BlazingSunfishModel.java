package com.suwako.northstarplanetaddon.mercury.client.model;

import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import com.suwako.northstarplanetaddon.mercury.entity.BlazingSunfishEntity;
import com.suwako.northstarplanetaddon.mercury.entity.SunfishAttackMode;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 炽热太阳鱼（Blazing sunfish）: the same serpentine sky-fish, mutated by the solar storm.
 * <p>
 * Same skeleton and body sections as {@link SunfishModel}, so the two read as one family, but every
 * detail is replaced: a hooked beak instead of the long snout, a crown of jagged spikes instead of
 * antlers, two long whiskers, a glowing sun-core bossed onto the chest, taller molten sail spikes, huge
 * layered flame-blade pectoral fins and a solid flame tail blade. The motion language is aggressive
 * too — faster, bigger amplitude, a pulsing core and a charge-up flare during the beam attack.
 * <p>
 * Geometry lives in the generated {@link BlazingSunfishMesh}; regenerate it with
 * {@code node tools/gen-entity-model.mjs tools/entity-specs/blazing_sunfish.bbmodel ... BlazingSunfishMesh}.
 */
public class BlazingSunfishModel extends SkyfishModel<BlazingSunfishEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MercuryModule.NAMESPACE, "blazing_sunfish"), "main");

    public static final int TEXTURE_WIDTH = 128;
    public static final int TEXTURE_HEIGHT = 128;

    private final ModelPart core;
    private final ModelPart barbelLeft;
    private final ModelPart barbelRight;
    private final ModelPart crest;

    public BlazingSunfishModel(ModelPart root) {
        super(root);
        this.core = childOrEmpty(this.body, "core");
        this.barbelLeft = childOrEmpty(this.head, "barbel_l");
        this.barbelRight = childOrEmpty(this.head, "barbel_r");
        this.crest = childOrEmpty(this.head, "crest");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        BlazingSunfishMesh.build(mesh);
        return LayerDefinition.create(mesh, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    // ---- 暴躁、短促的节奏：频率与振幅都明显高于太阳鱼 ----

    @Override
    protected float bobSpeed() {
        return 0.09F;
    }

    @Override
    protected float bobAmplitude() {
        return 0.75F;
    }

    @Override
    protected float swaySpeed() {
        return 0.085F;
    }

    @Override
    protected float swayAmplitude() {
        return 0.085F;
    }

    @Override
    protected float waveSpeed() {
        return 0.15F;
    }

    @Override
    protected float waveAmplitude() {
        return 0.26F;
    }

    @Override
    protected float finSpeed() {
        return 0.1F;
    }

    @Override
    protected float finAmplitude() {
        return 0.3F;
    }

    @Override
    protected float finRestAngle() {
        return 0.22F;
    }

    @Override
    protected float diveSpinSpeed() {
        return 1.35F;
    }

    @Override
    protected void animateSpecies(BlazingSunfishEntity entity, float time, SunfishAttackMode mode) {
        // 熔岩心跳：胸核缓慢鼓动，蓄能时明显加速放大（见 applyAttackPose）。
        float beat = time * 0.055F;
        float pulse = 1.0F + Mth.sin(beat) * 0.07F + Mth.sin(beat * 2.3F) * 0.02F;
        this.core.xScale = pulse;
        this.core.yScale = pulse;
        this.core.zScale = pulse;

        // 长须：滞后甩动，像被热流吹动。
        float whip = Mth.sin(time * 0.12F - 0.5F);
        this.barbelLeft.yRot = whip * 0.16F;
        this.barbelRight.yRot = -whip * 0.16F;
        this.barbelLeft.xRot = Mth.sin(time * 0.1F - 0.8F) * 0.12F;
        this.barbelRight.xRot = Mth.sin(time * 0.1F - 0.8F) * 0.12F;

        // 头冠：轻微抖动，攻击蓄力时向后绷紧。
        float shiver = Mth.sin(time * 0.18F) * 0.04F;
        this.crest.xRot = shiver;
        this.hornLeft.zRot = Mth.cos(time * 0.15F) * 0.05F;
        this.hornRight.zRot = -Mth.cos(time * 0.15F) * 0.05F;

        this.jaw.xRot += Mth.sin(time * 0.13F) * 0.02F;
    }

    @Override
    protected void applyAttackPose(BlazingSunfishEntity entity, float time, float pitch, SunfishAttackMode mode) {
        super.applyAttackPose(entity, time, pitch, mode);
        if (mode == SunfishAttackMode.RAY) {
            // 射线蓄能：胸核爆亮长大，双鳍全部前张，头冠后压、长须外翻。
            float charge = 1.25F + Mth.sin(time * 0.5F) * 0.18F;
            this.core.xScale = charge;
            this.core.yScale = charge;
            this.core.zScale = charge;
            this.finRight.zRot = 0.72F;
            this.finLeft.zRot = -0.72F;
            this.finRight.yRot = 0.7F;
            this.finLeft.yRot = -0.7F;
            this.finRightTip.zRot = 0.5F;
            this.finLeftTip.zRot = -0.5F;
            this.barbelLeft.yRot = 0.35F;
            this.barbelRight.yRot = -0.35F;
            this.barbelLeft.xRot = -0.3F;
            this.barbelRight.xRot = -0.3F;
            this.crest.xRot = -0.18F;
            this.hornLeft.xRot = -0.2F;
            this.hornRight.xRot = -0.2F;
        } else if (mode == SunfishAttackMode.DIVE) {
            // 俯冲时胸核收束成一束，鳍完全后掠。
            this.core.xScale = 0.86F;
            this.core.yScale = 0.86F;
            this.core.zScale = 0.86F;
            this.barbelLeft.xRot = 0.4F;
            this.barbelRight.xRot = 0.4F;
            this.crest.xRot = -0.25F;
        }
    }
}
