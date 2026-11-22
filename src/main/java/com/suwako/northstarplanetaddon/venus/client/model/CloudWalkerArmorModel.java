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
 * CloudWalkerArmorModel — 云行者套装的 3D 装饰件（79 部件）。
 *
 * <p>按参考图 docs/previews/cloud_walker-reference.jpg 重做：兜帽 + 颈巾 + 分层肩甲 +
 * 胸口人字甲 + 腰封、背后蝶结与长飘带、膜翼（翼膜形状画在贴图 alpha 里）、
 * 破损长袍下摆、护膝 / 胫甲 / 战靴。</p>
 *
 * <p>贴图：assets/northstar_planet_addon/textures/models/armor/cloud_walker_armor.png（256x256）。</p>
 * <p>只画装饰件，基础甲面交给 cloud_walker_layer_1/2.png；脸和手由 layer 贴图开口保证可见。</p>
 *
 * <p>生成脚本：tools/gen-cloud-walker-armor.py（改形制请改脚本，不要手改本文件）。</p>
 */
public class CloudWalkerArmorModel extends HumanoidModel<LivingEntity> implements ArmorDecoModel {

    private final ModelPart hoodTop;
    private final ModelPart hoodTop2;
    private final ModelPart hoodCrest;
    private final ModelPart hoodSideL;
    private final ModelPart hoodSideR;
    private final ModelPart hoodBack;
    private final ModelPart hoodCheekL;
    private final ModelPart hoodCheekR;
    private final ModelPart hoodChin;
    private final ModelPart hoodFlapL;
    private final ModelPart hoodFlapR;
    private final ModelPart cowlFront;
    private final ModelPart cowlShoulderL;
    private final ModelPart cowlShoulderR;
    private final ModelPart cowlFoldL;
    private final ModelPart cowlFoldR;
    private final ModelPart pauldronCapL;
    private final ModelPart pauldronCapR;
    private final ModelPart pauldronLameL;
    private final ModelPart pauldronLameR;
    private final ModelPart pauldronTipL;
    private final ModelPart pauldronTipR;
    private final ModelPart pauldronCrestL;
    private final ModelPart pauldronCrestR;
    private final ModelPart chestCenter;
    private final ModelPart sash;
    private final ModelPart sashV;
    private final ModelPart sashKnot;
    private final ModelPart backplate;
    private final ModelPart backSpine;
    private final ModelPart bowKnot;
    private final ModelPart bowLoopL;
    private final ModelPart bowLoopR;
    private final ModelPart ribbonBackL;
    private final ModelPart ribbonBackR;
    private final ModelPart wingHubL;
    private final ModelPart wingHubR;
    private final ModelPart wingMembraneL;
    private final ModelPart wingMembraneR;
    private final ModelPart wingSpar1L;
    private final ModelPart wingSpar1R;
    private final ModelPart wingSpar2L;
    private final ModelPart wingSpar2R;
    private final ModelPart wingSpar3L;
    private final ModelPart wingSpar3R;
    private final ModelPart wingSpar4L;
    private final ModelPart wingSpar4R;
    private final ModelPart wingClawL;
    private final ModelPart wingClawR;
    private final ModelPart skirtFrontL;
    private final ModelPart skirtFrontR;
    private final ModelPart skirtApron;
    private final ModelPart skirtSideL;
    private final ModelPart skirtSideR;
    private final ModelPart skirtBack;
    private final ModelPart skirtFlareL;
    private final ModelPart skirtFlareR;
    private final ModelPart tassetL;
    private final ModelPart tassetR;
    private final ModelPart kneeL;
    private final ModelPart kneeR;
    private final ModelPart shinL;
    private final ModelPart shinR;
    private final ModelPart thighStrapL;
    private final ModelPart thighStrapR;
    private final ModelPart bootCuffL;
    private final ModelPart bootCuffR;
    private final ModelPart bootToeL;
    private final ModelPart bootToeR;
    private final ModelPart bootHeelL;
    private final ModelPart bootHeelR;
    private final ModelPart sleeveL;
    private final ModelPart sleeveR;
    private final ModelPart elbowFinL;
    private final ModelPart elbowFinR;
    private final ModelPart bracerL;
    private final ModelPart bracerR;
    private final ModelPart cuffL;
    private final ModelPart cuffR;

