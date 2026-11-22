package com.suwako.northstarplanetaddon.mars.client.model;

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
 * MoltenCoreArmorModel — 熔核套的立体装饰件（63 部件）。
 *
 * <p>对着参考图重做：五尖冠头盔 + 发光眼缝、塔式肩甲 + 上翘尖、胸口熔核环、
 * 铜腰带 + 方扣、链饰、膝甲、赤热靴底。</p>
 *
 * <p>贴图：assets/northstar_planet_addon/textures/models/armor/molten_core_armor.png（128x128）。
 * 只画装饰件，身体本体交给 molten_core_layer_1/2.png；手部窗口仍然镂空。</p>
 *
 * <p>本类即唯一来源：形制直接改这里；改完重跑 <code>python tools/check-armor.py --set molten_core</code> 校验部件/UV。</p>
 */
public class MoltenCoreArmorModel extends HumanoidModel<LivingEntity> implements ArmorDecoModel {

    private final ModelPart platePauldronLipLeft;
    private final ModelPart platePauldronLeft;
    private final ModelPart platePauldronSpikeBLeft;
    private final ModelPart platePauldronSpikeALeft;
    private final ModelPart chain3Left;
    private final ModelPart chain4Left;
    private final ModelPart chain1Left;
    private final ModelPart chain2Left;
    private final ModelPart plateCollar;
    private final ModelPart plateTassetLeft;
    private final ModelPart platePauldronSpikeCLeft;
    private final ModelPart plateBack;
    private final ModelPart plateChest;
    private final ModelPart leatherBelt;
    private final ModelPart trimStrapLeft;
    private final ModelPart trimCoreRing;
    private final ModelPart glowCore;
    private final ModelPart trimBuckle;
    private final ModelPart plateTassetMid;
    private final ModelPart trimStrapRight;
    private final ModelPart plateTassetRight;
    private final ModelPart platePauldronRight;
    private final ModelPart platePauldronSpikeCRight;
    private final ModelPart chain3Right;
    private final ModelPart platePauldronSpikeARight;
    private final ModelPart chain4Right;
    private final ModelPart chain1Right;
    private final ModelPart chain2Right;
    private final ModelPart platePauldronLipRight;
    private final ModelPart platePauldronSpikeBRight;
    private final ModelPart plateHelmBack;
    private final ModelPart plateHelmSideLeft;
    private final ModelPart plateCrownOutBLeft;
    private final ModelPart plateCrownOutALeft;
    private final ModelPart plateHelmTop;
    private final ModelPart plateCheekLeft;
    private final ModelPart plateVisorBrow;
    private final ModelPart plateHelmChin;
    private final ModelPart plateCrownInALeft;
    private final ModelPart plateCrownInBLeft;
    private final ModelPart plateCrownBack;
    private final ModelPart plateCrownMidA;
    private final ModelPart plateCrownMidB;
    private final ModelPart plateCrownInARight;
    private final ModelPart plateCrownInBRight;
    private final ModelPart plateCheekRight;
    private final ModelPart plateCrownOutBRight;
    private final ModelPart plateCrownOutARight;
    private final ModelPart plateHelmSideRight;
    private final ModelPart plateBracerRight;
    private final ModelPart plateUpperarmRight;
    private final ModelPart plateBootRight;
    private final ModelPart plateThighRight;
    private final ModelPart plateShinRight;
    private final ModelPart plateBootCapRight;
    private final ModelPart plateKneeRight;
    private final ModelPart plateBracerLeft;
    private final ModelPart plateUpperarmLeft;
    private final ModelPart plateBootLeft;
    private final ModelPart plateShinLeft;
    private final ModelPart plateThighLeft;
    private final ModelPart plateBootCapLeft;
    private final ModelPart plateKneeLeft;

    private boolean showHead = true;
    private boolean showChest = true;
    private boolean showLegs = true;
    private boolean showBoots = true;

