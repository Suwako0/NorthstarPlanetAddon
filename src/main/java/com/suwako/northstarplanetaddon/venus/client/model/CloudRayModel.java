package com.suwako.northstarplanetaddon.venus.client.model;

import com.suwako.northstarplanetaddon.venus.entity.CloudRayEntity;
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
 * 云帆鳐模型：扁平菱形躯干 + 左右两片大翼 + 细长尾。
 * 贴图 64×64：{@code assets/phosphoros/textures/entity/cloud_ray.png}。
 */
public class CloudRayModel extends EntityModel<CloudRayEntity> {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("northstar_planet_addon", "cloud_ray"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;

    public CloudRayModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.tail = this.body.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -2.0F, -7.0F, 8.0F, 3.0F, 14.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));

        body.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(0, 20)
                        .addBox(0.0F, -0.5F, -4.0F, 14.0F, 1.0F, 8.0F),
                PartPose.offset(4.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(0, 20).mirror()
                        .addBox(-14.0F, -0.5F, -4.0F, 14.0F, 1.0F, 8.0F),
                PartPose.offset(-4.0F, 0.0F, 0.0F));

        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-1.0F, -0.5F, 0.0F, 2.0F, 1.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 7.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(CloudRayEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float flap = Mth.sin(ageInTicks * 0.45F) * 0.45F;
        this.leftWing.zRot = flap;
        this.rightWing.zRot = -flap;
        this.tail.yRot = Mth.sin(ageInTicks * 0.3F) * 0.25F;
        this.body.xRot = headPitch * Mth.DEG_TO_RAD * 0.4F;
        this.body.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.4F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
