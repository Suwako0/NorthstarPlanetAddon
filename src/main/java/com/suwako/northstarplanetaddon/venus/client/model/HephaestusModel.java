package com.suwako.northstarplanetaddon.venus.client.model;

import com.suwako.northstarplanetaddon.venus.entity.boss.HephaestusEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
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
 * 赫菲斯托斯巨像模型：躯干 + 头 + 双臂 + 双腿 + 背部炉心 + 双肩散热口。
 *
 * <p>贴图 128×128，UV 布局（与 {@code docs/TEXTURE-SPEC.md} 一致）：</p>
 * <table>
 *   <tr><th>区域</th><th>部位</th></tr>
 *   <tr><td>(0,0)–(64,36)</td><td>躯干 20×24×12</td></tr>
 *   <tr><td>(64,0)–(104,20)</td><td>头部 10×10×10</td></tr>
 *   <tr><td>(0,36)–(32,68)</td><td>手臂 8×24×8（左右共用）</td></tr>
 *   <tr><td>(32,36)–(72,66)</td><td>腿 10×20×10（左右共用）</td></tr>
 *   <tr><td>(96,20)–(128,36)</td><td>背部炉心 8×8×8</td></tr>
 *   <tr><td>(72,36)–(96,46)</td><td>肩部散热口 6×4×6（左右共用）</td></tr>
 * </table>
 */
public class HephaestusModel extends EntityModel<HephaestusEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("northstar_planet_addon", "hephaestus"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart armLeft;
    private final ModelPart armRight;
    private final ModelPart legLeft;
    private final ModelPart legRight;
    private final ModelPart core;

    public HephaestusModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.armLeft = this.body.getChild("arm_left");
        this.armRight = this.body.getChild("arm_right");
        this.legLeft = this.body.getChild("leg_left");
        this.legRight = this.body.getChild("leg_right");
        this.core = this.body.getChild("core");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-10.0F, -12.0F, -6.0F, 20.0F, 24.0F, 12.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));

        body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(64, 0)
                        .addBox(-5.0F, -10.0F, -5.0F, 10.0F, 10.0F, 10.0F),
                PartPose.offset(0.0F, -12.0F, 0.0F));

        body.addOrReplaceChild("arm_left",
                CubeListBuilder.create().texOffs(0, 36)
                        .addBox(-4.0F, 0.0F, -4.0F, 8.0F, 24.0F, 8.0F),
                PartPose.offset(11.0F, -10.0F, 0.0F));

        body.addOrReplaceChild("arm_right",
                CubeListBuilder.create().texOffs(0, 36).mirror()
                        .addBox(-4.0F, 0.0F, -4.0F, 8.0F, 24.0F, 8.0F),
                PartPose.offset(-11.0F, -10.0F, 0.0F));

        body.addOrReplaceChild("leg_left",
                CubeListBuilder.create().texOffs(32, 36)
                        .addBox(-5.0F, 0.0F, -5.0F, 10.0F, 20.0F, 10.0F),
                PartPose.offset(5.0F, 12.0F, 0.0F));

        body.addOrReplaceChild("leg_right",
                CubeListBuilder.create().texOffs(32, 36).mirror()
                        .addBox(-5.0F, 0.0F, -5.0F, 10.0F, 20.0F, 10.0F),
                PartPose.offset(-5.0F, 12.0F, 0.0F));

        body.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(96, 20)
                        .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 2.0F, 6.0F));

        body.addOrReplaceChild("vent_left",
                CubeListBuilder.create().texOffs(72, 36)
                        .addBox(-3.0F, -2.0F, -3.0F, 6.0F, 4.0F, 6.0F),
                PartPose.offset(11.0F, -12.0F, 0.0F));

        body.addOrReplaceChild("vent_right",
                CubeListBuilder.create().texOffs(72, 36).mirror()
                        .addBox(-3.0F, -2.0F, -3.0F, 6.0F, 4.0F, 6.0F),
                PartPose.offset(-11.0F, -12.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(HephaestusEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float swing = Mth.cos(limbSwing * 0.4F) * 0.9F * limbSwingAmount;
        this.legLeft.xRot = swing;
        this.legRight.xRot = -swing;
        this.armLeft.xRot = -swing * 0.6F;
        this.armRight.xRot = swing * 0.6F;

        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5F;

        // 缓慢的呼吸/震动
        this.body.y = 12.0F + Mth.sin(ageInTicks * 0.08F) * 0.4F;

        // 炉心在过载时脉动（弱点提示）
        float pulse = entity.isOverloaded() ? 1.0F + Mth.sin(ageInTicks * 0.6F) * 0.12F : 1.0F;
        this.core.xScale = pulse;
        this.core.yScale = pulse;
        this.core.zScale = pulse;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