    /**
     * 按「部件所属槽位」开关，而不是按骨骼：腿骨上只有膝/胫属于护腿，
     * 靴子属于靴子槽；肩上和手臂上的件属于胸甲槽。
     */
    public void setSlot(ArmorItem.Type type) {
        if (type == ArmorItem.Type.BODY) {
            return;                       // 全套：全部都画
        }
        this.showHead = type == ArmorItem.Type.HELMET;
        this.showChest = type == ArmorItem.Type.CHESTPLATE;
        this.showLegs = type == ArmorItem.Type.LEGGINGS;
        this.showBoots = type == ArmorItem.Type.BOOTS;
    }

    public MoltenCoreArmorModel(ModelPart root) {
        super(root);
        ModelPart headPart = root.getChild("head");
        ModelPart bodyPart = root.getChild("body");
        ModelPart rightArmPart = root.getChild("right_arm");
        ModelPart leftArmPart = root.getChild("left_arm");
        ModelPart rightLegPart = root.getChild("right_leg");
        ModelPart leftLegPart = root.getChild("left_leg");
        this.platePauldronLipLeft = bodyPart.getChild("plate_pauldron_lip_left");
        this.platePauldronLeft = bodyPart.getChild("plate_pauldron_left");
        this.platePauldronSpikeBLeft = bodyPart.getChild("plate_pauldron_spike_b_left");
        this.platePauldronSpikeALeft = bodyPart.getChild("plate_pauldron_spike_a_left");
        this.chain3Left = bodyPart.getChild("chain_3_left");
        this.chain4Left = bodyPart.getChild("chain_4_left");
        this.chain1Left = bodyPart.getChild("chain_1_left");
        this.chain2Left = bodyPart.getChild("chain_2_left");
        this.plateCollar = bodyPart.getChild("plate_collar");
        this.plateTassetLeft = bodyPart.getChild("plate_tasset_left");
        this.platePauldronSpikeCLeft = bodyPart.getChild("plate_pauldron_spike_c_left");
        this.plateBack = bodyPart.getChild("plate_back");
        this.plateChest = bodyPart.getChild("plate_chest");
        this.leatherBelt = bodyPart.getChild("leather_belt");
        this.trimStrapLeft = bodyPart.getChild("trim_strap_left");
        this.trimCoreRing = bodyPart.getChild("trim_core_ring");
        this.glowCore = bodyPart.getChild("glow_core");
        this.trimBuckle = bodyPart.getChild("trim_buckle");
        this.plateTassetMid = bodyPart.getChild("plate_tasset_mid");
        this.trimStrapRight = bodyPart.getChild("trim_strap_right");
        this.plateTassetRight = bodyPart.getChild("plate_tasset_right");
        this.platePauldronRight = bodyPart.getChild("plate_pauldron_right");
        this.platePauldronSpikeCRight = bodyPart.getChild("plate_pauldron_spike_c_right");
        this.chain3Right = bodyPart.getChild("chain_3_right");
        this.platePauldronSpikeARight = bodyPart.getChild("plate_pauldron_spike_a_right");
        this.chain4Right = bodyPart.getChild("chain_4_right");
        this.chain1Right = bodyPart.getChild("chain_1_right");
        this.chain2Right = bodyPart.getChild("chain_2_right");
        this.platePauldronLipRight = bodyPart.getChild("plate_pauldron_lip_right");
        this.platePauldronSpikeBRight = bodyPart.getChild("plate_pauldron_spike_b_right");
        this.plateHelmBack = headPart.getChild("plate_helm_back");
        this.plateHelmSideLeft = headPart.getChild("plate_helm_side_left");
        this.plateCrownOutBLeft = headPart.getChild("plate_crown_out_b_left");
        this.plateCrownOutALeft = headPart.getChild("plate_crown_out_a_left");
        this.plateHelmTop = headPart.getChild("plate_helm_top");
        this.plateCheekLeft = headPart.getChild("plate_cheek_left");
        this.plateVisorBrow = headPart.getChild("plate_visor_brow");
        this.plateHelmChin = headPart.getChild("plate_helm_chin");
        this.plateCrownInALeft = headPart.getChild("plate_crown_in_a_left");
        this.plateCrownInBLeft = headPart.getChild("plate_crown_in_b_left");
        this.plateCrownBack = headPart.getChild("plate_crown_back");
        this.plateCrownMidA = headPart.getChild("plate_crown_mid_a");
        this.plateCrownMidB = headPart.getChild("plate_crown_mid_b");
        this.plateCrownInARight = headPart.getChild("plate_crown_in_a_right");
        this.plateCrownInBRight = headPart.getChild("plate_crown_in_b_right");
        this.plateCheekRight = headPart.getChild("plate_cheek_right");
        this.plateCrownOutBRight = headPart.getChild("plate_crown_out_b_right");
        this.plateCrownOutARight = headPart.getChild("plate_crown_out_a_right");
        this.plateHelmSideRight = headPart.getChild("plate_helm_side_right");
        this.plateBracerRight = leftArmPart.getChild("plate_bracer_right");
        this.plateUpperarmRight = leftArmPart.getChild("plate_upperarm_right");
        this.plateBootRight = leftLegPart.getChild("plate_boot_right");
        this.plateThighRight = leftLegPart.getChild("plate_thigh_right");
        this.plateShinRight = leftLegPart.getChild("plate_shin_right");
        this.plateBootCapRight = leftLegPart.getChild("plate_boot_cap_right");
        this.plateKneeRight = leftLegPart.getChild("plate_knee_right");
        this.plateBracerLeft = rightArmPart.getChild("plate_bracer_left");
        this.plateUpperarmLeft = rightArmPart.getChild("plate_upperarm_left");
        this.plateBootLeft = rightLegPart.getChild("plate_boot_left");
        this.plateShinLeft = rightLegPart.getChild("plate_shin_left");
        this.plateThighLeft = rightLegPart.getChild("plate_thigh_left");
        this.plateBootCapLeft = rightLegPart.getChild("plate_boot_cap_left");
        this.plateKneeLeft = rightLegPart.getChild("plate_knee_left");
    }

