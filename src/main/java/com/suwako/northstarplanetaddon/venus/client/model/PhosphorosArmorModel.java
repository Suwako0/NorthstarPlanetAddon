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
 * PhosphorosArmorModel — 自动生成的装饰版盔甲模型（17 个额外部件，其中 8 个装饰件）。
 * 贴图：assets/northstar_planet_addon/textures/models/armor/phosphoros_armor.png（128x128）。
 *
 * 只渲染装饰件：基础的头/身/臂/腿不在这里画，交给 phosphoros_layer_1/2.png，
 * 所以脸和手不会被盔甲模型整块盖住（这是"戴上头盔看不见脸"的根因）。
 * 渲染哪个槽位由 setSlot(ArmorItem.Type) 决定，避免头盔渲染时把披风、护腿也画出来。
 */
public class PhosphorosArmorModel extends HumanoidModel<LivingEntity> implements ArmorDecoModel {

    private final ModelPart crownLeft;
    private final ModelPart crownMid;
    private final ModelPart crownRight;
    private final ModelPart hornLeft;
    private final ModelPart hornRight;
    private final ModelPart chestPlate;
    private final ModelPart coreGem;
    private final ModelPart pauldronLeft;
    private final ModelPart pauldronRight;
    private final ModelPart cape;
    private final ModelPart belt;
    private final ModelPart bracerRight;
    private final ModelPart bracerLeft;
    private final ModelPart tassetRight;
    private final ModelPart tassetLeft;
    private final ModelPart greaveRight;
    private final ModelPart greaveLeft;

    private boolean showHead = true;
    private boolean showBody = true;
    private boolean showArms = true;
    private boolean showLegs = true;

    /** 渲染前按槽位设置：HELMET 只画头部件，CHESTPLATE 画躯干+护臂，LEGGINGS/BOOTS 画腿部件。 */
    public void setSlot(ArmorItem.Type type) {
        this.showHead = type == ArmorItem.Type.HELMET;
        this.showBody = type == ArmorItem.Type.CHESTPLATE || type == ArmorItem.Type.LEGGINGS || type == ArmorItem.Type.BODY;
        this.showArms = type == ArmorItem.Type.CHESTPLATE;
        this.showLegs = type == ArmorItem.Type.LEGGINGS || type == ArmorItem.Type.BOOTS;
    }

    public PhosphorosArmorModel(ModelPart root) {
        super(root);
        this.crownLeft = root.getChild("head").getChild("crown_left");
        this.crownMid = root.getChild("head").getChild("crown_mid");
        this.crownRight = root.getChild("head").getChild("crown_right");
        this.hornLeft = root.getChild("head").getChild("horn_left");
        this.hornRight = root.getChild("head").getChild("horn_right");
        this.chestPlate = root.getChild("body").getChild("chest_plate");
        this.coreGem = root.getChild("body").getChild("core_gem");
        this.pauldronLeft = root.getChild("body").getChild("pauldron_left");
        this.pauldronRight = root.getChild("body").getChild("pauldron_right");
        this.cape = root.getChild("body").getChild("cape");
        this.belt = root.getChild("body").getChild("belt");
        this.bracerRight = root.getChild("right_arm").getChild("bracer_right");
        this.bracerLeft = root.getChild("left_arm").getChild("bracer_left");
        this.tassetRight = root.getChild("right_leg").getChild("tasset_right");
        this.tassetLeft = root.getChild("left_leg").getChild("tasset_left");
        this.greaveRight = root.getChild("right_leg").getChild("greave_right");
        this.greaveLeft = root.getChild("left_leg").getChild("greave_left");
    }

