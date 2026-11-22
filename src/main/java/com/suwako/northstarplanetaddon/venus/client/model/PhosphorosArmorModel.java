package com.suwako.northstarplanetaddon.venus.client.model;

import com.suwako.northstarplanetaddon.client.render.ArmorDecoModel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;

/**
 * Phosphoros dawn aegis: a radiant, ornate Venusian champion set.
 * The vanilla armor layer supplies the fitted body shell; this model supplies
 * the sculptural gold decorations and celestial silhouette.
 */
public class PhosphorosArmorModel extends HumanoidModel<LivingEntity> implements ArmorDecoModel {

    private final ModelPart halo;
    private final ModelPart haloRing;
    private final ModelPart crownSpikeLeft;
    private final ModelPart crownSpikeRight;
    private final ModelPart pauldronLeft;
    private final ModelPart pauldronRight;
    private final ModelPart mantleLeft;
    private final ModelPart mantleRight;
    private final ModelPart chestGem;
    private final ModelPart bracerLeft;
    private final ModelPart bracerRight;
    private final ModelPart tassetRight;
    private final ModelPart tassetLeft;
    private final ModelPart greaveRight;
    private final ModelPart greaveLeft;
    private final ModelPart kneeGemRight;
    private final ModelPart kneeGemLeft;
    private final ModelPart stud_1;
    private final ModelPart stud_2;
    private final ModelPart stud_3;

    private boolean showHead = true;
    private boolean showBody = true;
    private boolean showArms = true;
    private boolean showLegs = true;

    public void setSlot(ArmorItem.Type type) {
        this.showHead = type == ArmorItem.Type.HELMET;
        this.showBody = type == ArmorItem.Type.CHESTPLATE || type == ArmorItem.Type.LEGGINGS || type == ArmorItem.Type.BODY;
        this.showArms = type == ArmorItem.Type.CHESTPLATE;
        this.showLegs = type == ArmorItem.Type.LEGGINGS || type == ArmorItem.Type.BOOTS;
    }

    public PhosphorosArmorModel(ModelPart root) {
        super(root);
        ModelPart headPart = root.getChild("head");
        ModelPart bodyPart = root.getChild("body");
        ModelPart rightArmPart = root.getChild("right_arm");
        ModelPart leftArmPart = root.getChild("left_arm");
        ModelPart rightLegPart = root.getChild("right_leg");
        ModelPart leftLegPart = root.getChild("left_leg");
        this.halo = headPart.getChild("halo");
        this.haloRing = headPart.getChild("halo_ring");
        this.crownSpikeLeft = headPart.getChild("crown_spike_left");
        this.crownSpikeRight = headPart.getChild("crown_spike_right");
        this.pauldronLeft = bodyPart.getChild("pauldron_left");
        this.pauldronRight = bodyPart.getChild("pauldron_right");
        this.mantleLeft = bodyPart.getChild("mantle_left");
        this.mantleRight = bodyPart.getChild("mantle_right");
        this.chestGem = bodyPart.getChild("chest_gem");
        this.bracerLeft = leftArmPart.getChild("bracer_left");
        this.bracerRight = rightArmPart.getChild("bracer_right");
        this.tassetRight = rightLegPart.getChild("tasset_right");
        this.tassetLeft = leftLegPart.getChild("tasset_left");
        this.greaveRight = rightLegPart.getChild("greave_right");
        this.greaveLeft = leftLegPart.getChild("greave_left");
        this.kneeGemRight = rightLegPart.getChild("knee_gem_right");
        this.kneeGemLeft = leftLegPart.getChild("knee_gem_left");
        this.stud_1 = bodyPart.getChild("stud_1");
        this.stud_2 = bodyPart.getChild("stud_2");
        this.stud_3 = bodyPart.getChild("stud_3");
    }