    public static LayerDefinition createArmorLayer(CubeDeformation deformation) {
        MeshDefinition mesh = HumanoidModel.createMesh(deformation, 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");
        PartDefinition body = root.getChild("body");
        PartDefinition right_arm = root.getChild("right_arm");
        PartDefinition left_arm = root.getChild("left_arm");
        PartDefinition right_leg = root.getChild("right_leg");
        PartDefinition left_leg = root.getChild("left_leg");
        body.addOrReplaceChild("plate_pauldron_lip_left",
                CubeListBuilder.create().texOffs(0, 35).addBox(-12, 2, -5, 3, 4, 10),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("plate_pauldron_left",
                CubeListBuilder.create().texOffs(66, 19).addBox(-11.05F, -1.05F, -4.05F, 7.1F, 5.1F, 9.1F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("plate_pauldron_spike_b_left",
                CubeListBuilder.create().texOffs(62, 87).addBox(-10, -6, -1, 2, 5, 3),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("plate_pauldron_spike_a_left",
                CubeListBuilder.create().texOffs(38, 87).addBox(-8.05F, -5.05F, -2.05F, 2.1F, 4.1F, 4.1F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("chain_3_left",
                CubeListBuilder.create().texOffs(96, 104).addBox(-7.1F, 7.9F, -5.1F, 1.2F, 2.2F, 1.2F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("chain_4_left",
                CubeListBuilder.create().texOffs(106, 104).addBox(-7.05F, 9.95F, -5.05F, 1.1F, 2.1F, 1.1F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("chain_1_left",
                CubeListBuilder.create().texOffs(76, 104).addBox(-7, 4, -5, 1, 2, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("chain_2_left",
                CubeListBuilder.create().texOffs(86, 104).addBox(-7, 6, -5, 1, 2, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("plate_collar",
                CubeListBuilder.create().texOffs(0, 76).addBox(-6.15F, -1.15F, -4.15F, 12.3F, 2.3F, 8.3F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("plate_tasset_left",
                CubeListBuilder.create().texOffs(95, 97).addBox(-6.05F, 11.95F, -4.05F, 3.1F, 3.1F, 1.1F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("plate_pauldron_spike_c_left",
                CubeListBuilder.create().texOffs(0, 97).addBox(-6, -4, -1, 2, 3, 3),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("plate_back",
                CubeListBuilder.create().texOffs(74, 50).addBox(-5.1F, -0.1F, 2.9F, 10.2F, 10.2F, 1.2F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("plate_chest",
                CubeListBuilder.create().texOffs(46, 0).addBox(-5, 0, -4, 10, 10, 7),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("leather_belt",
                CubeListBuilder.create().texOffs(64, 76).addBox(-5, 10, -4, 11, 2, 7),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("trim_strap_left",
                CubeListBuilder.create().texOffs(81, 97).addBox(-3.2F, 11.8F, -4.2F, 1.4F, 3.4F, 1.4F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("trim_core_ring",
                CubeListBuilder.create().texOffs(91, 87).addBox(-3.1F, 1.9F, -4.1F, 7.2F, 5.2F, 1.2F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("glow_core",
                CubeListBuilder.create().texOffs(65, 97).addBox(-2.15F, 2.85F, -4.15F, 4.3F, 3.3F, 1.3F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("trim_buckle",
                CubeListBuilder.create().texOffs(60, 104).addBox(-2.15F, 9.85F, -4.15F, 4.3F, 2.3F, 1.3F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("plate_tasset_mid",
                CubeListBuilder.create().texOffs(104, 97).addBox(-1.1F, 11.9F, -4.1F, 2.2F, 3.2F, 1.2F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("trim_strap_right",
                CubeListBuilder.create().texOffs(76, 97).addBox(1.8F, 11.8F, -4.2F, 1.4F, 3.4F, 1.4F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("plate_tasset_right",
                CubeListBuilder.create().texOffs(86, 97).addBox(2.9F, 11.9F, -4.1F, 3.2F, 3.2F, 1.2F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("plate_pauldron_right",
                CubeListBuilder.create().texOffs(33, 19).addBox(3.95F, -1.05F, -4.05F, 7.1F, 5.1F, 9.1F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("plate_pauldron_spike_c_right",
                CubeListBuilder.create().texOffs(108, 87).addBox(5, -4, -1, 2, 3, 3),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("chain_3_right",
                CubeListBuilder.create().texOffs(91, 104).addBox(5.9F, 7.9F, -5.1F, 1.2F, 2.2F, 1.2F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("plate_pauldron_spike_a_right",
                CubeListBuilder.create().texOffs(25, 87).addBox(5.95F, -5.05F, -2.05F, 2.1F, 4.1F, 4.1F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("chain_4_right",
                CubeListBuilder.create().texOffs(101, 104).addBox(5.95F, 9.95F, -5.05F, 1.1F, 2.1F, 1.1F),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("chain_1_right",
                CubeListBuilder.create().texOffs(71, 104).addBox(6, 4, -5, 1, 2, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("chain_2_right",
                CubeListBuilder.create().texOffs(81, 104).addBox(6, 6, -5, 1, 2, 1),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("plate_pauldron_lip_right",
                CubeListBuilder.create().texOffs(99, 19).addBox(9, 2, -5, 3, 4, 10),
                PartPose.offset(0, 0, 0));
        body.addOrReplaceChild("plate_pauldron_spike_b_right",
                CubeListBuilder.create().texOffs(51, 87).addBox(9, -6, -1, 2, 5, 3),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_helm_back",
                CubeListBuilder.create().texOffs(41, 76).addBox(-5.2F, -8.2F, 3.8F, 10.4F, 8.4F, 1.4F),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_helm_side_left",
                CubeListBuilder.create().texOffs(23, 0).addBox(-5.15F, -8.15F, -5.15F, 1.3F, 8.3F, 10.3F),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_crown_out_b_left",
                CubeListBuilder.create().texOffs(51, 104).addBox(-5.1F, -12.1F, -1.1F, 2.2F, 1.2F, 2.2F),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_crown_out_a_left",
                CubeListBuilder.create().texOffs(47, 97).addBox(-5.05F, -11.05F, -1.05F, 2.1F, 2.1F, 2.1F),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_helm_top",
                CubeListBuilder.create().texOffs(33, 50).addBox(-5, -9, -5, 10, 1, 10),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_cheek_left",
                CubeListBuilder.create().texOffs(82, 87).addBox(-4.25F, -4.25F, -5.25F, 1.5F, 3.5F, 3.5F),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_visor_brow",
                CubeListBuilder.create().texOffs(0, 104).addBox(-4.2F, -6.2F, -5.2F, 8.4F, 2.4F, 1.4F),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_helm_chin",
                CubeListBuilder.create().texOffs(13, 108).addBox(-3.2F, -1.2F, -5.2F, 6.4F, 1.4F, 1.4F),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_crown_in_a_left",
                CubeListBuilder.create().texOffs(29, 97).addBox(-3, -12, -1, 2, 3, 2),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_crown_in_b_left",
                CubeListBuilder.create().texOffs(33, 104).addBox(-3, -13, -1, 2, 1, 2),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_crown_back",
                CubeListBuilder.create().texOffs(56, 97).addBox(-1.1F, -11.1F, 1.9F, 2.2F, 2.2F, 2.2F),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_crown_mid_a",
                CubeListBuilder.create().texOffs(11, 97).addBox(-1.05F, -12.05F, -1.05F, 2.1F, 3.1F, 2.1F),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_crown_mid_b",
                CubeListBuilder.create().texOffs(19, 104).addBox(-0.1F, -14.1F, -0.1F, 1.2F, 2.2F, 1.2F),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_crown_in_a_right",
                CubeListBuilder.create().texOffs(20, 97).addBox(1, -12, -1, 2, 3, 2),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_crown_in_b_right",
                CubeListBuilder.create().texOffs(24, 104).addBox(1, -13, -1, 2, 1, 2),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_cheek_right",
                CubeListBuilder.create().texOffs(73, 87).addBox(2.75F, -4.25F, -5.25F, 1.5F, 3.5F, 3.5F),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_crown_out_b_right",
                CubeListBuilder.create().texOffs(42, 104).addBox(2.9F, -12.1F, -1.1F, 2.2F, 1.2F, 2.2F),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_crown_out_a_right",
                CubeListBuilder.create().texOffs(38, 97).addBox(2.95F, -11.05F, -1.05F, 2.1F, 2.1F, 2.1F),
                PartPose.offset(0, 0, 0));
        head.addOrReplaceChild("plate_helm_side_right",
                CubeListBuilder.create().texOffs(0, 0).addBox(3.85F, -8.15F, -5.15F, 1.3F, 8.3F, 10.3F),
                PartPose.offset(0, 0, 0));
        left_arm.addOrReplaceChild("plate_bracer_right",
                CubeListBuilder.create().texOffs(81, 0).addBox(-2.45F, 1.95F, -4.45F, 6.9F, 6.1F, 8.9F),
                PartPose.offset(0, 0, 0));
        left_arm.addOrReplaceChild("plate_upperarm_right",
                CubeListBuilder.create().texOffs(97, 50).addBox(-2.4F, -1, -3.4F, 6.8F, 4, 6.8F),
                PartPose.offset(0, 0, 0));
        left_leg.addOrReplaceChild("plate_boot_right",
                CubeListBuilder.create().texOffs(29, 64).addBox(-4.4F, 9.9F, -4.5F, 9, 2.2F, 9),
                PartPose.offset(0, 0, 0));
        left_leg.addOrReplaceChild("plate_thigh_right",
                CubeListBuilder.create().texOffs(27, 35).addBox(-3.35F, -0.05F, -3.45F, 6.9F, 7.1F, 6.9F),
                PartPose.offset(0, 0, 0));
        left_leg.addOrReplaceChild("plate_shin_right",
                CubeListBuilder.create().texOffs(85, 35).addBox(-3.3F, 6, -4.4F, 6.8F, 4, 8.8F),
                PartPose.offset(0, 0, 0));
        left_leg.addOrReplaceChild("plate_boot_cap_right",
                CubeListBuilder.create().texOffs(101, 76).addBox(-2.1F, 9.8F, -4.2F, 5.4F, 2.4F, 7.4F),
                PartPose.offset(0, 0, 0));
        left_leg.addOrReplaceChild("plate_knee_right",
                CubeListBuilder.create().texOffs(111, 104).addBox(-2.05F, 4.85F, -4.15F, 5.3F, 2.3F, 1.3F),
                PartPose.offset(0, 0, 0));
        right_arm.addOrReplaceChild("plate_bracer_left",
                CubeListBuilder.create().texOffs(0, 19).addBox(-4.45F, 1.95F, -4.45F, 6.9F, 6.1F, 8.9F),
                PartPose.offset(0, 0, 0));
        right_arm.addOrReplaceChild("plate_upperarm_left",
                CubeListBuilder.create().texOffs(0, 64).addBox(-4.4F, -1, -3.4F, 6.8F, 4, 6.8F),
                PartPose.offset(0, 0, 0));
        right_leg.addOrReplaceChild("plate_boot_left",
                CubeListBuilder.create().texOffs(66, 64).addBox(-4.5F, 10, -4.4F, 8.8F, 2, 8.8F),
                PartPose.offset(0, 0, 0));
        right_leg.addOrReplaceChild("plate_shin_left",
                CubeListBuilder.create().texOffs(0, 50).addBox(-3.55F, 5.95F, -4.45F, 6.9F, 4.1F, 8.9F),
                PartPose.offset(0, 0, 0));
        right_leg.addOrReplaceChild("plate_thigh_left",
                CubeListBuilder.create().texOffs(56, 35).addBox(-3.5F, 0, -3.4F, 6.8F, 7, 6.8F),
                PartPose.offset(0, 0, 0));
        right_leg.addOrReplaceChild("plate_boot_cap_left",
                CubeListBuilder.create().texOffs(0, 87).addBox(-3.25F, 9.85F, -4.15F, 5.3F, 2.3F, 7.3F),
                PartPose.offset(0, 0, 0));
        right_leg.addOrReplaceChild("plate_knee_left",
                CubeListBuilder.create().texOffs(0, 108).addBox(-3.2F, 4.9F, -4.1F, 5.2F, 2.2F, 1.2F),
                PartPose.offset(0, 0, 0));
        return LayerDefinition.create(mesh, 128, 256);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        // 只画装饰件；身体由 layer 贴图负责，戴头盔能看见手
        poseStack.pushPose();
        this.head.translateAndRotate(poseStack);
        if (showHead) {
            this.plateHelmBack.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showHead) {
            this.plateHelmSideLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateCrownOutBLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateCrownOutALeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showHead) {
            this.plateHelmTop.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showHead) {
            this.plateCheekLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateVisorBrow.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showHead) {
            this.plateHelmChin.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateCrownInALeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateCrownInBLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateCrownBack.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateCrownMidA.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateCrownMidB.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateCrownInARight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateCrownInBRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showHead) {
            this.plateCheekRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateCrownOutBRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateCrownOutARight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showHead) {
            this.plateHelmSideRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        poseStack.popPose();
        poseStack.pushPose();
        this.body.translateAndRotate(poseStack);
        if (showChest) {
            this.platePauldronLipLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.platePauldronLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.platePauldronSpikeBLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.platePauldronSpikeALeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.chain3Left.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.chain4Left.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.chain1Left.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.chain2Left.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateCollar.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.plateTassetLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.platePauldronSpikeCLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateBack.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateChest.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.leatherBelt.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.trimStrapLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.trimCoreRing.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.glowCore.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.trimBuckle.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.plateTassetMid.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.trimStrapRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.plateTassetRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.platePauldronRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.platePauldronSpikeCRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.chain3Right.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.platePauldronSpikeARight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.chain4Right.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.chain1Right.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.chain2Right.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.platePauldronLipRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.platePauldronSpikeBRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        poseStack.popPose();
        poseStack.pushPose();
        this.rightArm.translateAndRotate(poseStack);
        if (showChest) {
            this.plateBracerLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateUpperarmLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        poseStack.popPose();
        poseStack.pushPose();
        this.leftArm.translateAndRotate(poseStack);
        if (showChest) {
            this.plateBracerRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showChest) {
            this.plateUpperarmRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        poseStack.popPose();
        poseStack.pushPose();
        this.rightLeg.translateAndRotate(poseStack);
        if (showBoots) {
            this.plateBootLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.plateShinLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.plateThighLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showBoots) {
            this.plateBootCapLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.plateKneeLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        poseStack.popPose();
        poseStack.pushPose();
        this.leftLeg.translateAndRotate(poseStack);
        if (showBoots) {
            this.plateBootRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.plateThighRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.plateShinRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showBoots) {
            this.plateBootCapRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        if (showLegs) {
            this.plateKneeRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        poseStack.popPose();
    }
}
