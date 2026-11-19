package com.suwako.northstarplanetaddon.venus.client.model;

import com.suwako.northstarplanetaddon.venus.entity.boss.NepheleEntity;
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
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * 涅斐勒模型：鲸形躯干 + 双鳍 + 尾部 + 背部两个气囊（弱点可视化）。
 *
 * <p>贴图 128×128，由 {@code assets/phosphoros/textures/entity/nephele.png} 提供。</p>
 */
public class NepheleModel extends EntityModel<NepheleEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("northstar_planet_addon", "nephele"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart sacLeft;
    private final ModelPart sacRight;

    public NepheleModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.tail = this.body.getChild("tail");
        this.leftFin = this.body.getChild("left_fin");
        this.rightFin = this.body.getChild("right_fin");
        this.sacLeft = this.body.getChild("sac_left");
        this.sacRight = this.body.getChild("sac_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-12.0F, -10.0F, -32.0F, 24.0F, 20.0F, 64.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));

        body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 84)
                        .addBox(-8.0F, -7.0F, -14.0F, 16.0F, 14.0F, 14.0F),
                PartPose.offset(0.0F, 0.0F, -32.0F));

        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(64, 84)
                        .addBox(-6.0F, -6.0F, 0.0F, 12.0F, 12.0F, 18.0F),
                PartPose.offset(0.0F, 0.0F, 32.0F));

        body.addOrReplaceChild("left_fin",
                CubeListBuilder.create().texOffs(176, 0)
                        .addBox(0.0F, 0.0F, -8.0F, 18.0F, 2.0F, 16.0F),
                PartPose.offset(12.0F, 2.0F, 0.0F));

        body.addOrReplaceChild("right_fin",
                CubeListBuilder.create().texOffs(176, 0).mirror()
                        .addBox(-18.0F, 0.0F, -8.0F, 18.0F, 2.0F, 16.0F),
                PartPose.offset(-12.0F, 2.0F, 0.0F));

        body.addOrReplaceChild("sac_left",
                CubeListBuilder.create().texOffs(128, 84)
                        .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(8.0F, 10.0F, -6.0F));

        body.addOrReplaceChild("sac_right",
                CubeListBuilder.create().texOffs(128, 84).mirror()
                        .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(-8.0F, 10.0F, -6.0F));

        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(NepheleEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float yawRad = netHeadYaw * Mth.DEG_TO_RAD;
        float pitchRad = headPitch * Mth.DEG_TO_RAD;

        this.head.yRot = yawRad * 0.6F;
        this.head.xRot = pitchRad * 0.6F;

        this.tail.yRot = Mth.sin(ageInTicks * 0.15F) * 0.22F;
        this.tail.xRot = Mth.sin(ageInTicks * 0.11F) * 0.12F;

        float flap = Mth.cos(ageInTicks * 0.2F) * 0.35F;
        this.leftFin.zRot = flap;
        this.rightFin.zRot = -flap;

        // 悬停起伏
        this.body.y = 18.0F + Mth.sin(ageInTicks * 0.1F) * 0.8F;

        // 过载时气囊鼓动（弱点提示）
        float sacScale = 1.0F;
        this.sacLeft.xScale = sacScale;
        this.sacLeft.yScale = sacScale;
        this.sacLeft.zScale = sacScale;
        this.sacRight.xScale = sacScale;
        this.sacRight.yScale = sacScale;
        this.sacRight.zScale = sacScale;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