    public static LayerDefinition createArmorLayer(CubeDeformation deformation) {
        MeshDefinition mesh = HumanoidModel.createMesh(deformation, 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("halo", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -14, -4, 8, 0.57F, 8), PartPose.offset(0, -8, 0));
        head.addOrReplaceChild("halo_ring", CubeListBuilder.create().texOffs(34, 0).addBox(-5, -14.4F, -5, 10, 0.475F, 10), PartPose.offset(0, -8, 0));
        head.addOrReplaceChild("crown_spike_left", CubeListBuilder.create().texOffs(0, 22).addBox(-5.2F, -13.5F, -1.1F, 2.2F, 4.0F, 2.2F), PartPose.offsetAndRotation(0, -8, 0, 0, 0, -0.22F));
        head.addOrReplaceChild("crown_spike_right", CubeListBuilder.create().texOffs(9, 22).addBox(3.0F, -13.5F, -1.1F, 2.2F, 4.0F, 2.2F), PartPose.offsetAndRotation(0, -8, 0, 0, 0, 0.22F));

        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("pauldron_left", CubeListBuilder.create().texOffs(76, 0).addBox(-11.2F, -2, -3, 7.2F, 4, 6), PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("pauldron_right", CubeListBuilder.create().texOffs(0, 13).addBox(4.0F, -2, -3, 7.2F, 4, 6), PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("mantle_left", CubeListBuilder.create().texOffs(18, 22).addBox(-9.6F, -0.4F, 2.2F, 5.8F, 1.6F, 2.0F), PartPose.offsetAndRotation(0, 0, 0, 0.2F, 0, -0.12F));
        body.addOrReplaceChild("mantle_right", CubeListBuilder.create().texOffs(34, 22).addBox(3.8F, -0.4F, 2.2F, 5.8F, 1.6F, 2.0F), PartPose.offsetAndRotation(0, 0, 0, 0.2F, 0, 0.12F));
        body.addOrReplaceChild("chest_gem", CubeListBuilder.create().texOffs(48, 22).addBox(-1.7F, 0.2F, -3.15F, 3.4F, 3.8F, 0.65F), PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("stud_1", CubeListBuilder.create().texOffs(115, 13).addBox(0.5F, 2.8F, -2.85F, 0.6F, 0.6F, 0.4F), PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("stud_2", CubeListBuilder.create().texOffs(119, 13).addBox(-0.5F, 4.55F, -2.85F, 0.6F, 0.6F, 0.4F), PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("stud_3", CubeListBuilder.create().texOffs(123, 13).addBox(-0.17F, 2.65F, -2.85F, 0.6F, 0.6F, 0.4F), PartPose.offset(0, 0, 0));

        PartDefinition rightArm = root.getChild("right_arm");
        rightArm.addOrReplaceChild("bracer_right", CubeListBuilder.create().texOffs(64, 22).addBox(-3.45F, 1.4F, -2.55F, 3.0F, 5.0F, 5.1F), PartPose.offset(0, 0, 0));
        PartDefinition leftArm = root.getChild("left_arm");
        leftArm.addOrReplaceChild("bracer_left", CubeListBuilder.create().texOffs(80, 22).addBox(0.45F, 1.4F, -2.55F, 3.0F, 5.0F, 5.1F), PartPose.offset(0, 0, 0));

        PartDefinition rightLeg = root.getChild("right_leg");
        rightLeg.addOrReplaceChild("tasset_right", CubeListBuilder.create().texOffs(29, 13).addBox(-2.3F, 0, -2.3F, 4.6F, 2.28F, 4.6F), PartPose.offset(0, 0, 0));
        rightLeg.addOrReplaceChild("greave_right", CubeListBuilder.create().texOffs(71, 13).addBox(-2.4F, 5, -2.4F, 4.8F, 5.7F, 4.8F), PartPose.offset(0, 0, 0));
        rightLeg.addOrReplaceChild("knee_gem_right", CubeListBuilder.create().texOffs(96, 22).addBox(-1.25F, 3.0F, -2.75F, 2.5F, 1.5F, 0.7F), PartPose.offset(0, 0, 0));
        PartDefinition leftLeg = root.getChild("left_leg");
        leftLeg.addOrReplaceChild("tasset_left", CubeListBuilder.create().texOffs(50, 13).addBox(-2.3F, 0, -2.3F, 4.6F, 2.28F, 4.6F), PartPose.offset(0, 0, 0));
        leftLeg.addOrReplaceChild("greave_left", CubeListBuilder.create().texOffs(93, 13).addBox(-2.4F, 5, -2.4F, 4.8F, 5.7F, 4.8F), PartPose.offset(0, 0, 0));
        leftLeg.addOrReplaceChild("knee_gem_left", CubeListBuilder.create().texOffs(106, 22).addBox(-1.25F, 3.0F, -2.75F, 2.5F, 1.5F, 0.7F), PartPose.offset(0, 0, 0));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        if (showHead) {
            poseStack.pushPose(); head.translateAndRotate(poseStack);
            halo.render(poseStack, buffer, packedLight, packedOverlay, color);
            haloRing.render(poseStack, buffer, packedLight, packedOverlay, color);
            crownSpikeLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
            crownSpikeRight.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
        if (showBody) {
            poseStack.pushPose(); body.translateAndRotate(poseStack);
            pauldronLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
            pauldronRight.render(poseStack, buffer, packedLight, packedOverlay, color);
            mantleLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
            mantleRight.render(poseStack, buffer, packedLight, packedOverlay, color);
            chestGem.render(poseStack, buffer, packedLight, packedOverlay, color);
            stud_1.render(poseStack, buffer, packedLight, packedOverlay, color);
            stud_2.render(poseStack, buffer, packedLight, packedOverlay, color);
            stud_3.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
        if (showArms) {
            poseStack.pushPose(); rightArm.translateAndRotate(poseStack); bracerRight.render(poseStack, buffer, packedLight, packedOverlay, color); poseStack.popPose();
            poseStack.pushPose(); leftArm.translateAndRotate(poseStack); bracerLeft.render(poseStack, buffer, packedLight, packedOverlay, color); poseStack.popPose();
        }
        if (showLegs) {
            poseStack.pushPose(); rightLeg.translateAndRotate(poseStack);
            tassetRight.render(poseStack, buffer, packedLight, packedOverlay, color); greaveRight.render(poseStack, buffer, packedLight, packedOverlay, color); kneeGemRight.render(poseStack, buffer, packedLight, packedOverlay, color); poseStack.popPose();
            poseStack.pushPose(); leftLeg.translateAndRotate(poseStack);
            tassetLeft.render(poseStack, buffer, packedLight, packedOverlay, color); greaveLeft.render(poseStack, buffer, packedLight, packedOverlay, color); kneeGemLeft.render(poseStack, buffer, packedLight, packedOverlay, color); poseStack.popPose();
        }
    }
}