    private boolean showHead = true;
    private boolean showChest = true;
    private boolean showLegs = true;
    private boolean showBoots = true;

    /** 按部位所属槽位显示：裙摆挂在 body 骨上但只在穿护腿时画。 */
    public void setSlot(ArmorItem.Type type) {
        if (type == ArmorItem.Type.BODY) {
            this.showHead = true;
            this.showChest = true;
            this.showLegs = true;
            this.showBoots = true;
            return;
        }
        this.showHead = type == ArmorItem.Type.HELMET;
        this.showChest = type == ArmorItem.Type.CHESTPLATE;
        this.showLegs = type == ArmorItem.Type.LEGGINGS;
        this.showBoots = type == ArmorItem.Type.BOOTS;
    }

    public CloudWalkerArmorModel(ModelPart root) {
        super(root);
        ModelPart headPart = root.getChild("head");
        ModelPart bodyPart = root.getChild("body");
        ModelPart leftArmPart = root.getChild("left_arm");
        ModelPart rightArmPart = root.getChild("right_arm");
        ModelPart leftLegPart = root.getChild("left_leg");
        ModelPart rightLegPart = root.getChild("right_leg");
        this.hoodTop = headPart.getChild("hood_top");
        this.hoodTop2 = headPart.getChild("hood_top2");
        this.hoodCrest = headPart.getChild("hood_crest");
        this.hoodSideL = headPart.getChild("hood_side_l");
        this.hoodSideR = headPart.getChild("hood_side_r");
        this.hoodBack = headPart.getChild("hood_back");
        this.hoodCheekL = headPart.getChild("hood_cheek_l");
        this.hoodCheekR = headPart.getChild("hood_cheek_r");
        this.hoodChin = headPart.getChild("hood_chin");
        this.hoodFlapL = headPart.getChild("hood_flap_l");
        this.hoodFlapR = headPart.getChild("hood_flap_r");
        this.cowlFront = bodyPart.getChild("cowl_front");
        this.cowlShoulderL = bodyPart.getChild("cowl_shoulder_l");
        this.cowlShoulderR = bodyPart.getChild("cowl_shoulder_r");
        this.cowlFoldL = bodyPart.getChild("cowl_fold_l");
        this.cowlFoldR = bodyPart.getChild("cowl_fold_r");
        this.pauldronCapL = bodyPart.getChild("pauldron_cap_l");
        this.pauldronCapR = bodyPart.getChild("pauldron_cap_r");
        this.pauldronLameL = bodyPart.getChild("pauldron_lame_l");
        this.pauldronLameR = bodyPart.getChild("pauldron_lame_r");
        this.pauldronTipL = bodyPart.getChild("pauldron_tip_l");
        this.pauldronTipR = bodyPart.getChild("pauldron_tip_r");
        this.pauldronCrestL = bodyPart.getChild("pauldron_crest_l");
        this.pauldronCrestR = bodyPart.getChild("pauldron_crest_r");
        this.chestCenter = bodyPart.getChild("chest_center");
        this.sash = bodyPart.getChild("sash");
        this.sashV = bodyPart.getChild("sash_v");
        this.sashKnot = bodyPart.getChild("sash_knot");
        this.backplate = bodyPart.getChild("backplate");
        this.backSpine = bodyPart.getChild("back_spine");
        this.bowKnot = bodyPart.getChild("bow_knot");
        this.bowLoopL = bodyPart.getChild("bow_loop_l");
        this.bowLoopR = bodyPart.getChild("bow_loop_r");
        this.ribbonBackL = bodyPart.getChild("ribbon_back_l");
        this.ribbonBackR = bodyPart.getChild("ribbon_back_r");
        this.wingHubL = bodyPart.getChild("wing_hub_l");
        this.wingHubR = bodyPart.getChild("wing_hub_r");
        this.wingMembraneL = bodyPart.getChild("wing_membrane_l");
        this.wingMembraneR = bodyPart.getChild("wing_membrane_r");
        this.wingSpar1L = bodyPart.getChild("wing_spar1_l");
        this.wingSpar1R = bodyPart.getChild("wing_spar1_r");
        this.wingSpar2L = bodyPart.getChild("wing_spar2_l");
        this.wingSpar2R = bodyPart.getChild("wing_spar2_r");
        this.wingSpar3L = bodyPart.getChild("wing_spar3_l");
        this.wingSpar3R = bodyPart.getChild("wing_spar3_r");
        this.wingSpar4L = bodyPart.getChild("wing_spar4_l");
        this.wingSpar4R = bodyPart.getChild("wing_spar4_r");
        this.wingClawL = bodyPart.getChild("wing_claw_l");
        this.wingClawR = bodyPart.getChild("wing_claw_r");
        this.skirtFrontL = bodyPart.getChild("skirt_front_l");
        this.skirtFrontR = bodyPart.getChild("skirt_front_r");
        this.skirtApron = bodyPart.getChild("skirt_apron");
        this.skirtSideL = bodyPart.getChild("skirt_side_l");
        this.skirtSideR = bodyPart.getChild("skirt_side_r");
        this.skirtBack = bodyPart.getChild("skirt_back");
        this.skirtFlareL = bodyPart.getChild("skirt_flare_l");
        this.skirtFlareR = bodyPart.getChild("skirt_flare_r");
        this.tassetL = bodyPart.getChild("tasset_l");
        this.tassetR = bodyPart.getChild("tasset_r");
        this.kneeL = leftLegPart.getChild("knee_l");
        this.kneeR = rightLegPart.getChild("knee_r");
        this.shinL = leftLegPart.getChild("shin_l");
        this.shinR = rightLegPart.getChild("shin_r");
        this.thighStrapL = leftLegPart.getChild("thigh_strap_l");
        this.thighStrapR = rightLegPart.getChild("thigh_strap_r");
        this.bootCuffL = leftLegPart.getChild("boot_cuff_l");
        this.bootCuffR = rightLegPart.getChild("boot_cuff_r");
        this.bootToeL = leftLegPart.getChild("boot_toe_l");
        this.bootToeR = rightLegPart.getChild("boot_toe_r");
        this.bootHeelL = leftLegPart.getChild("boot_heel_l");
        this.bootHeelR = rightLegPart.getChild("boot_heel_r");
        this.sleeveL = leftArmPart.getChild("sleeve_l");
        this.sleeveR = rightArmPart.getChild("sleeve_r");
        this.elbowFinL = leftArmPart.getChild("elbow_fin_l");
        this.elbowFinR = rightArmPart.getChild("elbow_fin_r");
        this.bracerL = leftArmPart.getChild("bracer_l");
        this.bracerR = rightArmPart.getChild("bracer_r");
        this.cuffL = leftArmPart.getChild("cuff_l");
        this.cuffR = rightArmPart.getChild("cuff_r");
    }

