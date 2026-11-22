package com.suwako.northstarplanetaddon.mercury.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * 由 tools/gen-entity-model.mjs 从 tools/entity-specs/sunfish.bbmodel 生成（115 cubes / 33 groups）。
 * 重新生成：node tools/gen-entity-model.mjs tools/entity-specs/sunfish.bbmodel src/main/java/com/suwako/northstarplanetaddon/mercury/client/model/SunfishMesh.java com.suwako.northstarplanetaddon.mercury.client.model SunfishMesh
 */
final class SunfishMesh {
    private SunfishMesh() {
    }

    static final String[] PART_NAMES = {"body", "neck1", "neck2", "head", "jaw", "horn_l", "horn_l2", "horn_l3",
            "horn_r", "horn_r2", "horn_r3", "spine1", "spine2", "fin_l", "fin_l_tip", "fin_r", "fin_r_tip", "belly",
            "tail1", "tail2", "tail3", "tail_fin", "streamer_a", "streamer_b", "streamer_c", "streamer_d", "spine3",
            "spine4", "spine5", "spine6", "spine7", "ventral_l", "ventral_r"};

    static void build(MeshDefinition mesh) {
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                // b2_mid
                .texOffs(67, 0).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 4.0F, 3.0F)
                // b2_top
                .texOffs(0, 24).addBox(-2.0F, 3.0F, -3.0F, 4.0F, 1.0F, 3.0F)
                // b2_bot
                .texOffs(15, 24).addBox(-2.05F, -2.05F, -3.05F, 4.1F, 1.1F, 3.1F)
                // b3_mid
                .texOffs(86, 0).addBox(-4.0F, -2.0F, -1.0F, 8.0F, 4.0F, 3.0F)
                // b3_top
                .texOffs(30, 24).addBox(-3.05F, 1.95F, -1.05F, 6.1F, 1.1F, 3.1F)
                // b3_bot
                .texOffs(49, 24).addBox(-3.15F, -3.15F, -1.15F, 6.3F, 1.3F, 3.3F)
                // b4_mid
                .texOffs(109, 0).addBox(-3.1F, -3.1F, 0.9F, 6.2F, 4.2F, 3.2F)
                // b4_top
                .texOffs(68, 24).addBox(-2.15F, 0.85F, 0.85F, 4.3F, 1.3F, 3.3F)
                // b4_bot
                .texOffs(83, 24).addBox(-2.15F, -4.15F, 0.85F, 4.3F, 1.3F, 3.3F)
                // b5_mid
                .texOffs(0, 10).addBox(-3.0F, -4.0F, 3.0F, 6.0F, 4.0F, 3.0F)
                // b5_top
                .texOffs(98, 24).addBox(-2.0F, 0.0F, 3.0F, 4.0F, 1.0F, 3.0F)
                // b5_bot
                .texOffs(113, 24).addBox(-2.0F, -5.0F, 3.0F, 4.0F, 1.0F, 3.0F)
                // b6_mid
                .texOffs(70, 10).addBox(-2.05F, -3.05F, 4.95F, 4.1F, 2.1F, 3.1F)
                // b6_top
                .texOffs(0, 29).addBox(-1.3F, -1.3F, 4.7F, 2.6F, 1.6F, 3.6F)
                // b6_bot
                .texOffs(11, 29).addBox(-1.1F, -4.1F, 4.9F, 2.2F, 1.2F, 3.2F)
                // mane3_l
                .texOffs(48, 48).addBox(-5.0F, -1.0F, -1.0F, 1.0F, 1.0F, 2.0F)
                // mane3_r
                .texOffs(55, 48).addBox(4.0F, -1.0F, -1.0F, 1.0F, 1.0F, 2.0F)
                // mane4_l
                .texOffs(62, 48).addBox(-4.05F, -0.05F, 0.95F, 1.1F, 1.1F, 2.1F)
                // mane4_r
                .texOffs(69, 48).addBox(2.95F, -0.05F, 0.95F, 1.1F, 1.1F, 2.1F)
                , PartPose.offset(0.0F, 12.0F, 2.0F));
        PartDefinition neck1 = body.addOrReplaceChild("neck1", CubeListBuilder.create()
                // b1_mid
                .texOffs(48, 0).addBox(-3.05F, -2.05F, -1.05F, 6.1F, 4.1F, 3.1F)
                // b1_top
                .texOffs(87, 18).addBox(-2.0F, 2.0F, -1.0F, 4.0F, 1.0F, 3.0F)
                // b1_bot
                .texOffs(102, 18).addBox(-2.1F, -3.1F, -1.1F, 4.2F, 1.2F, 3.2F)
                // mane1_l
                .texOffs(20, 48).addBox(-4.0F, 1.0F, -1.0F, 1.0F, 1.0F, 2.0F)
                // mane1_r
                .texOffs(27, 48).addBox(3.0F, 1.0F, -1.0F, 1.0F, 1.0F, 2.0F)
                // mane2_l
                .texOffs(34, 48).addBox(-5.0F, -1.0F, 1.0F, 1.0F, 1.0F, 2.0F)
                // mane2_r
                .texOffs(41, 48).addBox(4.0F, -1.0F, 1.0F, 1.0F, 1.0F, 2.0F)
                , PartPose.offset(0.0F, 2.0F, -4.0F));
        PartDefinition neck2 = neck1.addOrReplaceChild("neck2", CubeListBuilder.create()
                // b0_mid
                .texOffs(55, 10).addBox(-2.1F, -2.1F, -0.6F, 4.2F, 2.2F, 3.2F)
                // b0_top
                .texOffs(65, 18).addBox(-1.15F, -0.15F, -0.65F, 2.3F, 1.3F, 3.3F)
                // b0_bot
                .texOffs(76, 18).addBox(-1.0F, -3.0F, -0.5F, 2.0F, 1.0F, 3.0F)
                , PartPose.offset(0.0F, 2.0F, -2.5F));
        PartDefinition head = neck2.addOrReplaceChild("head", CubeListBuilder.create()
                // skull
                .texOffs(25, 0).addBox(-3.5F, -1.5F, -2.0F, 7.0F, 4.0F, 4.0F)
                // skull_top
                .texOffs(11, 18).addBox(-2.5F, 2.5F, -2.0F, 5.0F, 1.0F, 4.0F)
                // skull_back
                .texOffs(30, 18).addBox(-2.5F, -2.5F, 2.0F, 5.0F, 4.0F, 1.0F)
                // brow_l
                .texOffs(44, 29).addBox(-3.55F, 1.45F, -2.55F, 2.1F, 1.1F, 2.6F)
                // brow_r
                .texOffs(55, 29).addBox(1.45F, 1.45F, -2.55F, 2.1F, 1.1F, 2.6F)
                // snout_up
                .texOffs(19, 10).addBox(-2.5F, -0.5F, -6.0F, 5.0F, 2.0F, 4.0F)
                // snout_mid
                .texOffs(66, 39).addBox(-2.0F, -0.5F, -8.0F, 4.0F, 1.0F, 2.0F)
                // snout_tip
                .texOffs(76, 48).addBox(-1.5F, -0.5F, -9.0F, 3.0F, 1.0F, 1.0F)
                // nostril_l
                .texOffs(85, 48).addBox(-2.5F, 1.5F, -6.0F, 1.0F, 1.0F, 1.0F)
                // nostril_r
                .texOffs(90, 48).addBox(1.5F, 1.5F, -6.0F, 1.0F, 1.0F, 1.0F)
                // cheek_l
                .texOffs(66, 29).addBox(-4.5F, -1.5F, -1.0F, 1.0F, 2.0F, 2.0F)
                // cheek_r
                .texOffs(73, 29).addBox(3.5F, -1.5F, -1.0F, 1.0F, 2.0F, 2.0F)
                // gill_l
                .texOffs(80, 29).addBox(-4.5F, -3.5F, 1.0F, 1.0F, 3.0F, 1.0F)
                // gill_r
                .texOffs(85, 29).addBox(3.5F, -3.5F, 1.0F, 1.0F, 3.0F, 1.0F)
                // crest_h1
                .texOffs(95, 48).addBox(-1.0F, 3.5F, -1.0F, 2.0F, 1.0F, 1.0F)
                // crest_h2
                .texOffs(102, 48).addBox(-1.0F, 3.5F, 1.0F, 2.0F, 1.0F, 1.0F)
                , PartPose.offset(0.0F, 1.5F, -1.5F));
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create()
                // jaw_tip
                .texOffs(79, 39).addBox(-1.5F, -0.5F, -8.0F, 3.0F, 1.0F, 2.0F)
                // jaw_main
                .texOffs(0, 0).addBox(-2.5F, -0.5F, -6.0F, 5.0F, 2.0F, 7.0F)
                , PartPose.offset(0.0F, -2.0F, -1.0F));
        PartDefinition horn_l = head.addOrReplaceChild("horn_l", CubeListBuilder.create()
                // horn_l_a
                .texOffs(90, 39).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F)
                , PartPose.offset(-3.0F, 2.5F, -1.0F));
        PartDefinition horn_l2 = horn_l.addOrReplaceChild("horn_l2", CubeListBuilder.create()
                // horn_l_b
                .texOffs(95, 39).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 2.0F, 1.0F)
                // horn_l_d
                .texOffs(109, 48).addBox(-1.5F, 0.5F, -0.5F, 1.0F, 1.0F, 1.0F)
                , PartPose.offset(-1.0F, 1.5F, 0.5F));
        PartDefinition horn_l3 = horn_l2.addOrReplaceChild("horn_l3", CubeListBuilder.create()
                // horn_l_c
                .texOffs(100, 39).addBox(0.5F, 0.5F, 0.0F, 1.0F, 2.0F, 1.0F)
                , PartPose.offset(-1.0F, 1.0F, 0.5F));
        PartDefinition horn_r = head.addOrReplaceChild("horn_r", CubeListBuilder.create()
                // horn_r_a
                .texOffs(105, 39).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F)
                , PartPose.offset(3.0F, 2.5F, -1.0F));
        PartDefinition horn_r2 = horn_r.addOrReplaceChild("horn_r2", CubeListBuilder.create()
                // horn_r_b
                .texOffs(110, 39).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 2.0F, 1.0F)
                // horn_r_d
                .texOffs(114, 48).addBox(0.5F, 0.5F, -0.5F, 1.0F, 1.0F, 1.0F)
                , PartPose.offset(1.0F, 1.5F, 0.5F));
        PartDefinition horn_r3 = horn_r2.addOrReplaceChild("horn_r3", CubeListBuilder.create()
                // horn_r_c
                .texOffs(115, 39).addBox(-1.5F, 0.5F, 0.0F, 1.0F, 2.0F, 1.0F)
                , PartPose.offset(1.0F, 1.0F, 0.5F));
        PartDefinition spine1 = neck2.addOrReplaceChild("spine1", CubeListBuilder.create()
                // dspine1_base
                .texOffs(120, 39).addBox(-0.7F, -1.2F, -0.2F, 1.4F, 2.4F, 1.4F)
                // dspine1_tip
                .texOffs(0, 44).addBox(-0.55F, -0.05F, 0.95F, 1.1F, 2.1F, 1.1F)
                , PartPose.offset(0.0F, 1.0F, -0.5F));
        PartDefinition spine2 = neck1.addOrReplaceChild("spine2", CubeListBuilder.create()
                // dspine2_base
                .texOffs(5, 44).addBox(-0.7F, -1.2F, -0.2F, 1.4F, 2.4F, 1.4F)
                // dspine2_tip
                .texOffs(10, 44).addBox(-0.5F, 0.0F, 1.0F, 1.0F, 2.0F, 1.0F)
                , PartPose.offset(0.0F, 3.0F, -1.0F));
        PartDefinition fin_l = body.addOrReplaceChild("fin_l", CubeListBuilder.create()
                // fin_l_web
                .texOffs(54, 18).addBox(-1.7F, -3.2F, 0.3F, 2.4F, 2.4F, 3.4F)
                // fin_l_a1
                .texOffs(23, 34).addBox(-2.6F, -0.1F, -0.6F, 3.2F, 2.2F, 2.2F)
                , PartPose.offset(-3.5F, 0.0F, -1.5F));
        PartDefinition fin_l_tip = fin_l.addOrReplaceChild("fin_l_tip", CubeListBuilder.create()
                // fin_l_a2
                .texOffs(34, 34).addBox(-2.0F, -0.5F, -1.0F, 2.0F, 2.0F, 2.0F)
                // fin_l_a3
                .texOffs(43, 34).addBox(-3.0F, -2.5F, 0.0F, 1.0F, 2.0F, 2.0F)
                // fin_l_b1
                .texOffs(50, 34).addBox(0.75F, 0.25F, -0.25F, 2.5F, 2.5F, 2.5F)
                // fin_l_b2
                .texOffs(59, 34).addBox(-1.0F, -1.5F, 1.0F, 2.0F, 2.0F, 2.0F)
                // fin_l_b3
                .texOffs(68, 34).addBox(-2.0F, -3.5F, 2.0F, 1.0F, 2.0F, 2.0F)
                // fin_l_c1
                .texOffs(75, 34).addBox(2.0F, -0.5F, 2.0F, 1.0F, 2.0F, 2.0F)
                , PartPose.offset(-2.5F, -1.5F, 1.5F));
        PartDefinition fin_r = body.addOrReplaceChild("fin_r", CubeListBuilder.create()
                // fin_r_web
                .texOffs(43, 18).addBox(-0.7F, -3.2F, 0.3F, 2.4F, 2.4F, 3.4F)
                // fin_r_a1
                .texOffs(90, 29).addBox(-0.6F, -0.1F, -0.6F, 3.2F, 2.2F, 2.2F)
                , PartPose.offset(3.5F, 0.0F, -1.5F));
        PartDefinition fin_r_tip = fin_r.addOrReplaceChild("fin_r_tip", CubeListBuilder.create()
                // fin_r_a2
                .texOffs(101, 29).addBox(0.0F, -0.5F, -1.0F, 2.0F, 2.0F, 2.0F)
                // fin_r_a3
                .texOffs(110, 29).addBox(2.0F, -2.5F, 0.0F, 1.0F, 2.0F, 2.0F)
                // fin_r_b1
                .texOffs(117, 29).addBox(-3.25F, 0.25F, -0.25F, 2.5F, 2.5F, 2.5F)
                // fin_r_b2
                .texOffs(0, 34).addBox(-1.0F, -1.5F, 1.0F, 2.0F, 2.0F, 2.0F)
                // fin_r_b3
                .texOffs(9, 34).addBox(1.0F, -3.5F, 2.0F, 1.0F, 2.0F, 2.0F)
                // fin_r_c1
                .texOffs(16, 34).addBox(-3.0F, -0.5F, 2.0F, 1.0F, 2.0F, 2.0F)
                , PartPose.offset(2.5F, -1.5F, 1.5F));
        PartDefinition belly = body.addOrReplaceChild("belly", CubeListBuilder.create()
                // belly1
                .texOffs(82, 34).addBox(-3.0F, -4.0F, -1.0F, 6.0F, 1.0F, 3.0F)
                // belly2
                .texOffs(0, 48).addBox(-2.0F, -3.0F, 2.0F, 4.0F, 1.0F, 2.0F)
                , PartPose.offset(0.0F, -2.0F, 4.0F));
        PartDefinition tail1 = belly.addOrReplaceChild("tail1", CubeListBuilder.create()
                // b7_mid
                .texOffs(85, 10).addBox(-2.0F, -1.0F, -0.5F, 4.0F, 2.0F, 3.0F)
                // b7_top
                .texOffs(22, 29).addBox(-1.25F, 0.75F, -0.75F, 2.5F, 1.5F, 3.5F)
                // b7_bot
                .texOffs(33, 29).addBox(-1.0F, -2.0F, -0.5F, 2.0F, 1.0F, 3.0F)
                // dspine8_base
                .texOffs(65, 44).addBox(-0.5F, 1.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                // dspine8_tip
                .texOffs(70, 44).addBox(-0.5F, 2.0F, 0.5F, 1.0F, 2.0F, 1.0F)
                // dspine9_base
                .texOffs(75, 44).addBox(-0.5F, 1.0F, 1.5F, 1.0F, 2.0F, 1.0F)
                // dspine9_tip
                .texOffs(80, 44).addBox(-0.5F, 2.0F, 2.5F, 1.0F, 2.0F, 1.0F)
                , PartPose.offset(0.0F, 0.0F, 3.5F));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail2", CubeListBuilder.create()
                // b8
                .texOffs(100, 10).addBox(-1.2F, -0.7F, -1.2F, 2.4F, 2.4F, 3.4F)
                // dspine10_base
                .texOffs(85, 44).addBox(-0.5F, 1.5F, 1.0F, 1.0F, 2.0F, 1.0F)
                // dspine10_tip
                .texOffs(90, 44).addBox(-0.5F, 2.5F, 2.0F, 1.0F, 2.0F, 1.0F)
                , PartPose.offset(0.0F, 0.5F, 2.5F));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create()
                // b9
                .texOffs(111, 10).addBox(-1.15F, -1.15F, -1.65F, 2.3F, 2.3F, 3.3F)
                // b10
                .texOffs(0, 18).addBox(-1.1F, -0.1F, 0.4F, 2.2F, 2.2F, 3.2F)
                // dspine11_base
                .texOffs(95, 44).addBox(-0.7F, 0.8F, 0.3F, 1.4F, 2.4F, 1.4F)
                // dspine11_tip
                .texOffs(100, 44).addBox(-0.6F, 1.9F, 1.4F, 1.2F, 2.2F, 1.2F)
                , PartPose.offset(0.0F, 1.5F, 2.5F));
        PartDefinition tail_fin = tail3.addOrReplaceChild("tail_fin", CubeListBuilder.create()
                // tailfin_web
                .texOffs(38, 10).addBox(-3.0F, -2.0F, -0.5F, 6.0F, 4.0F, 2.0F)
                , PartPose.offset(0.0F, 1.0F, 2.0F));
        PartDefinition streamer_a = tail_fin.addOrReplaceChild("streamer_a", CubeListBuilder.create()
                // str_a1
                .texOffs(101, 34).addBox(-1.05F, -0.05F, -0.55F, 2.1F, 1.1F, 3.1F)
                // str_a2
                .texOffs(112, 34).addBox(-1.0F, 1.0F, 2.5F, 2.0F, 1.0F, 3.0F)
                // str_a3
                .texOffs(13, 48).addBox(-0.5F, 2.0F, 5.5F, 1.0F, 1.0F, 2.0F)
                , PartPose.offset(0.0F, 1.0F, 1.0F));
        PartDefinition streamer_b = tail_fin.addOrReplaceChild("streamer_b", CubeListBuilder.create()
                // str_b1
                .texOffs(0, 39).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 1.0F, 3.0F)
                // str_b2
                .texOffs(11, 39).addBox(-1.0F, 0.5F, 3.0F, 2.0F, 1.0F, 3.0F)
                , PartPose.offset(0.0F, -0.5F, 1.5F));
        PartDefinition streamer_c = tail_fin.addOrReplaceChild("streamer_c", CubeListBuilder.create()
                // str_c1
                .texOffs(22, 39).addBox(-1.0F, -0.5F, -0.5F, 2.0F, 1.0F, 3.0F)
                // str_c2
                .texOffs(33, 39).addBox(-1.0F, 0.5F, 2.5F, 2.0F, 1.0F, 3.0F)
                , PartPose.offset(0.0F, -2.5F, 2.0F));
        PartDefinition streamer_d = tail_fin.addOrReplaceChild("streamer_d", CubeListBuilder.create()
                // str_d1
                .texOffs(44, 39).addBox(-1.0F, -0.5F, 0.5F, 2.0F, 1.0F, 3.0F)
                // str_e1
                .texOffs(55, 39).addBox(-1.0F, -2.5F, -0.5F, 2.0F, 1.0F, 3.0F)
                , PartPose.offset(0.0F, -4.5F, 1.0F));
        PartDefinition spine3 = body.addOrReplaceChild("spine3", CubeListBuilder.create()
                // dspine3_base
                .texOffs(15, 44).addBox(-0.6F, -1.1F, -0.1F, 1.2F, 2.2F, 1.2F)
                // dspine3_tip
                .texOffs(20, 44).addBox(-0.5F, 0.0F, 1.0F, 1.0F, 2.0F, 1.0F)
                , PartPose.offset(0.0F, 4.0F, -3.0F));
        PartDefinition spine4 = body.addOrReplaceChild("spine4", CubeListBuilder.create()
                // dspine4_base
                .texOffs(25, 44).addBox(-0.6F, -1.1F, -0.1F, 1.2F, 2.2F, 1.2F)
                // dspine4_tip
                .texOffs(30, 44).addBox(-0.5F, 0.0F, 1.0F, 1.0F, 2.0F, 1.0F)
                , PartPose.offset(0.0F, 3.0F, -1.0F));
        PartDefinition spine5 = body.addOrReplaceChild("spine5", CubeListBuilder.create()
                // dspine5_base
                .texOffs(35, 44).addBox(-0.7F, -1.2F, -0.2F, 1.4F, 2.4F, 1.4F)
                // dspine5_tip
                .texOffs(40, 44).addBox(-0.5F, 0.0F, 1.0F, 1.0F, 2.0F, 1.0F)
                , PartPose.offset(0.0F, 2.0F, 1.0F));
        PartDefinition spine6 = body.addOrReplaceChild("spine6", CubeListBuilder.create()
                // dspine6_base
                .texOffs(45, 44).addBox(-0.7F, -1.2F, -0.2F, 1.4F, 2.4F, 1.4F)
                // dspine6_tip
                .texOffs(50, 44).addBox(-0.5F, 0.0F, 1.0F, 1.0F, 2.0F, 1.0F)
                , PartPose.offset(0.0F, 1.0F, 3.0F));
        PartDefinition spine7 = body.addOrReplaceChild("spine7", CubeListBuilder.create()
                // dspine7_base
                .texOffs(55, 44).addBox(-0.6F, -1.1F, -0.1F, 1.2F, 2.2F, 1.2F)
                // dspine7_tip
                .texOffs(60, 44).addBox(-0.5F, 0.0F, 1.0F, 1.0F, 2.0F, 1.0F)
                , PartPose.offset(0.0F, 0.0F, 5.0F));
        PartDefinition ventral_l = body.addOrReplaceChild("ventral_l", CubeListBuilder.create()
                // ventral_l_a
                .texOffs(115, 44).addBox(0.0F, -1.0F, -1.0F, 1.0F, 2.0F, 1.0F)
                // ventral_l_b
                .texOffs(120, 44).addBox(-1.05F, -2.05F, -0.05F, 1.1F, 2.1F, 1.1F)
                , PartPose.offset(-2.0F, -5.0F, 3.0F));
        PartDefinition ventral_r = body.addOrReplaceChild("ventral_r", CubeListBuilder.create()
                // ventral_r_a
                .texOffs(105, 44).addBox(-1.0F, -1.0F, -1.0F, 1.0F, 2.0F, 1.0F)
                // ventral_r_b
                .texOffs(110, 44).addBox(-0.05F, -2.05F, -0.05F, 1.1F, 2.1F, 1.1F)
                , PartPose.offset(2.0F, -5.0F, 3.0F));
    }
}
