package com.suwako.northstarplanetaddon.venus.client.model;

import com.suwako.northstarplanetaddon.venus.entity.boss.AphroditeEntity;
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
 * 阿佛洛狄忒模型：躯干 + 头 + 双臂 + 双腿 + 胸口硫晶核 + 背部双酸囊。
 *
 * <p>贴图 128×128，UV 布局：</p>
 * <table>
 *   <tr><th>区域</th><th>部位</th></tr>
 *   <tr><td>(0,0)–(48,28)</td><td>躯干 16×20×8</td></tr>
 *   <tr><td>(48,0)–(80,16)</td><td>头部 8×8×8</td></tr>
 *   <tr><td>(0,28)–(24,52)</td><td>手臂 6×18×6（左右共用）</td></tr>
 *   <tr><td>(24,28)–(48,50)</td><td>腿 6×16×6（左右共用）</td></tr>
 *   <tr><td>(48,16)–(72,28)</td><td>胸口硫晶核 6×6×6</td></tr>
 *   <tr><td>(72,16)–(92,26)</td><td>背部酸囊 5×5×5（左右共用）</td></tr>
 * </table>
 */
public class AphroditeModel extends EntityModel<AphroditeEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("northstar_planet_addon", "aphrodite"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart armLeft;
    private final ModelPart armRight;
    private final ModelPart legLeft;
    private final ModelPart legRight;
    private final ModelPart sulfurCore;
    private final ModelPart sacLeft;
    private final ModelPart sacRight;

    public AphroditeModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.armLeft = this.body.getChild("arm_left");
        this.armRight = this.body.getChild("arm_right");
        this.legLeft = this.body.getChild("leg_left");
        this.legRight = this.body.getChild("leg_right");
        this.sulfurCore = this.body.getChild("sulfur_core");
        this.sacLeft = this.body.getChild("sac_left");
        this.sacRight = this.body.getChild("sac_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-8.0F, -10.0F, -4.0F, 16.0F, 20.0F, 8.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -10.0F, 0.0F));

        body.addOrReplaceChild("arm_left",
                CubeListBuilder.create().texOffs(0, 28)
                        .addBox(-3.0F, 0.0F, -3.0F, 6.0F, 18.0F, 6.0F),
                PartPose.offset(9.0F, -8.0F, 0.0F));

        body.addOrReplaceChild("arm_right",
                CubeListBuilder.create().texOffs(0, 28).mirror()
                        .addBox(-3.0F, 0.0F, -3.0F, 6.0F, 18.0F, 6.0F),
                PartPose.offset(-9.0F, -8.0F, 0.0F));

        body.addOrReplaceChild("leg_left",
                CubeListBuilder.create().texOffs(24, 28)
                        .addBox(-3.0F, 0.0F, -3.0F, 6.0F, 16.0F, 6.0F),
                PartPose.offset(4.0F, 10.0F, 0.0F));

        body.addOrReplaceChild("leg_right",
                CubeListBuilder.create().texOffs(24, 28).mirror()
                        .addBox(-3.0F, 0.0F, -3.0F, 6.0F, 16.0F, 6.0F),
                PartPose.offset(-4.0F, 10.0F, 0.0F));

        body.addOrReplaceChild("sulfur_core",
                CubeListBuilder.create().texOffs(48, 16)
                        .addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F),
                PartPose.offset(0.0F, -2.0F, -4.0F));

        body.addOrReplaceChild("sac_left",
                CubeListBuilder.create().texOffs(72, 16)
                        .addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(4.0F, 0.0F, 5.0F));

        body.addOrReplaceChild("sac_right",
                CubeListBuilder.create().texOffs(72, 16).mirror()
                        .addBox(-2.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(-4.0F, 0.0F, 5.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(AphroditeEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float swing = Mth.cos(limbSwing * 0.5F) * 0.8F * limbSwingAmount;
        this.legLeft.xRot = swing;
        this.legRight.xRot = -swing;
        this.armLeft.xRot = -swing * 0.5F;
        this.armRight.xRot = swing * 0.5F;

        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6F;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD * 0.6F;

        // 硫晶核常亮脉动；结茧时酸囊鼓起
        float corePulse = 1.0F + Mth.sin(ageInTicks * 0.25F) * 0.08F;
        this.sulfurCore.xScale = corePulse;
        this.sulfurCore.yScale = corePulse;
        this.sulfurCore.zScale = corePulse;

        float sacPulse = entity.isOverloaded() ? 1.25F : 1.0F;
        this.sacLeft.xScale = sacPulse;
        this.sacLeft.yScale = sacPulse;
        this.sacLeft.zScale = sacPulse;
        this.sacRight.xScale = sacPulse;
        this.sacRight.yScale = sacPulse;
        this.sacRight.zScale = sacPulse;

        this.body.y = 8.0F + Mth.sin(ageInTicks * 0.1F) * 0.3F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