    public static LayerDefinition createArmorLayer(CubeDeformation deformation) {
        MeshDefinition mesh = HumanoidModel.createMesh(deformation, 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        PartDefinition body = root.getChild("body");
        PartDefinition left_arm = root.getChild("left_arm");
        PartDefinition right_arm = root.getChild("right_arm");
        PartDefinition left_leg = root.getChild("left_leg");
        PartDefinition right_leg = root.getChild("right_leg");
        head.addOrReplaceChild("hood_top",
                CubeListBuilder.create().texOffs(80, 0).addBox(-5.2F, -9, -5.4F, 10, 2, 10),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("hood_top2",
                CubeListBuilder.create().texOffs(170, 0).addBox(-4.4F, -9.8F, -4.6F, 9, 1, 9),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("hood_crest",
                CubeListBuilder.create().texOffs(0, 54).addBox(-1.5F, -10.6F, -2.4F, 3, 1, 6),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("hood_side_l",
                CubeListBuilder.create().texOffs(122, 0).addBox(3.8F, -8.9F, -5.2F, 1, 10, 10),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("hood_side_r",
                CubeListBuilder.create().texOffs(146, 0).addBox(-5.2F, -8.9F, -5.2F, 1, 10, 10),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("hood_back",
                CubeListBuilder.create().texOffs(70, 22).addBox(-5.2F, -8.8F, 3.6F, 10, 10, 1),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("hood_cheek_l",
                CubeListBuilder.create().texOffs(140, 54).addBox(3, -6.9F, -5.2F, 2, 8, 2),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("hood_cheek_r",
                CubeListBuilder.create().texOffs(150, 54).addBox(-4.9F, -6.9F, -5.2F, 2, 8, 2),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("hood_chin",
                CubeListBuilder.create().texOffs(68, 68).addBox(-3, 0.2F, -5.2F, 6, 1, 2),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("hood_flap_l",
                CubeListBuilder.create().texOffs(20, 54).addBox(4.2F, -5.4F, -3.6F, 1, 8, 4),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("hood_flap_r",
                CubeListBuilder.create().texOffs(32, 54).addBox(-5.6F, -5.4F, -3.6F, 1, 8, 4),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("cowl_front",
                CubeListBuilder.create().texOffs(116, 54).addBox(-4.8F, -1.2F, -3.2F, 10, 4, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("cowl_shoulder_l",
                CubeListBuilder.create().texOffs(198, 22).addBox(2.6F, -1.4F, -2.8F, 4, 4, 6),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("cowl_shoulder_r",
                CubeListBuilder.create().texOffs(220, 22).addBox(-6.4F, -1.4F, -2.8F, 4, 4, 6),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("cowl_fold_l",
                CubeListBuilder.create().texOffs(68, 81).addBox(3.2F, -0.2F, -3.4F, 3, 2, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("cowl_fold_r",
                CubeListBuilder.create().texOffs(78, 81).addBox(-5.8F, -0.2F, -3.4F, 3, 2, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("pauldron_cap_l",
                CubeListBuilder.create().texOffs(52, 40).addBox(4.2F, -1.6F, -2.8F, 4, 3, 6),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("pauldron_cap_r",
                CubeListBuilder.create().texOffs(74, 40).addBox(-7.8F, -1.6F, -2.8F, 4, 3, 6),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("pauldron_lame_l",
                CubeListBuilder.create().texOffs(44, 54).addBox(4.4F, 1.6F, -2.6F, 3, 2, 5),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("pauldron_lame_r",
                CubeListBuilder.create().texOffs(62, 54).addBox(-7.4F, 1.6F, -2.6F, 3, 2, 5),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("pauldron_tip_l",
                CubeListBuilder.create().texOffs(80, 54).addBox(4.4F, 3.4F, -2.4F, 3, 2, 5),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("pauldron_tip_r",
                CubeListBuilder.create().texOffs(98, 54).addBox(-7.2F, 3.4F, -2.4F, 3, 2, 5),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("pauldron_crest_l",
                CubeListBuilder.create().texOffs(196, 54).addBox(4.4F, -2.4F, -1.8F, 3, 1, 4),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("pauldron_crest_r",
                CubeListBuilder.create().texOffs(212, 54).addBox(-7.2F, -2.4F, -1.8F, 3, 1, 4),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("chest_center",
                CubeListBuilder.create().texOffs(44, 81).addBox(-0.4F, 1.6F, -2.9F, 1, 7, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("sash",
                CubeListBuilder.create().texOffs(0, 22).addBox(-4.8F, 8.8F, -2.9F, 10, 2, 6),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("sash_v",
                CubeListBuilder.create().texOffs(50, 81).addBox(-3.4F, 9.8F, -3, 7, 1, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("sash_knot",
                CubeListBuilder.create().texOffs(152, 81).addBox(3.2F, 8.9F, -3.4F, 2, 2, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("backplate",
                CubeListBuilder.create().texOffs(148, 40).addBox(-4.2F, 0.4F, 2, 8, 8, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("back_spine",
                CubeListBuilder.create().texOffs(10, 81).addBox(-0.5F, 0.8F, 2.5F, 1, 8, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("bow_knot",
                CubeListBuilder.create().texOffs(234, 68).addBox(-1.4F, -0.8F, 3.4F, 3, 2, 2),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("bow_loop_l",
                CubeListBuilder.create().texOffs(88, 81).addBox(-4, -1.4F, 3.6F, 3, 2, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("bow_loop_r",
                CubeListBuilder.create().texOffs(98, 81).addBox(1.2F, -1.4F, 3.6F, 3, 2, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("ribbon_back_l",
                CubeListBuilder.create().texOffs(16, 68).addBox(-2.5F, 1, 4, 2, 10, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("ribbon_back_r",
                CubeListBuilder.create().texOffs(60, 68).addBox(0.9F, 1, 4.2F, 2, 8, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("wing_hub_l",
                CubeListBuilder.create().texOffs(246, 68).addBox(1.8F, -1.2F, 2.4F, 2, 3, 2),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("wing_hub_r",
                CubeListBuilder.create().texOffs(0, 81).addBox(-4.2F, -1.2F, 2.4F, 2, 3, 2),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("wing_membrane_l",
                CubeListBuilder.create().texOffs(0, 0).addBox(2.4F, -5.4F, 3.2F, 18, 13, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("wing_membrane_r",
                CubeListBuilder.create().texOffs(40, 0).addBox(-20, -5.4F, 3.2F, 18, 13, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("wing_spar1_l",
                CubeListBuilder.create().texOffs(122, 68).addBox(2.6F, -0.4F, 2.7F, 5, 1, 2),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("wing_spar1_r",
                CubeListBuilder.create().texOffs(138, 68).addBox(-8, -0.4F, 2.7F, 5, 1, 2),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("wing_spar2_l",
                CubeListBuilder.create().texOffs(86, 68).addBox(7.2F, -2.2F, 2.7F, 6, 1, 2),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("wing_spar2_r",
                CubeListBuilder.create().texOffs(104, 68).addBox(-13, -2.2F, 2.7F, 6, 1, 2),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("wing_spar3_l",
                CubeListBuilder.create().texOffs(154, 68).addBox(12.2F, -4.2F, 2.7F, 5, 1, 2),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("wing_spar3_r",
                CubeListBuilder.create().texOffs(170, 68).addBox(-17.6F, -4.2F, 2.7F, 5, 1, 2),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("wing_spar4_l",
                CubeListBuilder.create().texOffs(16, 81).addBox(16.8F, -3.2F, 2.7F, 4, 1, 2),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("wing_spar4_r",
                CubeListBuilder.create().texOffs(30, 81).addBox(-20.4F, -3.2F, 2.7F, 4, 1, 2),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("wing_claw_l",
                CubeListBuilder.create().texOffs(160, 81).addBox(19.2F, -0.6F, 2.8F, 2, 2, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("wing_claw_r",
                CubeListBuilder.create().texOffs(168, 81).addBox(-21, -0.6F, 2.8F, 2, 2, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("skirt_front_l",
                CubeListBuilder.create().texOffs(204, 40).addBox(0.2F, 9.2F, -3.4F, 5, 11, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("skirt_front_r",
                CubeListBuilder.create().texOffs(218, 40).addBox(-5.6F, 9.2F, -3.4F, 5, 11, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("skirt_apron",
                CubeListBuilder.create().texOffs(232, 40).addBox(-3.6F, 9.4F, -3.9F, 7, 8, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("skirt_side_l",
                CubeListBuilder.create().texOffs(34, 22).addBox(4.8F, 9.2F, -3.2F, 2, 10, 6),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("skirt_side_r",
                CubeListBuilder.create().texOffs(52, 22).addBox(-6.4F, 9.2F, -3.2F, 2, 10, 6),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("skirt_back",
                CubeListBuilder.create().texOffs(208, 0).addBox(-5.2F, 9.2F, 2.4F, 10, 12, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("skirt_flare_l",
                CubeListBuilder.create().texOffs(186, 68).addBox(5, 13, -3, 2, 6, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("skirt_flare_r",
                CubeListBuilder.create().texOffs(194, 68).addBox(-7, 13, -3, 2, 6, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("tasset_l",
                CubeListBuilder.create().texOffs(168, 40).addBox(4.2F, 9, -3.2F, 2, 4, 6),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("tasset_r",
                CubeListBuilder.create().texOffs(186, 40).addBox(-6, 9, -3.2F, 2, 4, 6),
                PartPose.offset(0, 0, 0));
        left_leg.addOrReplaceChild("knee_l",
                CubeListBuilder.create().texOffs(24, 68).addBox(-2.9F, 3.6F, -3.4F, 6, 2, 2),
                PartPose.offset(0, 0, 0));
        right_leg.addOrReplaceChild("knee_r",
                CubeListBuilder.create().texOffs(42, 68).addBox(-2.9F, 3.6F, -3.4F, 6, 2, 2),
                PartPose.offset(0, 0, 0));
        left_leg.addOrReplaceChild("shin_l",
                CubeListBuilder.create().texOffs(228, 54).addBox(-2.7F, 6, -3.3F, 5, 3, 2),
                PartPose.offset(0, 0, 0));
        right_leg.addOrReplaceChild("shin_r",
                CubeListBuilder.create().texOffs(0, 68).addBox(-2.7F, 6, -3.3F, 5, 3, 2),
                PartPose.offset(0, 0, 0));
        left_leg.addOrReplaceChild("thigh_strap_l",
                CubeListBuilder.create().texOffs(108, 81).addBox(-2.6F, 1, -2.9F, 5, 1, 1),
                PartPose.offset(0, 0, 0));
        right_leg.addOrReplaceChild("thigh_strap_r",
                CubeListBuilder.create().texOffs(122, 81).addBox(-2.6F, 1, -2.9F, 5, 1, 1),
                PartPose.offset(0, 0, 0));
        left_leg.addOrReplaceChild("boot_cuff_l",
                CubeListBuilder.create().texOffs(0, 40).addBox(-3, 9.4F, -3, 6, 2, 6),
                PartPose.offset(0, 0, 0));
        right_leg.addOrReplaceChild("boot_cuff_r",
                CubeListBuilder.create().texOffs(26, 40).addBox(-3, 9.4F, -3, 6, 2, 6),
                PartPose.offset(0, 0, 0));
        left_leg.addOrReplaceChild("boot_toe_l",
                CubeListBuilder.create().texOffs(160, 54).addBox(-2.7F, 10.6F, -4.6F, 5, 2, 3),
                PartPose.offset(0, 0, 0));
        right_leg.addOrReplaceChild("boot_toe_r",
                CubeListBuilder.create().texOffs(178, 54).addBox(-2.7F, 10.6F, -4.6F, 5, 2, 3),
                PartPose.offset(0, 0, 0));
        left_leg.addOrReplaceChild("boot_heel_l",
                CubeListBuilder.create().texOffs(202, 68).addBox(-2.4F, 10.8F, 2, 5, 1, 2),
                PartPose.offset(0, 0, 0));
        right_leg.addOrReplaceChild("boot_heel_r",
                CubeListBuilder.create().texOffs(218, 68).addBox(-2.4F, 10.8F, 2, 5, 1, 2),
                PartPose.offset(0, 0, 0));
        left_arm.addOrReplaceChild("sleeve_l",
                CubeListBuilder.create().texOffs(94, 22).addBox(-2.9F, -2.4F, -2.9F, 6, 4, 6),
                PartPose.offset(0, 0, 0));
        right_arm.addOrReplaceChild("sleeve_r",
                CubeListBuilder.create().texOffs(120, 22).addBox(-2.9F, -2.4F, -2.9F, 6, 4, 6),
                PartPose.offset(0, 0, 0));
        left_arm.addOrReplaceChild("elbow_fin_l",
                CubeListBuilder.create().texOffs(136, 81).addBox(1.8F, 1.4F, -1, 1, 2, 2),
                PartPose.offset(0, 0, 0));
        right_arm.addOrReplaceChild("elbow_fin_r",
                CubeListBuilder.create().texOffs(144, 81).addBox(-3.2F, 1.4F, -1, 1, 2, 2),
                PartPose.offset(0, 0, 0));
        left_arm.addOrReplaceChild("bracer_l",
                CubeListBuilder.create().texOffs(146, 22).addBox(-2.9F, 3.8F, -2.9F, 6, 4, 6),
                PartPose.offset(0, 0, 0));
        right_arm.addOrReplaceChild("bracer_r",
                CubeListBuilder.create().texOffs(172, 22).addBox(-2.9F, 3.8F, -2.9F, 6, 4, 6),
                PartPose.offset(0, 0, 0));
        left_arm.addOrReplaceChild("cuff_l",
                CubeListBuilder.create().texOffs(96, 40).addBox(-3, 7.4F, -3, 6, 1, 6),
                PartPose.offset(0, 0, 0));
        right_arm.addOrReplaceChild("cuff_r",
                CubeListBuilder.create().texOffs(122, 40).addBox(-3, 7.4F, -3, 6, 1, 6),
                PartPose.offset(0, 0, 0));
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        // 只画装饰件；基础身体由 layer 贴图负责，脸和手不会被甲片盖住
        poseStack.pushPose();
        this.head.translateAndRotate(poseStack);
        if (showHead) {
            this.hoodTop.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showHead) {
            this.hoodTop2.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showHead) {
            this.hoodCrest.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showHead) {
            this.hoodSideL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showHead) {
            this.hoodSideR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showHead) {
            this.hoodBack.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showHead) {
            this.hoodCheekL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showHead) {
            this.hoodCheekR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showHead) {
            this.hoodChin.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showHead) {
            this.hoodFlapL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showHead) {
            this.hoodFlapR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        poseStack.popPose();
        poseStack.pushPose();
        this.body.translateAndRotate(poseStack);
        if (showChest) {
            this.cowlFront.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.cowlShoulderL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.cowlShoulderR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.cowlFoldL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.cowlFoldR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.pauldronCapL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.pauldronCapR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.pauldronLameL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.pauldronLameR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.pauldronTipL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.pauldronTipR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.pauldronCrestL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.pauldronCrestR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.chestCenter.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.sash.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.sashV.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.sashKnot.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.backplate.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.backSpine.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.bowKnot.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.bowLoopL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.bowLoopR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.ribbonBackL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.ribbonBackR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.wingHubL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.wingHubR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.wingMembraneL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.wingMembraneR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.wingSpar1L.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.wingSpar1R.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.wingSpar2L.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.wingSpar2R.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.wingSpar3L.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.wingSpar3R.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.wingSpar4L.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.wingSpar4R.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.wingClawL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.wingClawR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.skirtFrontL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.skirtFrontR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.skirtApron.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.skirtSideL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.skirtSideR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.skirtBack.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.skirtFlareL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.skirtFlareR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.tassetL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.tassetR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        poseStack.popPose();
        poseStack.pushPose();
        this.leftArm.translateAndRotate(poseStack);
        if (showChest) {
            this.sleeveL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.elbowFinL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.bracerL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.cuffL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        poseStack.popPose();
        poseStack.pushPose();
        this.rightArm.translateAndRotate(poseStack);
        if (showChest) {
            this.sleeveR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.elbowFinR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.bracerR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.cuffR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        poseStack.popPose();
        poseStack.pushPose();
        this.leftLeg.translateAndRotate(poseStack);
        if (showLegs) {
            this.kneeL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.shinL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.thighStrapL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showBoots) {
            this.bootCuffL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showBoots) {
            this.bootToeL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showBoots) {
            this.bootHeelL.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        poseStack.popPose();
        poseStack.pushPose();
        this.rightLeg.translateAndRotate(poseStack);
        if (showLegs) {
            this.kneeR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.shinR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.thighStrapR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showBoots) {
            this.bootCuffR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showBoots) {
            this.bootToeR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showBoots) {
            this.bootHeelR.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        poseStack.popPose();
    }
}