    public static LayerDefinition createArmorLayer(CubeDeformation deformation) {
        MeshDefinition mesh = HumanoidModel.createMesh(deformation, 0.0F);
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("crown_left",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -1, 1, 4, 2),
                PartPose.offset(0, -8, 0));
        head.addOrReplaceChild("crown_mid",
                CubeListBuilder.create().texOffs(8, 0).addBox(-1, -6, -1, 2, 6, 2),
                PartPose.offset(0, -8, 0));
        head.addOrReplaceChild("crown_right",
                CubeListBuilder.create().texOffs(18, 0).addBox(3, -4, -1, 1, 4, 2),
                PartPose.offset(0, -8, 0));
        head.addOrReplaceChild("horn_left",
                CubeListBuilder.create().texOffs(26, 0).addBox(-6, -3, -1, 2, 3, 2),
                PartPose.offset(0, -8, 0));
        head.addOrReplaceChild("horn_right",
                CubeListBuilder.create().texOffs(36, 0).addBox(4, -3, -1, 2, 3, 2),
                PartPose.offset(0, -8, 0));
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("chest_plate",
                CubeListBuilder.create().texOffs(46, 0).addBox(-4.5F, 0, -2.5F, 9, 6, 5),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("core_gem",
                CubeListBuilder.create().texOffs(76, 0).addBox(-1, 2, -2.8F, 2, 2, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("pauldron_left",
                CubeListBuilder.create().texOffs(84, 0).addBox(-6, -1, -2.5F, 3, 3, 5),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("pauldron_right",
                CubeListBuilder.create().texOffs(102, 0).addBox(3, -1, -2.5F, 3, 3, 5),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("cape",
                CubeListBuilder.create().texOffs(0, 13).addBox(-4, 1, 2.2F, 8, 10, 0.6F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("belt",
                CubeListBuilder.create().texOffs(20, 13).addBox(-4.2F, 5, -2.2F, 8.4F, 1.5F, 4.4F),
                PartPose.offset(0, 0, 0));
        PartDefinition rightarm = root.getChild("right_arm");
        rightarm.addOrReplaceChild("bracer_right",
                CubeListBuilder.create().texOffs(48, 13).addBox(-2.2F, 4, -2.2F, 4.4F, 3, 4.4F),
                PartPose.offset(0, 0, 0));
        PartDefinition leftarm = root.getChild("left_arm");
        leftarm.addOrReplaceChild("bracer_left",
                CubeListBuilder.create().texOffs(68, 13).addBox(-2.2F, 4, -2.2F, 4.4F, 3, 4.4F),
                PartPose.offset(0, 0, 0));
        PartDefinition rightleg = root.getChild("right_leg");
        rightleg.addOrReplaceChild("tasset_right",
                CubeListBuilder.create().texOffs(88, 13).addBox(-2.2F, 0, -2.2F, 4.4F, 2, 4.4F),
                PartPose.offset(0, 0, 0));
        rightleg.addOrReplaceChild("greave_right",
                CubeListBuilder.create().texOffs(0, 26).addBox(-2.2F, 6, -2.2F, 4.4F, 5, 4.4F),
                PartPose.offset(0, 0, 0));
        PartDefinition leftleg = root.getChild("left_leg");
        leftleg.addOrReplaceChild("tasset_left",
                CubeListBuilder.create().texOffs(108, 13).addBox(-2.2F, 0, -2.2F, 4.4F, 2, 4.4F),
                PartPose.offset(0, 0, 0));
        leftleg.addOrReplaceChild("greave_left",
                CubeListBuilder.create().texOffs(20, 26).addBox(-2.2F, 6, -2.2F, 4.4F, 5, 4.4F),
                PartPose.offset(0, 0, 0));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        // 仅装饰件；基础身体由原版 layer 贴图负责，脸/手保持可见
        if (this.showHead) {
            poseStack.pushPose();
            this.head.translateAndRotate(poseStack);
            this.crownLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.crownMid.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.crownRight.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.hornLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.hornRight.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
        if (this.showBody) {
            poseStack.pushPose();
            this.body.translateAndRotate(poseStack);
            this.chestPlate.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.coreGem.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.pauldronLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.pauldronRight.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.cape.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.belt.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
        if (this.showArms) {
            poseStack.pushPose();
            this.rightArm.translateAndRotate(poseStack);
            this.bracerRight.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
        if (this.showArms) {
            poseStack.pushPose();
            this.leftArm.translateAndRotate(poseStack);
            this.bracerLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
        if (this.showLegs) {
            poseStack.pushPose();
            this.rightLeg.translateAndRotate(poseStack);
            this.tassetRight.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.greaveRight.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
        if (this.showLegs) {
            poseStack.pushPose();
            this.leftLeg.translateAndRotate(poseStack);
            this.tassetLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.greaveLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
    }
}
