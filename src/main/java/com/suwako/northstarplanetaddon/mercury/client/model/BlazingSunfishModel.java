package com.suwako.northstarplanetaddon.mercury.client.model;

import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import com.suwako.northstarplanetaddon.mercury.entity.BlazingSunfishEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 炽烈太阳鱼 skeleton: the same rounded body, wings and forked tail as 太阳鱼, plus four crest spikes
 * along the skull, a molten vent on each flank and two flame streamers trailing the tail — the pieces
 * that read as "mutated" from a distance, and that the emissive layer lights up.
 * <p>
 * The base skeleton is built by the same helpers {@link SunfishModel} uses with the extras switched on,
 * so both models share one UV layout and one set of part names. It reuses the base slots wherever the
 * geometry matches and adds the blazing-only slots {@code (18,32) crest spike}, {@code (22,32) flank
 * vent} and {@code (30,32) tail flame}.
 */
public class BlazingSunfishModel extends SunfishModel<BlazingSunfishEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MercuryModule.NAMESPACE, "blazing_sunfish"), "main");

    private final ModelPart crest1;
    private final ModelPart crest2;
    private final ModelPart crest3;
    private final ModelPart crest4;
    private final ModelPart ventLeft;
    private final ModelPart ventRight;
    private final ModelPart flameUpper;
    private final ModelPart flameLower;

    public BlazingSunfishModel(ModelPart root) {
        super(root);
        this.crest1 = childOrEmpty(this.head, "crest_1");
        this.crest2 = childOrEmpty(this.head, "crest_2");
        this.crest3 = childOrEmpty(this.head, "crest_3");
        this.crest4 = childOrEmpty(this.head, "crest_4");
        this.ventLeft = childOrEmpty(this.body, "vent_left");
        this.ventRight = childOrEmpty(this.body, "vent_right");
        this.flameUpper = childOrEmpty(this.tailBase, "flame_upper");
        this.flameLower = childOrEmpty(this.tailBase, "flame_lower");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 基础骨架与太阳鱼一致，变异部件一并建出来。
        PartDefinition body = root.addOrReplaceChild("body", bodyCubes(),
                PartPose.offset(0.0F, BODY_PIVOT_Y, 0.0F));
        // 两侧熔岩喷口：贴在体侧的薄板，可以独立膨胀脉动。
        body.addOrReplaceChild("vent_left", CubeListBuilder.create()
                .texOffs(22, 32).addBox(0.0F, 0.0F, 0.0F, 1.0F, 5.0F, 3.0F)
                , PartPose.offset(-4.4F, -2.5F, 0.0F));
        body.addOrReplaceChild("vent_right", CubeListBuilder.create()
                .texOffs(22, 32).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 5.0F, 3.0F)
                , PartPose.offset(3.4F, -2.5F, 0.0F));

        addHead(body, true);
        addFins(body);
        addTail(body, true);

        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * The crest, the vents and the tail streamers breathe on their own slow clock, and every so often
     * the whole fish surges — a visible flare that sets it apart from the calm gold breed.
     */
    @Override
    protected void animateExtras(float ageInTicks) {
        // 骨冠依次摆动
        float crest = Mth.sin(ageInTicks * 0.12F) * 0.2F;
        this.crest1.xRot = crest;
        this.crest2.xRot = crest * 0.75F;
        this.crest3.xRot = crest * 0.75F;
        this.crest4.xRot = crest;
        this.crest1.zRot = Mth.cos(ageInTicks * 0.09F) * 0.08F;
        this.crest4.zRot = -Mth.cos(ageInTicks * 0.09F) * 0.08F;

        // 熔岩涌动：约 7 秒一个周期的缓慢"心跳"，喷口随之鼓起。
        float surge = 0.5F + 0.5F * Mth.sin(ageInTicks * 0.06F);
        float swell = 1.0F + surge * 0.45F;
        this.ventLeft.xScale = swell;
        this.ventRight.xScale = swell;
        this.ventLeft.zScale = 1.0F + surge * 0.2F;
        this.ventRight.zScale = 1.0F + surge * 0.2F;

        // 尾焰：左右甩动 + 随涌动拉长。
        float flicker = Mth.sin(ageInTicks * 0.28F) * 0.32F;
        this.flameUpper.yRot = flicker;
        this.flameLower.yRot = -flicker;
        this.flameUpper.xRot = -0.15F + flicker * 0.3F;
        this.flameLower.xRot = 0.15F - flicker * 0.3F;
        float stretch = 1.0F + surge * 0.35F;
        this.flameUpper.yScale = stretch;
        this.flameLower.yScale = stretch;
    }
}
