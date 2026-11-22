package com.suwako.northstarplanetaddon.mercury.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * 由 tools/gen-entity-model.mjs 从 tools/entity-specs/blazing_sunfish.bbmodel 生成（127 cubes / 37 groups）。
 * 重新生成：node tools/gen-entity-model.mjs tools/entity-specs/blazing_sunfish.bbmodel src/main/java/com/suwako/northstarplanetaddon/mercury/client/model/BlazingSunfishMesh.java com.suwako.northstarplanetaddon.mercury.client.model BlazingSunfishMesh
 */
final class BlazingSunfishMesh {
    private BlazingSunfishMesh() {
    }

    static final String[] PART_NAMES = {"body", "neck1", "neck2", "head", "jaw", "horn_l", "horn_l2", "horn_l3",
            "horn_r", "horn_r2", "horn_r3", "barbel_l", "barbel_r", "crest", "spine1", "spine2", "fin_l",
            "fin_l_tip", "fin_r", "fin_r_tip", "belly", "tail1", "tail2", "tail3", "tail_fin", "streamer_a",
            "streamer_b", "streamer_c", "streamer_d", "spine3", "spine4", "spine5", "spine6", "spine7", "ventral_l",
            "ventral_r", "core"};

    static void build(MeshDefinition mesh) {
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                // b2_mid
                .texOffs(86, 0).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 4.0F, 3.0F)
                // b2_top
                .texOffs(41, 37).addBox(-2.0F, 3.0F, -3.0F, 4.0F, 1.0F, 3.0F)
                // b2_bot
                .texOffs(56, 37).addBox(-2.05F, -2.05F, -3.05F, 4.1F, 1.1F, 3.1F)
                // b3_mid
                .texOffs(105, 0).addBox(-4.0F, -2.0F, -1.0F, 8.0F, 4.0F, 3.0F)
                // b3_top
                .texOffs(71, 37).addBox(-3.1F, 1.9F, -1.1F, 6.2F, 1.2F, 3.2F)
                // b3_bot
                .texOffs(90, 37).addBox(-3.25F, -3.25F, -1.25F, 6.5F, 1.5F, 3.5F)
                // b4_mid
                .texOffs(0, 10).addBox(-3.2F, -3.2F, 0.8F, 6.4F, 4.4F, 3.4F)
                // b4_top
                .texOffs(109, 37).addBox(-2.25F, 0.75F, 0.75F, 4.5F, 1.5F, 3.5F)
                // b4_bot
                .texOffs(0, 42).addBox(-2.25F, -4.25F, 0.75F, 4.5F, 1.5F, 3.5F)
                // b5_mid
                .texOffs(19, 10).addBox(-3.0F, -4.0F, 3.0F, 6.0F, 4.0F, 3.0F)
                // b5_top
                .texOffs(15, 42).addBox(-2.0F, 0.0F, 3.0F, 4.0F, 1.0F, 3.0F)
                // b5_bot
                .texOffs(30, 42).addBox(-2.0F, -5.0F, 3.0F, 4.0F, 1.0F, 3.0F)
                // b6_mid
                .texOffs(28, 18).addBox(-2.1F, -3.1F, 4.9F, 4.2F, 2.2F, 3.2F)
                // b6_top
                .texOffs(45, 42).addBox(-1.3F, -1.3F, 4.7F, 2.6F, 1.6F, 3.6F)
                // b6_bot
                .texOffs(56, 42).addBox(-1.15F, -4.15F, 4.85F, 2.3F, 1.3F, 3.3F)
                // mane3_l
                .texOffs(65, 52).addBox(-5.0F, -2.0F, -1.0F, 1.0F, 2.0F, 2.0F)
                // mane4_l
                .texOffs(72, 52).addBox(-4.15F, -0.15F, 0.85F, 1.3F, 1.8F, 2.3F)
                // mane3_r
                .texOffs(93, 52).addBox(4.0F, -2.0F, -1.0F, 1.0F, 2.0F, 2.0F)
                // mane4_r
                .texOffs(100, 52).addBox(2.85F, -0.15F, 0.85F, 1.3F, 1.8F, 2.3F)
                // plate_chest
                .texOffs(90, 57).addBox(-3.55F, -0.05F, -0.05F, 7.1F, 2.1F, 1.7F)
                // plate_belly
                .texOffs(109, 57).addBox(-3.1F, -3.6F, 0.4F, 6.2F, 2.2F, 1.7F)
                // plate_tail
                .texOffs(0, 62).addBox(-2.05F, -4.05F, 4.45F, 4.1F, 1.6F, 1.6F)
                , PartPose.offset(0.0F, 12.0F, 2.0F));
        PartDefinition neck1 = body.addOrReplaceChild("neck1", CubeListBuilder.create()
                // b1_mid
                .texOffs(67, 0).addBox(-3.1F, -2.1F, -1.1F, 6.2F, 4.2F, 3.2F)
                // b1_top
                .texOffs(11, 37).addBox(-2.0F, 2.0F, -1.0F, 4.0F, 1.0F, 3.0F)
                // b1_bot
                .texOffs(26, 37).addBox(-2.15F, -3.15F, -1.15F, 4.3F, 1.3F, 3.3F)
                // mane1_l
                .texOffs(51, 52).addBox(-4.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F)
                // mane2_l
                .texOffs(58, 52).addBox(-5.55F, -2.05F, 0.95F, 1.1F, 2.1F, 2.1F)
                // mane1_r
                .texOffs(79, 52).addBox(3.5F, 0.0F, -1.0F, 1.0F, 2.0F, 2.0F)
                // mane2_r
                .texOffs(86, 52).addBox(4.45F, -2.05F, 0.95F, 1.1F, 2.1F, 2.1F)
                , PartPose.offset(0.0F, 2.0F, -4.0F));
        PartDefinition neck2 = neck1.addOrReplaceChild("neck2", CubeListBuilder.create()
                // b0_mid
                .texOffs(13, 18).addBox(-2.15F, -2.15F, -0.65F, 4.3F, 2.3F, 3.3F)
                // b0_top
                .texOffs(109, 31).addBox(-1.2F, -0.2F, -0.7F, 2.4F, 1.4F, 3.4F)
                // b0_bot
                .texOffs(0, 37).addBox(-1.05F, -3.05F, -0.55F, 2.1F, 1.1F, 3.1F)
                , PartPose.offset(0.0F, 2.0F, -2.5F));
        PartDefinition head = neck2.addOrReplaceChild("head", CubeListBuilder.create()
                // skull
                .texOffs(25, 0).addBox(-3.5F, -1.5F, -2.0F, 7.0F, 4.0F, 4.0F)
                // skull_top
                .texOffs(91, 18).addBox(-3.0F, 2.5F, -2.0F, 6.0F, 1.0F, 4.0F)
                // skull_back
                .texOffs(112, 18).addBox(-2.5F, -2.5F, 2.0F, 5.0F, 4.0F, 1.0F)
                // brow_l
                .texOffs(108, 42).addBox(-3.55F, 1.45F, -3.05F, 2.1F, 1.1F, 2.6F)
                // brow_r
                .texOffs(0, 47).addBox(1.45F, 1.45F, -3.05F, 2.1F, 1.1F, 2.6F)
                // snout_up
                .texOffs(89, 10).addBox(-2.5F, -0.5F, -6.0F, 5.0F, 2.0F, 4.0F)
                // beak
                .texOffs(11, 47).addBox(-2.0F, -1.0F, -8.0F, 4.0F, 2.0F, 2.0F)
                // cheek_spike_l
                .texOffs(24, 47).addBox(-5.0F, -1.0F, -1.5F, 1.5F, 1.5F, 1.5F)
                // cheek_spike_r
                .texOffs(33, 47).addBox(3.5F, -1.0F, -1.5F, 1.5F, 1.5F, 1.5F)
                // cheek_fin_l
                .texOffs(42, 47).addBox(-5.55F, -2.05F, 0.45F, 1.6F, 1.4F, 1.6F)
                // cheek_fin_r
                .texOffs(51, 47).addBox(3.95F, -2.05F, 0.45F, 1.6F, 1.4F, 1.6F)
                // throat
                .texOffs(68, 10).addBox(-2.0F, -2.1F, -6.6F, 4.0F, 0.8F, 6.0F)
                , PartPose.offset(0.0F, 1.5F, -1.5F));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                // jaw_tip
                .texOffs(67, 47).addBox(-1.5F, -2.5F, -7.5F, 3.0F, 1.5F, 1.5F)
                // jaw_main
                .texOffs(0, 0).addBox(-2.5F, -2.5F, -6.0F, 5.0F, 2.0F, 6.5F)
                , PartPose.offset(0.0F, -2.0F, -1.0F));
        PartDefinition horn_l = head.addOrReplaceChild("horn_l", CubeListBuilder.create()
                // horn_l_a
                .texOffs(9, 52).addBox(-0.6F, -0.1F, -0.1F, 1.2F, 3.2F, 1.2F)
                , PartPose.offset(-3.0F, 2.5F, -1.0F));
        PartDefinition horn_l2 = horn_l.addOrReplaceChild("horn_l2", CubeListBuilder.create()
                // horn_l_b
                .texOffs(14, 52).addBox(-0.4F, -0.7F, -1.8F, 1.2F, 1.7F, 1.0F)
                , PartPose.offset(-1.0F, 1.5F, 0.5F));
        PartDefinition horn_l3 = horn_l2.addOrReplaceChild("horn_l3", CubeListBuilder.create()
                // horn_l_c
                .texOffs(23, 52).addBox(-0.1F, -0.3F, -1.4F, 1.0F, 1.2F, 1.2F)
                , PartPose.offset(-1.0F, 1.0F, 0.5F));
        PartDefinition horn_r = head.addOrReplaceChild("horn_r", CubeListBuilder.create()
                // horn_r_a
                .texOffs(30, 52).addBox(-0.6F, -0.1F, -0.1F, 1.2F, 3.2F, 1.2F)
                , PartPose.offset(3.0F, 2.5F, -1.0F));
        PartDefinition horn_r2 = horn_r.addOrReplaceChild("horn_r2", CubeListBuilder.create()
                // horn_r_b
                .texOffs(35, 52).addBox(-0.8F, -0.7F, -1.8F, 1.2F, 1.7F, 1.0F)
                , PartPose.offset(1.0F, 1.5F, 0.5F));
        PartDefinition horn_r3 = horn_r2.addOrReplaceChild("horn_r3", CubeListBuilder.create()
                // horn_r_c
                .texOffs(44, 52).addBox(-0.9F, -0.3F, -1.4F, 1.0F, 1.2F, 1.2F)
                , PartPose.offset(1.0F, 1.0F, 0.5F));
        PartDefinition barbel_l = head.addOrReplaceChild("barbel_l", CubeListBuilder.create()
                // barbel_l_a
                .texOffs(78, 47).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 1.0F, 3.0F)
                // barbel_l_b
                .texOffs(87, 47).addBox(-1.05F, -0.55F, 1.95F, 1.1F, 1.1F, 3.1F)
                // barbel_l_c
                .texOffs(96, 47).addBox(-1.5F, -1.0F, 5.0F, 1.0F, 1.0F, 3.0F)
                , PartPose.offset(-3.5F, -0.5F, -5.0F));
        PartDefinition barbel_r = head.addOrReplaceChild("barbel_r", CubeListBuilder.create()
                // barbel_r_a
                .texOffs(105, 47).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 1.0F, 3.0F)
                // barbel_r_b
                .texOffs(114, 47).addBox(-0.05F, -0.55F, 1.95F, 1.1F, 1.1F, 3.1F)
                // barbel_r_c
                .texOffs(0, 52).addBox(0.5F, -1.0F, 5.0F, 1.0F, 1.0F, 3.0F)
                , PartPose.offset(3.5F, -0.5F, -5.0F));
        PartDefinition crest = head.addOrReplaceChild("crest", CubeListBuilder.create()
                // crest_c1
                .texOffs(60, 47).addBox(-1.0F, -0.5F, -1.0F, 2.0F, 2.5F, 1.0F)
                // crest_c2
                .texOffs(46, 62).addBox(-1.05F, -1.05F, 0.95F, 2.1F, 2.1F, 1.1F)
                // crest_c3
                .texOffs(53, 62).addBox(-0.75F, 1.5F, -0.5F, 1.5F, 2.0F, 1.0F)
                , PartPose.offset(0.0F, 4.0F, 0.0F));
        PartDefinition spine1 = neck2.addOrReplaceChild("spine1", CubeListBuilder.create()
                // dspine1_base
                .texOffs(107, 52).addBox(-0.55F, -1.05F, -0.05F, 1.1F, 3.1F, 1.1F)
                // dspine1_tip
                .texOffs(112, 52).addBox(-0.6F, 1.4F, 0.9F, 1.2F, 2.7F, 1.2F)
                , PartPose.offset(0.0F, 1.0F, -0.5F));
        PartDefinition spine2 = neck1.addOrReplaceChild("spine2", CubeListBuilder.create()
                // dspine2_base
                .texOffs(117, 52).addBox(-0.55F, -1.05F, -0.05F, 1.1F, 3.1F, 1.1F)
                // dspine2_tip
                .texOffs(122, 52).addBox(-0.5F, 1.5F, 1.0F, 1.0F, 2.5F, 1.0F)
                , PartPose.offset(0.0F, 3.0F, -1.0F));
        PartDefinition fin_l = body.addOrReplaceChild("fin_l", CubeListBuilder.create()
                // fin_l_web
                .texOffs(53, 10).addBox(-2.05F, -4.05F, -0.05F, 2.6F, 3.1F, 3.6F)
                // fin_l_a1
                .texOffs(109, 25).addBox(-2.5F, 0.0F, -0.5F, 3.0F, 2.5F, 2.0F)
                , PartPose.offset(-3.5F, 0.0F, -1.5F));
        PartDefinition fin_l_tip = fin_l.addOrReplaceChild("fin_l_tip", CubeListBuilder.create()
                // fin_l_a2
                .texOffs(0, 31).addBox(-2.5F, -0.5F, -1.5F, 2.5F, 2.5F, 2.0F)
                // fin_l_a3
                .texOffs(11, 31).addBox(-4.0F, -2.5F, -1.0F, 1.5F, 2.5F, 2.0F)
                // fin_l_a4
                .texOffs(20, 31).addBox(-5.0F, -4.5F, -0.5F, 1.0F, 2.5F, 2.0F)
                // fin_l_b1
                .texOffs(27, 31).addBox(0.4F, 0.4F, -0.1F, 2.7F, 2.7F, 2.2F)
                // fin_l_b2
                .texOffs(38, 31).addBox(-1.5F, -1.5F, 0.5F, 2.0F, 2.5F, 2.0F)
                // fin_l_b3
                .texOffs(47, 31).addBox(-3.0F, -3.5F, 1.0F, 1.5F, 2.5F, 2.0F)
                // fin_l_c1
                .texOffs(56, 31).addBox(1.5F, -0.5F, 2.0F, 1.5F, 2.5F, 2.0F)
                // fin_l_c2
                .texOffs(65, 31).addBox(0.0F, -2.5F, 2.5F, 1.5F, 2.5F, 2.0F)
                , PartPose.offset(-2.5F, -1.5F, 1.5F));
        PartDefinition fin_r = body.addOrReplaceChild("fin_r", CubeListBuilder.create()
                // fin_r_web
                .texOffs(38, 10).addBox(-0.55F, -4.05F, -0.05F, 2.6F, 3.1F, 3.6F)
                // fin_r_a1
                .texOffs(24, 25).addBox(-0.5F, 0.0F, -0.5F, 3.0F, 2.5F, 2.0F)
                , PartPose.offset(3.5F, 0.0F, -1.5F));
        PartDefinition fin_r_tip = fin_r.addOrReplaceChild("fin_r_tip", CubeListBuilder.create()
                // fin_r_a2
                .texOffs(35, 25).addBox(0.0F, -0.5F, -1.5F, 2.5F, 2.5F, 2.0F)
                // fin_r_a3
                .texOffs(46, 25).addBox(2.5F, -2.5F, -1.0F, 1.5F, 2.5F, 2.0F)
                // fin_r_a4
                .texOffs(55, 25).addBox(4.0F, -4.5F, -0.5F, 1.0F, 2.5F, 2.0F)
                // fin_r_b1
                .texOffs(62, 25).addBox(-3.1F, 0.4F, -0.1F, 2.7F, 2.7F, 2.2F)
                // fin_r_b2
                .texOffs(73, 25).addBox(-0.5F, -1.5F, 0.5F, 2.0F, 2.5F, 2.0F)
                // fin_r_b3
                .texOffs(82, 25).addBox(1.5F, -3.5F, 1.0F, 1.5F, 2.5F, 2.0F)
                // fin_r_c1
                .texOffs(91, 25).addBox(-3.0F, -0.5F, 2.0F, 1.5F, 2.5F, 2.0F)
                // fin_r_c2
                .texOffs(100, 25).addBox(-1.5F, -2.5F, 2.5F, 1.5F, 2.5F, 2.0F)
                , PartPose.offset(2.5F, -1.5F, 1.5F));
        PartDefinition belly = body.addOrReplaceChild("belly", CubeListBuilder.create()
                // belly1
                .texOffs(89, 42).addBox(-3.0F, -4.0F, -1.0F, 6.0F, 1.0F, 3.0F)
                // belly2
                .texOffs(33, 62).addBox(-2.0F, -3.0F, 2.0F, 4.0F, 1.0F, 2.0F)
                , PartPose.offset(0.0F, -2.0F, 4.0F));
        PartDefinition tail1 = belly.addOrReplaceChild("tail1", CubeListBuilder.create()
                // b7_mid
                .texOffs(43, 18).addBox(-2.0F, -1.0F, -0.5F, 4.0F, 2.0F, 3.0F)
                // b7_top
                .texOffs(67, 42).addBox(-1.25F, 0.75F, -0.75F, 2.5F, 1.5F, 3.5F)
                // b7_bot
                .texOffs(78, 42).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 1.0F, 3.0F)
                // dspine8_base
                .texOffs(50, 57).addBox(-0.5F, 1.0F, -0.5F, 1.0F, 3.0F, 1.0F)
                // dspine8_tip
                .texOffs(55, 57).addBox(-0.5F, 3.5F, 0.5F, 1.0F, 2.5F, 1.0F)
                // dspine9_base
                .texOffs(60, 57).addBox(-0.5F, 1.0F, 1.5F, 1.0F, 3.0F, 1.0F)
                // dspine9_tip
                .texOffs(65, 57).addBox(-0.5F, 3.5F, 2.5F, 1.0F, 2.5F, 1.0F)
                , PartPose.offset(0.0F, 0.0F, 3.5F));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail2", CubeListBuilder.create()
                // b8
                .texOffs(58, 18).addBox(-1.2F, -0.7F, -1.2F, 2.4F, 2.4F, 3.4F)
                // dspine10_base
                .texOffs(70, 57).addBox(-0.5F, 1.5F, 1.0F, 1.0F, 3.0F, 1.0F)
                // dspine10_tip
                .texOffs(75, 57).addBox(-0.5F, 4.0F, 2.0F, 1.0F, 2.5F, 1.0F)
                , PartPose.offset(0.0F, 0.5F, 2.5F));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create()
                // b9
                .texOffs(69, 18).addBox(-1.15F, -1.15F, -1.65F, 2.3F, 2.3F, 3.3F)
                // b10
                .texOffs(80, 18).addBox(-1.1F, -0.1F, 0.4F, 2.2F, 2.2F, 3.2F)
                // dspine11_base
                .texOffs(80, 57).addBox(-0.5F, 1.0F, 0.5F, 1.0F, 3.0F, 1.0F)
                // dspine11_tip
                .texOffs(85, 57).addBox(-0.5F, 3.5F, 1.5F, 1.0F, 2.5F, 1.0F)
                , PartPose.offset(0.0F, 1.5F, 2.5F));
        PartDefinition tail_fin = tail3.addOrReplaceChild("tail_fin", CubeListBuilder.create()
                // tailfin_web
                .texOffs(108, 10).addBox(-3.0F, -2.0F, -0.5F, 6.0F, 4.0F, 2.0F)
                // tailblade_a
                .texOffs(0, 18).addBox(-2.0F, 1.0F, 1.5F, 4.0F, 4.0F, 2.0F)
                // tailblade_b
                .texOffs(74, 31).addBox(-1.0F, 4.0F, 2.5F, 2.0F, 3.0F, 2.0F)
                , PartPose.offset(0.0F, 1.0F, 2.0F));
        PartDefinition streamer_a = tail_fin.addOrReplaceChild("streamer_a", CubeListBuilder.create()
                // str_a1
                .texOffs(83, 31).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 1.0F, 4.0F)
                , PartPose.offset(0.0F, 1.0F, 1.0F));
        PartDefinition streamer_b = tail_fin.addOrReplaceChild("streamer_b", CubeListBuilder.create()
                // str_b1
                .texOffs(96, 31).addBox(-1.0F, -2.5F, -1.0F, 2.0F, 1.0F, 4.0F)
                , PartPose.offset(0.0F, -0.5F, 1.5F));
        PartDefinition streamer_c = tail_fin.addOrReplaceChild("streamer_c", CubeListBuilder.create()
                , PartPose.offset(0.0F, -2.5F, 2.0F));
        PartDefinition streamer_d = tail_fin.addOrReplaceChild("streamer_d", CubeListBuilder.create()
                , PartPose.offset(0.0F, -4.5F, 1.0F));
        PartDefinition spine3 = body.addOrReplaceChild("spine3", CubeListBuilder.create()
                // dspine3_base
                .texOffs(0, 57).addBox(-0.55F, -1.05F, -0.05F, 1.1F, 3.1F, 1.1F)
                // dspine3_tip
                .texOffs(5, 57).addBox(-0.5F, 1.5F, 1.0F, 1.0F, 2.5F, 1.0F)
                , PartPose.offset(0.0F, 4.0F, -3.0F));
        PartDefinition spine4 = body.addOrReplaceChild("spine4", CubeListBuilder.create()
                // dspine4_base
                .texOffs(10, 57).addBox(-0.55F, -1.05F, -0.05F, 1.1F, 3.1F, 1.1F)
                // dspine4_tip
                .texOffs(15, 57).addBox(-0.5F, 1.5F, 1.0F, 1.0F, 2.5F, 1.0F)
                , PartPose.offset(0.0F, 3.0F, -1.0F));
        PartDefinition spine5 = body.addOrReplaceChild("spine5", CubeListBuilder.create()
                // dspine5_base
                .texOffs(20, 57).addBox(-0.55F, -1.05F, -0.05F, 1.1F, 3.1F, 1.1F)
                // dspine5_tip
                .texOffs(25, 57).addBox(-0.5F, 1.5F, 1.0F, 1.0F, 2.5F, 1.0F)
                , PartPose.offset(0.0F, 2.0F, 1.0F));
        PartDefinition spine6 = body.addOrReplaceChild("spine6", CubeListBuilder.create()
                // dspine6_base
                .texOffs(30, 57).addBox(-0.55F, -1.05F, -0.05F, 1.1F, 3.1F, 1.1F)
                // dspine6_tip
                .texOffs(35, 57).addBox(-0.5F, 1.5F, 1.0F, 1.0F, 2.5F, 1.0F)
                , PartPose.offset(0.0F, 1.0F, 3.0F));
        PartDefinition spine7 = body.addOrReplaceChild("spine7", CubeListBuilder.create()
                // dspine7_base
                .texOffs(40, 57).addBox(-0.55F, -1.05F, -0.05F, 1.1F, 3.1F, 1.1F)
                // dspine7_tip
                .texOffs(45, 57).addBox(-0.5F, 1.5F, 1.0F, 1.0F, 2.5F, 1.0F)
                , PartPose.offset(0.0F, 0.0F, 5.0F));
        PartDefinition ventral_l = body.addOrReplaceChild("ventral_l", CubeListBuilder.create()
                // ventral_l_a
                .texOffs(23, 62).addBox(-0.05F, -1.05F, -1.05F, 1.1F, 2.1F, 1.1F)
                // ventral_l_b
                .texOffs(28, 62).addBox(-1.1F, -2.1F, -0.1F, 1.2F, 2.2F, 1.2F)
                // ventral_l_a
                .texOffs(70, 62).addBox(0.0F, -1.0F, -1.0F, 1.0F, 2.0F, 1.0F)
                // ventral_l_b
                .texOffs(75, 62).addBox(-1.05F, -2.05F, -0.05F, 1.1F, 2.1F, 1.1F)
                , PartPose.offset(-2.0F, -5.0F, 3.0F));
        PartDefinition ventral_r = body.addOrReplaceChild("ventral_r", CubeListBuilder.create()
                // ventral_r_a
                .texOffs(13, 62).addBox(-1.05F, -1.05F, -1.05F, 1.1F, 2.1F, 1.1F)
                // ventral_r_b
                .texOffs(18, 62).addBox(-0.1F, -2.1F, -0.1F, 1.2F, 2.2F, 1.2F)
                // ventral_r_a
                .texOffs(60, 62).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 2.0F, 1.0F)
                // ventral_r_b
                .texOffs(65, 62).addBox(-0.05F, -2.05F, -0.05F, 1.1F, 2.1F, 1.1F)
                , PartPose.offset(2.0F, -5.0F, 3.0F));
        PartDefinition core = body.addOrReplaceChild("core", CubeListBuilder.create()
                // core_ring
                .texOffs(48, 0).addBox(-2.5F, -2.0F, -2.9F, 5.0F, 3.5F, 3.2F)
                // core_inner
                .texOffs(0, 25).addBox(-1.5F, -1.3F, -3.1F, 3.0F, 2.0F, 2.2F)
                // core_glow
                .texOffs(13, 25).addBox(-1.1F, -1.0F, -3.35F, 2.2F, 1.4F, 2.35F)
                , PartPose.offset(0.0F, 0.5F, -4.5F));
    }
}
