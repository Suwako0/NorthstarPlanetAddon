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
 * TungstenArmorModel — 按「tungsten」生成的装饰盔甲模型（13 部件 / 9 装饰）。
 * 主题：iron；种子：tungsten|redesign2026|pressure_sentinel|broad。
 * 贴图：assets/northstar_planet_addon/textures/models/armor/tungsten_armor.png（128x128）。
 *
 * 只渲染装饰件，基础身体交给 tungsten_layer_1/2.png；
 * 生成的 layer 贴图脸部窗口与手部是镂空的，所以脸和手一定可见。
 */
public class TungstenArmorModel extends HumanoidModel<LivingEntity> implements ArmorDecoModel {

    private final ModelPart plume;
    private final ModelPart plumeBand;
    private final ModelPart pauldronLeft;
    private final ModelPart pauldronRight;
    private final ModelPart greaveRight;
    private final ModelPart greaveLeft;
    private final ModelPart furCollar;
    private final ModelPart furCollarTip;
    private final ModelPart hornLeft;
    private final ModelPart hornRight;
    private final ModelPart hornBack;
    private final ModelPart stud_1;
    private final ModelPart stud_2;

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

    public TungstenArmorModel(ModelPart root) {
        super(root);
        this.plume = root.getChild("head").getChild("plume");
        this.plumeBand = root.getChild("head").getChild("plume_band");
        this.pauldronLeft = root.getChild("body").getChild("pauldron_left");
        this.pauldronRight = root.getChild("body").getChild("pauldron_right");
        this.greaveRight = root.getChild("right_leg").getChild("greave_right");
        this.greaveLeft = root.getChild("left_leg").getChild("greave_left");
        this.furCollar = root.getChild("body").getChild("fur_collar");
        this.furCollarTip = root.getChild("body").getChild("fur_collar_tip");
        this.hornLeft = root.getChild("head").getChild("horn_left");
        this.hornRight = root.getChild("head").getChild("horn_right");
        this.hornBack = root.getChild("head").getChild("horn_back");
        this.stud_1 = root.getChild("body").getChild("stud_1");
        this.stud_2 = root.getChild("body").getChild("stud_2");
    }

    public static LayerDefinition createArmorLayer(CubeDeformation deformation) {
        MeshDefinition mesh = HumanoidModel.createMesh(deformation, 0.0F);
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("plume",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -13, 1, 1, 5.699999999999999F, 5),
                PartPose.offset(0, -8, 0));
        head.addOrReplaceChild("plume_band",
                CubeListBuilder.create().texOffs(14, 0).addBox(-1, -8, 0, 2, 0.95F, 6),
                PartPose.offset(0, -8, 0));
        head.addOrReplaceChild("horn_left",
                CubeListBuilder.create().texOffs(96, 13).addBox(-6, -3, -1, 2, 5.699999999999999F, 2),
                PartPose.offset(0, -8, 0));
        head.addOrReplaceChild("horn_right",
                CubeListBuilder.create().texOffs(106, 13).addBox(4, -3, -1, 2, 5.699999999999999F, 2),
                PartPose.offset(0, -8, 0));
        head.addOrReplaceChild("horn_back",
                CubeListBuilder.create().texOffs(116, 13).addBox(-1, -2, 3.5F, 2, 2.8499999999999996F, 2),
                PartPose.offset(0, -8, 0));
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("pauldron_left",
                CubeListBuilder.create().texOffs(32, 0).addBox(-11.500350377359428F, -2, -3, 7.450350377359428F, 4, 6),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("pauldron_right",
                CubeListBuilder.create().texOffs(61, 0).addBox(4.050000000000001F, -2, -3, 7.450350377359428F, 4, 6),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("fur_collar",
                CubeListBuilder.create().texOffs(22, 13).addBox(-7.290000000000001F, -1, -3.4F, 14.580000000000002F, 2.2F, 6.8F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("fur_collar_tip",
                CubeListBuilder.create().texOffs(67, 13).addBox(-4.050000000000001F, -2.2F, -2.6F, 8.100000000000001F, 1.6F, 5.2F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("stud_1",
                CubeListBuilder.create().texOffs(126, 13).addBox(1.2522458237363026F, 3.3477920591831207F, -2.85F, 0.6F, 0.6F, 0.4F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("stud_2",
                CubeListBuilder.create().texOffs(0, 26).addBox(0.8975951386382803F, 3.112543744035065F, -2.85F, 0.6F, 0.6F, 0.4F),
                PartPose.offset(0, 0, 0));
        PartDefinition rightleg = root.getChild("right_leg");
        rightleg.addOrReplaceChild("greave_right",
                CubeListBuilder.create().texOffs(90, 0).addBox(-2.4F, 5, -2.4F, 4.8F, 5.699999999999999F, 4.8F),
                PartPose.offset(0, 0, 0));
        PartDefinition leftleg = root.getChild("left_leg");
        leftleg.addOrReplaceChild("greave_left",
                CubeListBuilder.create().texOffs(0, 13).addBox(-2.4F, 5, -2.4F, 4.8F, 5.699999999999999F, 4.8F),
                PartPose.offset(0, 0, 0));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        // 仅装饰件；基础身体由 layer 贴图负责（脸/手已在贴图上镂空）
        if (this.showHead) {
            poseStack.pushPose();
            this.head.translateAndRotate(poseStack);
            this.plume.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.plumeBand.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.hornLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.hornRight.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.hornBack.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
        if (this.showBody) {
            poseStack.pushPose();
            this.body.translateAndRotate(poseStack);
            this.pauldronLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.pauldronRight.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.furCollar.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.furCollarTip.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.stud_1.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.stud_2.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
        if (this.showLegs) {
            poseStack.pushPose();
            this.rightLeg.translateAndRotate(poseStack);
            this.greaveRight.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
        if (this.showLegs) {
            poseStack.pushPose();
            this.leftLeg.translateAndRotate(poseStack);
            this.greaveLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
    }
}
