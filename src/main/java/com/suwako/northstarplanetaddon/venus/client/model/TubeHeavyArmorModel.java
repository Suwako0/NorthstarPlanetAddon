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
 * TubeHeavyArmorModel — 按「tube_heavy」生成的装饰盔甲模型（15 部件 / 13 装饰）。
 * 主题：flame；种子：tube_heavy|redesign2026|furnace_breaker|spiked。
 * 贴图：assets/northstar_planet_addon/textures/models/armor/tube_heavy_armor.png（128x128）。
 *
 * 只渲染装饰件，基础身体交给 tube_heavy_layer_1/2.png；
 * 生成的 layer 贴图脸部窗口与手部是镂空的，所以脸和手一定可见。
 */
public class TubeHeavyArmorModel extends HumanoidModel<LivingEntity> implements ArmorDecoModel {

    private final ModelPart plume;
    private final ModelPart plumeBand;
    private final ModelPart spike_1;
    private final ModelPart spike_2;
    private final ModelPart spike_3;
    private final ModelPart kneeRight;
    private final ModelPart kneeLeft;
    private final ModelPart cape;
    private final ModelPart capeTrim;
    private final ModelPart visorSlitL;
    private final ModelPart visorSlitR;
    private final ModelPart greaveRight;
    private final ModelPart greaveLeft;
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

    public TubeHeavyArmorModel(ModelPart root) {
        super(root);
        this.plume = root.getChild("head").getChild("plume");
        this.plumeBand = root.getChild("head").getChild("plume_band");
        this.spike_1 = root.getChild("body").getChild("spike_1");
        this.spike_2 = root.getChild("body").getChild("spike_2");
        this.spike_3 = root.getChild("body").getChild("spike_3");
        this.kneeRight = root.getChild("right_leg").getChild("knee_right");
        this.kneeLeft = root.getChild("left_leg").getChild("knee_left");
        this.cape = root.getChild("body").getChild("cape");
        this.capeTrim = root.getChild("body").getChild("cape_trim");
        this.visorSlitL = root.getChild("head").getChild("visor_slit_l");
        this.visorSlitR = root.getChild("head").getChild("visor_slit_r");
        this.greaveRight = root.getChild("right_leg").getChild("greave_right");
        this.greaveLeft = root.getChild("left_leg").getChild("greave_left");
        this.stud_1 = root.getChild("body").getChild("stud_1");
        this.stud_2 = root.getChild("body").getChild("stud_2");
    }

    public static LayerDefinition createArmorLayer(CubeDeformation deformation) {
        MeshDefinition mesh = HumanoidModel.createMesh(deformation, 0.0F);
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.getChild("head");
        head.addOrReplaceChild("plume",
                CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -13, 1, 1, 6, 5),
                PartPose.offset(0, -8, 0));
        head.addOrReplaceChild("plume_band",
                CubeListBuilder.create().texOffs(14, 0).addBox(-1, -8, 0, 2, 1, 6),
                PartPose.offset(0, -8, 0));
        head.addOrReplaceChild("visor_slit_l",
                CubeListBuilder.create().texOffs(21, 15).addBox(-3, -4.4F, -5.2F, 2, 0.8F, 0.4F),
                PartPose.offset(0, -8, 0));
        head.addOrReplaceChild("visor_slit_r",
                CubeListBuilder.create().texOffs(28, 15).addBox(1, -4.4F, -5.2F, 2, 0.8F, 0.4F),
                PartPose.offset(0, -8, 0));
        PartDefinition body = root.getChild("body");
        body.addOrReplaceChild("spike_1",
                CubeListBuilder.create().texOffs(32, 0).addBox(-6.483290819479152F, 3.6595700085163116F, -1, 1.888F, 3, 1.6F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("spike_2",
                CubeListBuilder.create().texOffs(41, 0).addBox(5.161083215000108F, 2.2917595226317644F, -1, 1.888F, 3, 1.6F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("spike_3",
                CubeListBuilder.create().texOffs(50, 0).addBox(-7.370639822566881F, 3.477500936947763F, -1, 1.888F, 3, 1.6F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("cape",
                CubeListBuilder.create().texOffs(89, 0).addBox(-4.5F, 1, 2.2F, 9, 11.743474731687456F, 0.6F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("cape_trim",
                CubeListBuilder.create().texOffs(0, 15).addBox(-4.5F, 11.543474731687457F, 2.4F, 9, 1.2F, 0.4F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("stud_1",
                CubeListBuilder.create().texOffs(79, 15).addBox(2.1721182722831145F, 3.629219708032906F, -2.85F, 0.6F, 0.6F, 0.4F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("stud_2",
                CubeListBuilder.create().texOffs(83, 15).addBox(-1.2689299217890948F, 4.764538639225066F, -2.85F, 0.6F, 0.6F, 0.4F),
                PartPose.offset(0, 0, 0));
        PartDefinition rightleg = root.getChild("right_leg");
        rightleg.addOrReplaceChild("knee_right",
                CubeListBuilder.create().texOffs(59, 0).addBox(-2.5F, 4, -3, 5, 2, 1.2F),
                PartPose.offset(0, 0, 0));
        rightleg.addOrReplaceChild("greave_right",
                CubeListBuilder.create().texOffs(35, 15).addBox(-2.4F, 5, -2.4F, 4.8F, 6, 4.8F),
                PartPose.offset(0, 0, 0));
        PartDefinition leftleg = root.getChild("left_leg");
        leftleg.addOrReplaceChild("knee_left",
                CubeListBuilder.create().texOffs(74, 0).addBox(-2.5F, 4, -3, 5, 2, 1.2F),
                PartPose.offset(0, 0, 0));
        leftleg.addOrReplaceChild("greave_left",
                CubeListBuilder.create().texOffs(57, 15).addBox(-2.4F, 5, -2.4F, 4.8F, 6, 4.8F),
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
            this.visorSlitL.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.visorSlitR.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
        if (this.showBody) {
            poseStack.pushPose();
            this.body.translateAndRotate(poseStack);
            this.spike_1.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.spike_2.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.spike_3.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.cape.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.capeTrim.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.stud_1.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.stud_2.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
        if (this.showLegs) {
            poseStack.pushPose();
            this.rightLeg.translateAndRotate(poseStack);
            this.kneeRight.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.greaveRight.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
        if (this.showLegs) {
            poseStack.pushPose();
            this.leftLeg.translateAndRotate(poseStack);
            this.kneeLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
            this.greaveLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
            poseStack.popPose();
        }
    }
}
