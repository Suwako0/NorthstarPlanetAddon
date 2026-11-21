package com.suwako.northstarplanetaddon.moon.client.model;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.entity.MoonDeerEntity;
import com.suwako.northstarplanetaddon.moon.entity.NMACombatAnimation;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 濠殿喖顭俊鍥э耿閵忋倕瀚夐柛顐犲劤閻?model, generated from the Blockbench rig ({@code build/art/moon_deer.geo.json}) and
 * kept in one place with the generator so geometry edits stay reproducible: run
 * {@code build/art/gen-moon-deer-java.ps1} after changing the Blockbench project.
 *
 * <p>The rig is a full quadruped skeleton: four body segments plus a rump plate and a chest
 * rune, a two-joint neck with a mane ruff, a skull split into cranium / brow / face plate /
 * cheek / two jaw parts / nose, a brow gem, two eyes, two ears, a five-joint antler per side
 * with three tines and two hanging blossom clusters, six ice blades along the spine and two on
 * the neck, a two-joint tail with a fan, and four three-segment legs.</p>
 *
 * <p>Texture sheet: 256x256, standard box UV; slot table lives in the geo.json. The model
 * faces {@code -Z}, so its own right is {@code -X}. Ground plane is {@code y = 24}.</p>
 */
public class MoonDeerModel extends HierarchicalModel<MoonDeerEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MoonModule.NAMESPACE, "moon_deer"), "main");

    /** Degrees to radians, so the animation code can talk in degrees like the rig does. */
    private static final float DEG = Mth.DEG_TO_RAD;

    /** PartPose rest poses are authored in degrees; the API wants radians. */
    private static float rad(float degrees) {
        return degrees * DEG;
    }

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart chest;
    private final ModelPart rune;
    private final ModelPart withers;
    private final ModelPart barrel;
    private final ModelPart haunch;
    private final ModelPart rump;
    private final ModelPart neck;
    private final ModelPart mane_neck_low;
    private final ModelPart neck_upper;
    private final ModelPart mane_neck_high;
    private final ModelPart head;
    private final ModelPart ear_right;
    private final ModelPart antler_right;
    private final ModelPart antler_right_mid;
    private final ModelPart antler_right_tip;
    private final ModelPart antler_right_branch_front;
    private final ModelPart antler_right_branch_outer;
    private final ModelPart antler_right_subtine_upper;
    private final ModelPart antler_right_flower;
    private final ModelPart antler_right_branch_back;
    private final ModelPart antler_right_subtine_lower;
    private final ModelPart antler_right_flower_low;
    private final ModelPart ear_left;
    private final ModelPart antler_left;
    private final ModelPart antler_left_mid;
    private final ModelPart antler_left_tip;
    private final ModelPart antler_left_branch_front;
    private final ModelPart antler_left_branch_outer;
    private final ModelPart antler_left_subtine_upper;
    private final ModelPart antler_left_flower;
    private final ModelPart antler_left_branch_back;
    private final ModelPart antler_left_subtine_lower;
    private final ModelPart antler_left_flower_low;
    private final ModelPart mane_a;
    private final ModelPart mane_b;
    private final ModelPart mane_c;
    private final ModelPart mane_d;
    private final ModelPart mane_e;
    private final ModelPart tail;
    private final ModelPart tail_tip;
    private final ModelPart right_front_leg;
    private final ModelPart right_front_shin;
    private final ModelPart right_front_hoof;
    private final ModelPart right_hind_leg;
    private final ModelPart right_hind_shin;
    private final ModelPart right_hind_hoof;
    private final ModelPart left_front_leg;
    private final ModelPart left_front_shin;
    private final ModelPart left_front_hoof;
    private final ModelPart left_hind_leg;
    private final ModelPart left_hind_shin;
    private final ModelPart left_hind_hoof;

    public MoonDeerModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.chest = body.getChild("chest");
        this.rune = chest.getChild("rune");
        this.withers = body.getChild("withers");
        this.barrel = body.getChild("barrel");
        this.haunch = body.getChild("haunch");
        this.rump = body.getChild("rump");
        this.neck = body.getChild("neck");
        this.mane_neck_low = neck.getChild("mane_neck_low");
        this.neck_upper = neck.getChild("neck_upper");
        this.mane_neck_high = neck_upper.getChild("mane_neck_high");
        this.head = neck_upper.getChild("head");
        this.ear_right = head.getChild("ear_right");
        this.antler_right = head.getChild("antler_right");
        this.antler_right_mid = antler_right.getChild("antler_right_mid");
        this.antler_right_tip = antler_right_mid.getChild("antler_right_tip");
        this.antler_right_branch_front = antler_right_mid.getChild("antler_right_branch_front");
        this.antler_right_branch_outer = antler_right_mid.getChild("antler_right_branch_outer");
        this.antler_right_subtine_upper = antler_right_mid.getChild("antler_right_subtine_upper");
        this.antler_right_flower = antler_right_mid.getChild("antler_right_flower");
        this.antler_right_branch_back = antler_right.getChild("antler_right_branch_back");
        this.antler_right_subtine_lower = antler_right.getChild("antler_right_subtine_lower");
        this.antler_right_flower_low = antler_right.getChild("antler_right_flower_low");
        this.ear_left = head.getChild("ear_left");
        this.antler_left = head.getChild("antler_left");
        this.antler_left_mid = antler_left.getChild("antler_left_mid");
        this.antler_left_tip = antler_left_mid.getChild("antler_left_tip");
        this.antler_left_branch_front = antler_left_mid.getChild("antler_left_branch_front");
        this.antler_left_branch_outer = antler_left_mid.getChild("antler_left_branch_outer");
        this.antler_left_subtine_upper = antler_left_mid.getChild("antler_left_subtine_upper");
        this.antler_left_flower = antler_left_mid.getChild("antler_left_flower");
        this.antler_left_branch_back = antler_left.getChild("antler_left_branch_back");
        this.antler_left_subtine_lower = antler_left.getChild("antler_left_subtine_lower");
        this.antler_left_flower_low = antler_left.getChild("antler_left_flower_low");
        this.mane_a = body.getChild("mane_a");
        this.mane_b = body.getChild("mane_b");
        this.mane_c = body.getChild("mane_c");
        this.mane_d = body.getChild("mane_d");
        this.mane_e = body.getChild("mane_e");
        this.tail = body.getChild("tail");
        this.tail_tip = tail.getChild("tail_tip");
        this.right_front_leg = root.getChild("right_front_leg");
        this.right_front_shin = right_front_leg.getChild("right_front_shin");
        this.right_front_hoof = right_front_shin.getChild("right_front_hoof");
        this.right_hind_leg = root.getChild("right_hind_leg");
        this.right_hind_shin = right_hind_leg.getChild("right_hind_shin");
        this.right_hind_hoof = right_hind_shin.getChild("right_hind_hoof");
        this.left_front_leg = root.getChild("left_front_leg");
        this.left_front_shin = left_front_leg.getChild("left_front_shin");
        this.left_front_hoof = left_front_shin.getChild("left_front_hoof");
        this.left_hind_leg = root.getChild("left_hind_leg");
        this.left_hind_shin = left_hind_leg.getChild("left_hind_shin");
        this.left_hind_hoof = left_hind_shin.getChild("left_hind_hoof");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(82, 93).addBox(-1.3F, -6.2F, -1F, 2.6F, 1.8F, 5F)
                        .texOffs(99, 93).addBox(-1.35F, -6.8F, 4F, 2.7F, 1.8F, 5F)
                , PartPose.offset(0F, -1F, 0F));
        PartDefinition chest = body.addOrReplaceChild("chest", CubeListBuilder.create()
                        .texOffs(63, 19).addBox(-4.2F, -6F, -4.1F, 8.4F, 2.6F, 9.2F)
                        .texOffs(0, 0).addBox(-5.5F, -3.4F, -5F, 11F, 6.8F, 11F)
                        .texOffs(102, 19).addBox(-4.46F, 3.4F, -4.28F, 8.92F, 2.6F, 9.56F)
                        .texOffs(21, 48).addBox(-3.2F, -5.6F, -6.4F, 6.4F, 8.6F, 1.4F)
                        .texOffs(141, 19).addBox(-4.6F, 4F, -4.4F, 9.2F, 2.4F, 9.4F)
                        .texOffs(90, 72).addBox(-1.8F, -7F, -4F, 3.6F, 2F, 7F)
                        .texOffs(204, 110).addBox(1.2F, -3F, -7F, 1.4F, 2.8F, 1.6F)
                        .texOffs(213, 110).addBox(-2.6F, -3F, -7F, 1.4F, 2.8F, 1.6F)
                        .texOffs(222, 110).addBox(2F, -0.6F, -6.4F, 1.6F, 3F, 1.6F)
                        .texOffs(231, 110).addBox(-3.6F, -0.6F, -6.4F, 1.6F, 3F, 1.6F)
                        .texOffs(240, 110).addBox(0.6F, 2.2F, -6F, 1.6F, 3F, 1.6F)
                        .texOffs(0, 117).addBox(-2.2F, 2.2F, -6F, 1.6F, 3F, 1.6F)
                        .texOffs(45, 0).addBox(-6.2F, -6.4F, -4F, 1.8F, 8.4F, 9F)
                        .texOffs(181, 48).addBox(-5.8F, -7.6F, -3F, 2.4F, 2.2F, 7F)
                        .texOffs(20, 102).addBox(-4.4F, -6.6F, -5.6F, 2.2F, 3F, 2.2F)
                        .texOffs(9, 123).addBox(1.2F, -3F, -7F, 1.4F, 2.8F, 1.6F)
                        .texOffs(18, 123).addBox(2F, -0.6F, -6.4F, 1.6F, 3F, 1.6F)
                        .texOffs(27, 123).addBox(0.6F, 2.2F, -6F, 1.6F, 3F, 1.6F)
                        .texOffs(68, 0).addBox(4.4F, -6.4F, -4F, 1.8F, 8.4F, 9F)
                        .texOffs(219, 61).addBox(3.4F, -7.6F, -3F, 2.4F, 2.2F, 7F)
                        .texOffs(24, 110).addBox(2.2F, -6.6F, -5.6F, 2.2F, 3F, 2.2F)
                , PartPose.offset(0F, 0F, -11F));
        PartDefinition rune = chest.addOrReplaceChild("rune", CubeListBuilder.create()
                        .texOffs(36, 129).addBox(-1.5F, -2.4F, -1.75F, 3F, 2.2F, 1F)
                        .texOffs(194, 129).addBox(-0.8F, -3.4F, -1.77F, 1.6F, 1.2F, 1F)
                        .texOffs(201, 129).addBox(-0.8F, -0.4F, -1.77F, 1.6F, 1.2F, 1F)
                        .texOffs(217, 129).addBox(-3.3F, -1.9F, -1.8F, 1.6F, 1F, 1F)
                        .texOffs(224, 129).addBox(1.7F, -1.9F, -1.8F, 1.6F, 1F, 1F)
                        .texOffs(231, 129).addBox(-0.5F, -4.75F, -1.73F, 1F, 1F, 1F)
                , PartPose.offset(0F, 0F, -6.4F));
        PartDefinition withers = body.addOrReplaceChild("withers", CubeListBuilder.create()
                        .texOffs(40, 48).addBox(-4.3F, -2.54F, -3.5F, 8.6F, 1F, 10F)
                        .texOffs(182, 19).addBox(-5.2F, -1.672F, -4F, 10.4F, 1.344F, 11F)
                        .texOffs(0, 35).addBox(-4.48F, -0.46F, -3.6F, 8.96F, 1F, 10.2F)
                        .texOffs(202, 48).addBox(-2.2F, -4F, -2F, 4.4F, 2F, 8F)
                        .texOffs(41, 35).addBox(-5.8F, -2F, -2F, 1.6F, 4F, 8F)
                        .texOffs(0, 48).addBox(4.2F, -2F, -2F, 1.6F, 4F, 8F)
                , PartPose.offset(0F, -6F, -9F));
        PartDefinition barrel = body.addOrReplaceChild("barrel", CubeListBuilder.create()
                        .texOffs(62, 35).addBox(-3.7F, -5F, -3.2F, 7.4F, 2.6F, 8.4F)
                        .texOffs(176, 0).addBox(-4.8F, -2.4F, -4F, 9.6F, 5.8F, 10F)
                        .texOffs(97, 35).addBox(-3.92F, 3.4F, -3.36F, 7.84F, 2.6F, 8.72F)
                        .texOffs(79, 48).addBox(-3.8F, 5F, -3F, 7.6F, 2.6F, 8F)
                        .texOffs(91, 0).addBox(-5.4F, -4F, -3F, 1.2F, 8.5F, 8F)
                        .texOffs(229, 48).addBox(-5F, -3.6F, -3.6F, 1.2F, 4.6F, 4.6F)
                        .texOffs(108, 83).addBox(-5F, 0F, -3F, 1.2F, 4F, 4F)
                        .texOffs(155, 0).addBox(4.2F, -4F, -3F, 1.2F, 8.5F, 8F)
                        .texOffs(240, 61).addBox(3.8F, -3.6F, -3.6F, 1.2F, 4.6F, 4.6F)
                        .texOffs(15, 93).addBox(3.8F, 0F, -3F, 1.2F, 4F, 4F)
                , PartPose.offset(0F, 0F, -1F));
        PartDefinition haunch = body.addOrReplaceChild("haunch", CubeListBuilder.create()
                        .texOffs(132, 35).addBox(-4.2F, -5.4F, -2.1F, 8.4F, 2.6F, 8.2F)
                        .texOffs(112, 0).addBox(-5.4F, -2.8F, -3F, 10.8F, 6.2F, 10F)
                        .texOffs(169, 35).addBox(-4.44F, 3.4F, -2.28F, 8.88F, 2.6F, 8.56F)
                        .texOffs(217, 0).addBox(-6F, -5F, -1.5F, 1.6F, 7F, 7.5F)
                        .texOffs(112, 48).addBox(-4F, 5F, -2F, 8F, 2.4F, 8F)
                        .texOffs(0, 19).addBox(-5.8F, -3F, -0.5F, 3F, 8F, 6.5F)
                        .texOffs(121, 83).addBox(-6.2F, -2F, -2.6F, 1.4F, 5F, 3F)
                        .texOffs(21, 19).addBox(4.4F, -5F, -1.5F, 1.6F, 7F, 7.5F)
                        .texOffs(42, 19).addBox(2.8F, -3F, -0.5F, 3F, 8F, 6.5F)
                        .texOffs(28, 93).addBox(4.8F, -2F, -2.6F, 1.4F, 5F, 3F)
                , PartPose.offset(0F, 0F, 7F));
        PartDefinition rump = body.addOrReplaceChild("rump", CubeListBuilder.create()
                        .texOffs(0, 61).addBox(-4.2F, -2.54F, -3.4F, 8.4F, 1F, 8.2F)
                        .texOffs(206, 35).addBox(-5F, -1.672F, -4F, 10F, 1.344F, 9.4F)
                        .texOffs(37, 61).addBox(-4.36F, -0.46F, -3.52F, 8.72F, 1F, 8.44F)
                        .texOffs(113, 72).addBox(-3.4F, -3.4F, -2F, 6.8F, 1.6F, 6.6F)
                , PartPose.offset(0F, -6F, 10F));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                        .texOffs(33, 102).addBox(-2.1F, -6.4F, -2.9F, 4.2F, 1.664F, 4F)
                        .texOffs(142, 72).addBox(-2.8F, -4.736F, -3.4F, 5.6F, 3.072F, 5F)
                        .texOffs(116, 93).addBox(-2.24F, -1.664F, -3F, 4.48F, 1.664F, 4.2F)
                        .texOffs(145, 48).addBox(-2.1F, -7F, -3F, 4.2F, 6.4F, 4F)
                        .texOffs(137, 93).addBox(-1.2F, -7.4F, -4F, 2.4F, 5.8F, 1F)
                        .texOffs(74, 61).addBox(-3.8F, -5.6F, -3F, 1.2F, 5.2F, 4F)
                        .texOffs(9, 117).addBox(-0.9F, -9F, -2.6F, 1.8F, 2.6F, 1.6F)
                        .texOffs(18, 117).addBox(-2.1F, -8.4F, -1.8F, 1.2F, 2.6F, 1.4F)
                        .texOffs(27, 117).addBox(-0.8F, -10F, -1.8F, 1.6F, 2.4F, 1.4F)
                        .texOffs(0, 72).addBox(2.6F, -5.6F, -3F, 1.2F, 5.2F, 4F)
                        .texOffs(36, 123).addBox(0.9F, -8.4F, -1.8F, 1.2F, 2.6F, 1.4F)
                , PartPose.offsetAndRotation(0F, -6F, -11F, rad(14), rad(0), rad(0)));
        PartDefinition mane_neck_low = neck.addOrReplaceChild("mane_neck_low", CubeListBuilder.create()
                        .texOffs(132, 83).addBox(-1.1F, -4.4F, 0.8F, 2.2F, 4.4F, 2.2F)
                        .texOffs(36, 117).addBox(-0.6F, -6.6F, 0.2F, 1.2F, 2.4F, 1.6F)
                , PartPose.offsetAndRotation(0F, -5F, -0.4F, rad(-26), rad(0), rad(0)));
        PartDefinition neck_upper = neck.addOrReplaceChild("neck_upper", CubeListBuilder.create()
                        .texOffs(45, 117).addBox(-2F, -3.81F, -2F, 4F, 1F, 3.2F)
                        .texOffs(52, 102).addBox(-2.6F, -2.812F, -2.4F, 5.2F, 1.824F, 4F)
                        .texOffs(62, 117).addBox(-2.12F, -0.99F, -2.08F, 4.24F, 1F, 3.36F)
                        .texOffs(146, 93).addBox(-2F, -4.2F, -2F, 4F, 3.8F, 3F)
                        .texOffs(81, 117).addBox(-1.1F, -4.6F, -3F, 2.2F, 3.6F, 1F)
                , PartPose.offsetAndRotation(0F, -8F, -4F, rad(-5), rad(0), rad(0)));
        PartDefinition mane_neck_high = neck_upper.addOrReplaceChild("mane_neck_high", CubeListBuilder.create()
                        .texOffs(145, 83).addBox(-1.1F, -4.4F, 1F, 2.2F, 4.4F, 2.2F)
                        .texOffs(90, 117).addBox(-0.6F, -6.6F, 0.4F, 1.2F, 2.4F, 1.6F)
                , PartPose.offsetAndRotation(0F, -3F, 1.4F, rad(-24), rad(0), rad(0)));
        PartDefinition head = neck_upper.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(158, 83).addBox(-2.5F, -2.8F, -4.1F, 5F, 1.508F, 5F)
                        .texOffs(87, 61).addBox(-3.2F, -1.292F, -4.7F, 6.4F, 2.784F, 6.2F)
                        .texOffs(181, 83).addBox(-2.64F, 1.492F, -4.22F, 5.28F, 1.508F, 5.24F)
                        .texOffs(99, 117).addBox(-3.4F, -2.2F, -5.7F, 6.8F, 2.2F, 2F)
                        .texOffs(73, 102).addBox(-2.7F, -1.4F, -6.1F, 5.4F, 3.2F, 1.4F)
                        .texOffs(90, 102).addBox(-1.4F, -3.6F, -4.1F, 2.8F, 1.4F, 3.8F)
                        .texOffs(116, 61).addBox(-3.5F, -1.6F, -5.3F, 1.2F, 3.6F, 6F)
                        .texOffs(105, 102).addBox(-3.1F, 1.6F, -4.5F, 1.3F, 1.6F, 4F)
                        .texOffs(118, 102).addBox(-3.8F, 1.4F, -3.3F, 1.1F, 2.6F, 2.2F)
                        .texOffs(118, 117).addBox(-2F, -0.4F, -6.9F, 4F, 2.6F, 2F)
                        .texOffs(126, 123).addBox(-1.8F, 0.1F, -8.3F, 3.6F, 1.7F, 1.4F)
                        .texOffs(208, 129).addBox(-1.3F, 0.3F, -9.1F, 2.6F, 1F, 1F)
                        .texOffs(139, 123).addBox(-0.6F, -0.8F, -7.5F, 1.2F, 1.4F, 1.2F)
                        .texOffs(148, 123).addBox(-1.8F, 1.8F, -6.3F, 3.6F, 1.2F, 1.4F)
                        .texOffs(45, 129).addBox(-1.5F, 2.7F, -6F, 3F, 1.1F, 1F)
                        .texOffs(54, 129).addBox(-1.1F, 3.05F, -7.9F, 2.2F, 1F, 2F)
                        .texOffs(161, 123).addBox(-1.9F, -1.6F, -6.5F, 3.8F, 2.8F, 1F)
                        .texOffs(65, 129).addBox(-1.5F, -1.2F, -6.95F, 3F, 2F, 1F)
                        .texOffs(131, 117).addBox(-3.4F, -0.8F, -5.1F, 1.1F, 2.4F, 1.8F)
                        .texOffs(172, 123).addBox(-3.6F, -0.4F, -4.8F, 1F, 1.7F, 1.2F)
                        .texOffs(13, 72).addBox(2.3F, -1.6F, -5.3F, 1.2F, 3.6F, 6F)
                        .texOffs(37, 110).addBox(1.8F, 1.6F, -4.5F, 1.3F, 1.6F, 4F)
                        .texOffs(50, 110).addBox(2.7F, 1.4F, -3.3F, 1.1F, 2.6F, 2.2F)
                        .texOffs(45, 123).addBox(2.3F, -0.8F, -5.1F, 1.1F, 2.4F, 1.8F)
                        .texOffs(229, 123).addBox(2.6F, -0.4F, -4.8F, 1F, 1.7F, 1.2F)
                , PartPose.offsetAndRotation(0F, -5F, -1.5F, rad(3), rad(0), rad(0)));
        PartDefinition ear_right = head.addOrReplaceChild("ear_right", CubeListBuilder.create()
                        .texOffs(140, 117).addBox(-0.9F, -2.6F, -1.2F, 1.7F, 3F, 1.8F)
                        .texOffs(149, 117).addBox(-0.7F, -5.2F, -1.8F, 1.3F, 3F, 2F)
                        .texOffs(179, 123).addBox(-0.55F, -4F, -1.55F, 1F, 2.4F, 1F)
                , PartPose.offsetAndRotation(-2.3F, -2.6F, -1.3F, rad(6), rad(12), rad(-18)));
        PartDefinition antler_right = head.addOrReplaceChild("antler_right", CubeListBuilder.create()
                        .texOffs(206, 83).addBox(-1.2F, -5F, -1.2F, 2.4F, 5F, 2.4F)
                , PartPose.offsetAndRotation(-2.3F, -2.4F, -2.1F, rad(-4), rad(0), rad(-56)));
        PartDefinition antler_right_mid = antler_right.addOrReplaceChild("antler_right_mid", CubeListBuilder.create()
                        .texOffs(161, 93).addBox(-1.1F, -5F, -1F, 2.2F, 5F, 2F)
                , PartPose.offsetAndRotation(0F, -5F, 0F, rad(3), rad(0), rad(17)));
        PartDefinition antler_right_tip = antler_right_mid.addOrReplaceChild("antler_right_tip", CubeListBuilder.create()
                        .texOffs(172, 93).addBox(-1F, -4.5F, -0.8F, 2F, 4.5F, 1.6F)
                        .texOffs(184, 123).addBox(-0.7F, -5.9F, -0.6F, 1.4F, 1.7F, 1.2F)
                , PartPose.offsetAndRotation(0F, -5F, 0F, rad(4), rad(0), rad(24)));
        PartDefinition antler_right_branch_front = antler_right_mid.addOrReplaceChild("antler_right_branch_front", CubeListBuilder.create()
                        .texOffs(129, 102).addBox(-1.1F, -3.4F, -1.2F, 2.2F, 3.4F, 1.4F)
                        .texOffs(193, 123).addBox(-0.6F, -5.2F, -2.2F, 1.4F, 2F, 1.2F)
                , PartPose.offsetAndRotation(0F, -2.7F, -1.2F, rad(50), rad(0), rad(-10)));
        PartDefinition antler_right_branch_outer = antler_right_mid.addOrReplaceChild("antler_right_branch_outer", CubeListBuilder.create()
                        .texOffs(151, 102).addBox(-1.1F, -3.4F, -1F, 2F, 3.4F, 1.8F)
                        .texOffs(202, 123).addBox(-0.7F, -5.2F, -0.8F, 1.2F, 2F, 1.2F)
                , PartPose.offsetAndRotation(-1.2F, -3.2F, 0F, rad(10), rad(0), rad(-44)));
        PartDefinition antler_right_subtine_upper = antler_right_mid.addOrReplaceChild("antler_right_subtine_upper", CubeListBuilder.create()
                        .texOffs(158, 117).addBox(-0.9F, -3F, -0.8F, 1.8F, 3F, 1.6F)
                , PartPose.offsetAndRotation(0F, -3.6F, 0F, rad(20), rad(0), rad(38)));
        PartDefinition antler_right_flower = antler_right_mid.addOrReplaceChild("antler_right_flower", CubeListBuilder.create()
                        .texOffs(211, 123).addBox(-0.8F, -0.7F, -0.7F, 1.6F, 1.6F, 1.6F)
                        .texOffs(74, 129).addBox(0.15F, -0.55F, -1.3F, 1F, 1F, 1.4F)
                        .texOffs(81, 129).addBox(-1.15F, -0.55F, -1.3F, 1F, 1F, 1.4F)
                        .texOffs(88, 129).addBox(0.15F, -1.15F, -0.1F, 1F, 1F, 1.4F)
                        .texOffs(95, 129).addBox(-1.15F, -1.15F, -0.1F, 1F, 1F, 1.4F)
                , PartPose.offset(0F, -4.8F, -0.8F));
        PartDefinition antler_right_branch_back = antler_right.addOrReplaceChild("antler_right_branch_back", CubeListBuilder.create()
                        .texOffs(140, 102).addBox(-1.1F, -3.4F, 1F, 2.2F, 3.4F, 1.4F)
                , PartPose.offsetAndRotation(0F, -2.2F, 1.2F, rad(-44), rad(0), rad(-8)));
        PartDefinition antler_right_subtine_lower = antler_right.addOrReplaceChild("antler_right_subtine_lower", CubeListBuilder.create()
                        .texOffs(167, 117).addBox(-0.9F, -3F, 0.2F, 1.8F, 3F, 1.6F)
                , PartPose.offsetAndRotation(0F, -3.2F, 0F, rad(-28), rad(0), rad(-28)));
        PartDefinition antler_right_flower_low = antler_right.addOrReplaceChild("antler_right_flower_low", CubeListBuilder.create()
                        .texOffs(220, 123).addBox(-0.7F, -0.7F, -0.7F, 1.4F, 1.4F, 1.4F)
                        .texOffs(102, 129).addBox(0.1F, -0.6F, -1.2F, 1F, 1F, 1.2F)
                        .texOffs(111, 129).addBox(-1.1F, -0.6F, -1.2F, 1F, 1F, 1.2F)
                        .texOffs(118, 129).addBox(0.1F, -1.2F, -0.2F, 1F, 1F, 1.2F)
                        .texOffs(127, 129).addBox(-1.1F, -1.2F, -0.2F, 1F, 1F, 1.2F)
                , PartPose.offset(0F, -1.8F, 1.8F));
        PartDefinition ear_left = head.addOrReplaceChild("ear_left", CubeListBuilder.create()
                        .texOffs(90, 123).addBox(-0.8F, -2.6F, -1.2F, 1.7F, 3F, 1.8F)
                        .texOffs(99, 123).addBox(-0.6F, -5.2F, -1.8F, 1.3F, 3F, 2F)
                        .texOffs(236, 123).addBox(-0.45F, -4F, -1.55F, 1F, 2.4F, 1F)
                , PartPose.offsetAndRotation(2.3F, -2.6F, -1.3F, rad(6), rad(-12), rad(18)));
        PartDefinition antler_left = head.addOrReplaceChild("antler_left", CubeListBuilder.create()
                        .texOffs(69, 93).addBox(-1.2F, -5F, -1.2F, 2.4F, 5F, 2.4F)
                , PartPose.offsetAndRotation(2.3F, -2.4F, -2.1F, rad(-4), rad(0), rad(56)));
        PartDefinition antler_left_mid = antler_left.addOrReplaceChild("antler_left_mid", CubeListBuilder.create()
                        .texOffs(0, 102).addBox(-1.1F, -5F, -1F, 2.2F, 5F, 2F)
                , PartPose.offsetAndRotation(0F, -5F, 0F, rad(3), rad(0), rad(-17)));
        PartDefinition antler_left_tip = antler_left_mid.addOrReplaceChild("antler_left_tip", CubeListBuilder.create()
                        .texOffs(11, 102).addBox(-1F, -4.5F, -0.8F, 2F, 4.5F, 1.6F)
                        .texOffs(241, 123).addBox(-0.7F, -5.9F, -0.6F, 1.4F, 1.7F, 1.2F)
                , PartPose.offsetAndRotation(0F, -5F, 0F, rad(4), rad(0), rad(-24)));
        PartDefinition antler_left_branch_front = antler_left_mid.addOrReplaceChild("antler_left_branch_front", CubeListBuilder.create()
                        .texOffs(173, 110).addBox(-1.1F, -3.4F, -1.2F, 2.2F, 3.4F, 1.4F)
                        .texOffs(0, 129).addBox(-0.8F, -5.2F, -2.2F, 1.4F, 2F, 1.2F)
                , PartPose.offsetAndRotation(0F, -2.7F, -1.2F, rad(50), rad(0), rad(10)));
        PartDefinition antler_left_branch_outer = antler_left_mid.addOrReplaceChild("antler_left_branch_outer", CubeListBuilder.create()
                        .texOffs(195, 110).addBox(-0.9F, -3.4F, -1F, 2F, 3.4F, 1.8F)
                        .texOffs(9, 129).addBox(-0.5F, -5.2F, -0.8F, 1.2F, 2F, 1.2F)
                , PartPose.offsetAndRotation(1.2F, -3.2F, 0F, rad(10), rad(0), rad(44)));
        PartDefinition antler_left_subtine_upper = antler_left_mid.addOrReplaceChild("antler_left_subtine_upper", CubeListBuilder.create()
                        .texOffs(108, 123).addBox(-0.9F, -3F, -0.8F, 1.8F, 3F, 1.6F)
                , PartPose.offsetAndRotation(0F, -3.6F, 0F, rad(20), rad(0), rad(-38)));
        PartDefinition antler_left_flower = antler_left_mid.addOrReplaceChild("antler_left_flower", CubeListBuilder.create()
                        .texOffs(18, 129).addBox(-0.8F, -0.7F, -0.7F, 1.6F, 1.6F, 1.6F)
                        .texOffs(134, 129).addBox(-1.15F, -0.55F, -1.3F, 1F, 1F, 1.4F)
                        .texOffs(141, 129).addBox(0.15F, -0.55F, -1.3F, 1F, 1F, 1.4F)
                        .texOffs(148, 129).addBox(-1.15F, -1.15F, -0.1F, 1F, 1F, 1.4F)
                        .texOffs(155, 129).addBox(0.15F, -1.15F, -0.1F, 1F, 1F, 1.4F)
                , PartPose.offset(0F, -4.8F, -0.8F));
        PartDefinition antler_left_branch_back = antler_left.addOrReplaceChild("antler_left_branch_back", CubeListBuilder.create()
                        .texOffs(184, 110).addBox(-1.1F, -3.4F, 1F, 2.2F, 3.4F, 1.4F)
                , PartPose.offsetAndRotation(0F, -2.2F, 1.2F, rad(-44), rad(0), rad(8)));
        PartDefinition antler_left_subtine_lower = antler_left.addOrReplaceChild("antler_left_subtine_lower", CubeListBuilder.create()
                        .texOffs(117, 123).addBox(-0.9F, -3F, 0.2F, 1.8F, 3F, 1.6F)
                , PartPose.offsetAndRotation(0F, -3.2F, 0F, rad(-28), rad(0), rad(28)));
        PartDefinition antler_left_flower_low = antler_left.addOrReplaceChild("antler_left_flower_low", CubeListBuilder.create()
                        .texOffs(27, 129).addBox(-0.7F, -0.7F, -0.7F, 1.4F, 1.4F, 1.4F)
                        .texOffs(162, 129).addBox(-1.1F, -0.6F, -1.2F, 1F, 1F, 1.2F)
                        .texOffs(171, 129).addBox(0.1F, -0.6F, -1.2F, 1F, 1F, 1.2F)
                        .texOffs(178, 129).addBox(-1.1F, -1.2F, -0.2F, 1F, 1F, 1.2F)
                        .texOffs(187, 129).addBox(0.1F, -1.2F, -0.2F, 1F, 1F, 1.2F)
                , PartPose.offset(0F, -1.8F, 1.8F));
        PartDefinition mane_a = body.addOrReplaceChild("mane_a", CubeListBuilder.create()
                        .texOffs(165, 72).addBox(-1.2F, -4F, -1.4F, 2.4F, 5.3F, 2.8F)
                        .texOffs(176, 117).addBox(-0.7F, -6.4F, -1.8F, 1.4F, 2.6F, 1.6F)
                        .texOffs(185, 117).addBox(-2.4F, -3F, -1F, 1.2F, 2.2F, 1.6F)
                        .texOffs(54, 123).addBox(1.2F, -3F, -1F, 1.2F, 2.2F, 1.6F)
                , PartPose.offsetAndRotation(0F, -9.6F, -11F, rad(-18), rad(0), rad(0)));
        PartDefinition mane_b = body.addOrReplaceChild("mane_b", CubeListBuilder.create()
                        .texOffs(178, 72).addBox(-1.2F, -4F, -1.4F, 2.4F, 5.2F, 2.8F)
                        .texOffs(194, 117).addBox(-0.7F, -6.4F, -1.8F, 1.4F, 2.6F, 1.6F)
                , PartPose.offsetAndRotation(0F, -9.4F, -4F, rad(-18), rad(0), rad(0)));
        PartDefinition mane_c = body.addOrReplaceChild("mane_c", CubeListBuilder.create()
                        .texOffs(133, 61).addBox(-1.1F, -4F, -2.4F, 2.2F, 6.8F, 2.8F)
                        .texOffs(203, 117).addBox(-0.65F, -6.4F, -2.8F, 1.3F, 2.6F, 1.6F)
                        .texOffs(212, 117).addBox(-2.2F, -3F, -2F, 1.1F, 2.2F, 1.6F)
                        .texOffs(63, 123).addBox(1.1F, -3F, -2F, 1.1F, 2.2F, 1.6F)
                , PartPose.offsetAndRotation(0F, -8.4F, 2F, rad(-18), rad(0), rad(0)));
        PartDefinition mane_d = body.addOrReplaceChild("mane_d", CubeListBuilder.create()
                        .texOffs(146, 61).addBox(-1.1F, -4F, -2.4F, 2.2F, 6.8F, 2.8F)
                        .texOffs(221, 117).addBox(-0.65F, -6.4F, -2.8F, 1.3F, 2.6F, 1.6F)
                , PartPose.offsetAndRotation(0F, -8.6F, 8.5F, rad(-17), rad(0), rad(0)));
        PartDefinition mane_e = body.addOrReplaceChild("mane_e", CubeListBuilder.create()
                        .texOffs(191, 72).addBox(-1F, -4F, -2.4F, 2F, 5.7F, 2.8F)
                        .texOffs(230, 117).addBox(-0.6F, -6.4F, -2.8F, 1.2F, 2.6F, 1.6F)
                        .texOffs(239, 117).addBox(-2.1F, -3F, -2.2F, 1.1F, 2.2F, 1.6F)
                        .texOffs(72, 123).addBox(1F, -3F, -2.2F, 1.1F, 2.2F, 1.6F)
                , PartPose.offsetAndRotation(0F, -10.2F, 14F, rad(-16), rad(0), rad(0)));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                        .texOffs(164, 48).addBox(-1.7F, 0.6F, -0.4F, 3.4F, 6.2F, 3F)
                        .texOffs(219, 83).addBox(-1.4F, 6.2F, 1.4F, 2.8F, 4.4F, 2.6F)
                , PartPose.offset(0F, -8.2F, 14F));
        PartDefinition tail_tip = tail.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                        .texOffs(181, 93).addBox(-1.8F, -0.3F, -0.2F, 3.6F, 3.5F, 2.4F)
                        .texOffs(0, 123).addBox(-3.2F, 0.4F, 0.2F, 1.6F, 2.4F, 1.4F)
                        .texOffs(81, 123).addBox(1.6F, 0.4F, 0.2F, 1.6F, 2.4F, 1.4F)
                , PartPose.offset(0F, 10.6F, 2.6F));
        PartDefinition right_front_leg = root.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
                        .texOffs(160, 102).addBox(-1F, -6.5F, -2.1F, 2.8F, 1.69F, 3.8F)
                        .texOffs(202, 72).addBox(-1.6F, -4.81F, -2.6F, 4F, 3.12F, 4.8F)
                        .texOffs(175, 102).addBox(-1.12F, -1.69F, -2.2F, 3.04F, 1.69F, 4F)
                        .texOffs(221, 72).addBox(-1.3F, -2.5F, -2.2F, 3.4F, 4.4F, 4F)
                , PartPose.offsetAndRotation(-3.8F, 7.5F, -11F, rad(0), rad(0), rad(4)));
        PartDefinition right_front_shin = right_front_leg.addOrReplaceChild("right_front_shin", CubeListBuilder.create()
                        .texOffs(232, 83).addBox(-0.8F, 0F, -1.8F, 2.4F, 3.5F, 3.2F)
                        .texOffs(196, 93).addBox(-0.6F, 3.5F, -1.6F, 2F, 3.4F, 2.8F)
                , PartPose.offset(0F, 0F, 0F));
        PartDefinition right_front_hoof = right_front_shin.addOrReplaceChild("right_front_hoof", CubeListBuilder.create()
                        .texOffs(192, 102).addBox(-0.7F, 0F, -1.8F, 2.2F, 2.8F, 2.8F)
                        .texOffs(205, 102).addBox(-0.4F, 2.6F, -1.6F, 1.6F, 2.4F, 2.4F)
                        .texOffs(159, 61).addBox(-0.7F, 4.8F, -2.6F, 1.1F, 4.8F, 4.2F)
                        .texOffs(174, 61).addBox(0.4F, 4.8F, -2.6F, 1.1F, 4.8F, 4.2F)
                , PartPose.offset(0F, 6.9F, 0F));
        PartDefinition right_hind_leg = root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
                        .texOffs(216, 102).addBox(-1F, -6.9F, -1.9F, 2.8F, 1.79F, 3.8F)
                        .texOffs(0, 83).addBox(-1.6F, -5.11F, -2.4F, 4F, 3.32F, 4.8F)
                        .texOffs(231, 102).addBox(-1.12F, -1.79F, -2F, 3.04F, 1.79F, 4F)
                        .texOffs(19, 83).addBox(-1.3F, -2.5F, -2F, 3.2F, 4.4F, 4F)
                , PartPose.offsetAndRotation(-3.8F, 7.5F, 10F, rad(0), rad(0), rad(3)));
        PartDefinition right_hind_shin = right_hind_leg.addOrReplaceChild("right_hind_shin", CubeListBuilder.create()
                        .texOffs(0, 93).addBox(-0.8F, 0F, -1.6F, 2.4F, 3.5F, 3.2F)
                        .texOffs(209, 93).addBox(-0.6F, 3.5F, -1.4F, 2F, 3.4F, 2.8F)
                , PartPose.offset(0F, 0F, 0F));
        PartDefinition right_hind_hoof = right_hind_shin.addOrReplaceChild("right_hind_hoof", CubeListBuilder.create()
                        .texOffs(0, 110).addBox(-0.7F, 0F, -1.4F, 2.2F, 2.8F, 2.8F)
                        .texOffs(13, 110).addBox(-0.4F, 2.6F, -1.2F, 1.6F, 2.4F, 2.4F)
                        .texOffs(189, 61).addBox(-0.7F, 4.8F, -2.1F, 1.1F, 4.8F, 4.2F)
                        .texOffs(204, 61).addBox(0.4F, 4.8F, -2.1F, 1.1F, 4.8F, 4.2F)
                , PartPose.offset(0F, 6.9F, 0F));
        PartDefinition left_front_leg = root.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
                        .texOffs(61, 110).addBox(-1.8F, -6.5F, -2.1F, 2.8F, 1.69F, 3.8F)
                        .texOffs(36, 83).addBox(-2.4F, -4.81F, -2.6F, 4F, 3.12F, 4.8F)
                        .texOffs(76, 110).addBox(-1.92F, -1.69F, -2.2F, 3.04F, 1.69F, 4F)
                        .texOffs(55, 83).addBox(-2.1F, -2.5F, -2.2F, 3.4F, 4.4F, 4F)
                , PartPose.offsetAndRotation(3.8F, 7.5F, -11F, rad(0), rad(0), rad(-4)));
        PartDefinition left_front_shin = left_front_leg.addOrReplaceChild("left_front_shin", CubeListBuilder.create()
                        .texOffs(39, 93).addBox(-1.6F, 0F, -1.8F, 2.4F, 3.5F, 3.2F)
                        .texOffs(222, 93).addBox(-1.4F, 3.5F, -1.6F, 2F, 3.4F, 2.8F)
                , PartPose.offset(0F, 0F, 0F));
        PartDefinition left_front_hoof = left_front_shin.addOrReplaceChild("left_front_hoof", CubeListBuilder.create()
                        .texOffs(93, 110).addBox(-1.5F, 0F, -1.8F, 2.2F, 2.8F, 2.8F)
                        .texOffs(106, 110).addBox(-1.2F, 2.6F, -1.6F, 1.6F, 2.4F, 2.4F)
                        .texOffs(30, 72).addBox(-0.4F, 4.8F, -2.6F, 1.1F, 4.8F, 4.2F)
                        .texOffs(45, 72).addBox(-1.5F, 4.8F, -2.6F, 1.1F, 4.8F, 4.2F)
                , PartPose.offset(0F, 6.9F, 0F));
        PartDefinition left_hind_leg = root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
                        .texOffs(117, 110).addBox(-1.8F, -6.9F, -1.9F, 2.8F, 1.79F, 3.8F)
                        .texOffs(72, 83).addBox(-2.4F, -5.11F, -2.4F, 4F, 3.32F, 4.8F)
                        .texOffs(132, 110).addBox(-1.92F, -1.79F, -2F, 3.04F, 1.79F, 4F)
                        .texOffs(91, 83).addBox(-1.9F, -2.5F, -2F, 3.2F, 4.4F, 4F)
                , PartPose.offsetAndRotation(3.8F, 7.5F, 10F, rad(0), rad(0), rad(-3)));
        PartDefinition left_hind_shin = left_hind_leg.addOrReplaceChild("left_hind_shin", CubeListBuilder.create()
                        .texOffs(54, 93).addBox(-1.6F, 0F, -1.6F, 2.4F, 3.5F, 3.2F)
                        .texOffs(235, 93).addBox(-1.4F, 3.5F, -1.4F, 2F, 3.4F, 2.8F)
                , PartPose.offset(0F, 0F, 0F));
        PartDefinition left_hind_hoof = left_hind_shin.addOrReplaceChild("left_hind_hoof", CubeListBuilder.create()
                        .texOffs(149, 110).addBox(-1.5F, 0F, -1.4F, 2.2F, 2.8F, 2.8F)
                        .texOffs(162, 110).addBox(-1.2F, 2.6F, -1.2F, 1.6F, 2.4F, 2.4F)
                        .texOffs(60, 72).addBox(-0.4F, 4.8F, -2.1F, 1.1F, 4.8F, 4.2F)
                        .texOffs(75, 72).addBox(-1.5F, 4.8F, -2.1F, 1.1F, 4.8F, 4.2F)
                , PartPose.offset(0F, 6.9F, 0F));

        return LayerDefinition.create(mesh, 256, 256);
    }

    /**
     * Per-entity skill clock: the entity only syncs *which* animation is playing, so the model keeps
     * its own start tick per entity to drive windup / strike / recover timing. Keyed by entity id and
     * cleared when it grows past a small bound.
     */
    private final java.util.Map<Integer, int[]> skillClocks = new java.util.HashMap<>();

    private static float ease(float t) {
        float c = Mth.clamp(t, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }

    /** Eased progress of one stage of a skill between two normalised times. */
    private static float stage(float p, float from, float to) {
        if (p <= from) {
            return 0.0F;
        }
        if (p >= to) {
            return 1.0F;
        }
        return ease((p - from) / (to - from));
    }

    /** Ticks elapsed since {@code animation} started on {@code entity}. */
    private int animationTicks(MoonDeerEntity entity, NMACombatAnimation animation) {
        int id = entity.getId();
        int ordinal = animation.ordinal();
        int[] state = this.skillClocks.get(id);
        if (state == null || state[0] != ordinal) {
            state = new int[]{ordinal, entity.tickCount};
            if (this.skillClocks.size() > 64) {
                this.skillClocks.clear();
            }
            this.skillClocks.put(id, state);
        }
        return Math.max(0, entity.tickCount - state[1]);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(MoonDeerEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        applyLook(netHeadYaw, headPitch);
        applyStride(limbSwing, limbSwingAmount, ageInTicks);
        applyIdleLife(ageInTicks, limbSwingAmount);

        if (entity.hurtTime > 0) {
            applyFlinch(entity.hurtTime / 10.0F, ageInTicks);
        }

        if (entity.deathTime > 0) {
            applyDeathPose(entity.deathTime + (ageInTicks % 1.0F));
        } else {
            applyCombatPose(entity, ageInTicks, limbSwingAmount);
        }
    }

    /** Gaze: the skull takes most of it, the upper neck shares the turn so the throat cannot shear. */
    private void applyLook(float netHeadYaw, float headPitch) {
        this.head.yRot += netHeadYaw * DEG * 0.72F;
        this.head.xRot += headPitch * DEG * 0.55F;
        this.neck_upper.yRot += netHeadYaw * DEG * 0.28F;
        this.neck.yRot += netHeadYaw * DEG * 0.10F;
    }

    /**
     * Four-beat walk: each leg folds its shin as it swings forward, the hoof levels on contact and the
     * torso both bobs (twice per cycle) and rolls (once), with the neck, mane and tail trailing behind.
     */
    private void applyStride(float limbSwing, float limbSwingAmount, float ageInTicks) {
        if (limbSwingAmount <= 1.0E-4F) {
            return;
        }
        float speed = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float phase = Mth.cos(limbSwing * 0.66F);
        float opposite = Mth.cos(limbSwing * 0.66F + Mth.PI);
        float swing = 0.82F * speed;

        swingLeg(this.left_front_leg, this.left_front_shin, this.left_front_hoof, phase, swing, 1.25F);
        swingLeg(this.right_hind_leg, this.right_hind_shin, this.right_hind_hoof, phase * 0.92F, swing, 1.10F);
        swingLeg(this.right_front_leg, this.right_front_shin, this.right_front_hoof, opposite, swing, 1.10F);
        swingLeg(this.left_hind_leg, this.left_hind_shin, this.left_hind_hoof, opposite * 0.92F, swing, 1.25F);

        float bob = Math.abs(Mth.cos(limbSwing * 1.32F)) * 0.75F * speed;
        float roll = Mth.cos(limbSwing * 0.66F) * 0.05F * speed;

        this.body.y -= bob;
        this.body.zRot += roll;
        this.body.xRot += bob * 0.025F;
        this.chest.xRot += bob * 0.02F + roll * 0.3F;
        this.barrel.xRot -= bob * 0.012F;
        this.haunch.xRot -= bob * 0.02F;
        this.rump.xRot += bob * 0.012F;

        // neck counter-nods, the head stays comparatively level
        this.neck.xRot -= bob * 0.055F;
        this.neck.yRot -= roll * 0.8F;
        this.neck_upper.xRot += bob * 0.03F;
        this.head.xRot += bob * 0.02F;
        this.head.yRot -= roll * 0.6F;

        // crest, mane and tail lag the torso
        this.withers.y -= bob * 0.2F;
        this.mane_a.xRot += bob * 0.10F;
        this.mane_b.xRot += bob * 0.08F;
        this.mane_c.xRot += bob * 0.06F;
        this.mane_d.xRot += bob * 0.05F;
        this.mane_e.xRot += bob * 0.04F;
        this.tail.xRot -= bob * 0.06F;
        this.tail.yRot -= roll * 1.6F;
        this.tail_tip.yRot -= roll * 1.2F;
        this.tail_tip.xRot += bob * 0.05F;
    }

    /**
     * One leg of the gait: the thigh swings, the shin folds only while the leg travels forward, and the
     * hoof counter-rotates so the sole stays roughly level through the stance.
     */
    private static void swingLeg(ModelPart thigh, ModelPart shin, ModelPart hoof, float phase, float amount, float foldScale) {
        thigh.xRot += phase * amount;
        float forward = Math.max(0.0F, -phase);
        float back = Math.max(0.0F, phase);
        float fold = forward * amount * foldScale;
        shin.xRot -= fold;
        hoof.xRot += fold * 0.62F + back * amount * 0.12F;
    }

    /** Breathing, weight shift, ear flicks, a slow head drift, mane ripple and a swishing tail. */
    private void applyIdleLife(float ageInTicks, float limbSwingAmount) {
        float idle = 1.0F - Mth.clamp(limbSwingAmount * 1.5F, 0.0F, 1.0F);
        if (idle <= 0.0F) {
            return;
        }
        float breath = Mth.cos(ageInTicks * 0.095F);
        float breathFast = Mth.cos(ageInTicks * 0.19F);
        float sway = Mth.cos(ageInTicks * 0.041F);
        float drift = Mth.cos(ageInTicks * 0.021F);

        this.chest.xRot -= breath * 0.022F * idle;
        this.barrel.xRot += breath * 0.026F * idle;
        this.haunch.xRot += breath * 0.015F * idle;
        this.rump.xRot -= breath * 0.012F * idle;
        this.withers.y -= breath * 0.28F * idle;
        this.rune.y -= (breath * 0.5F + 0.5F) * 0.10F * idle;
        this.body.zRot += sway * 0.022F * idle;
        this.body.y += breathFast * 0.10F * idle;

        // head drift + a long look-around cycle
        this.neck.xRot += breath * 0.030F * idle;
        this.neck.yRot += (sway * 0.05F + drift * 0.06F) * idle;
        this.neck_upper.xRot -= breath * 0.020F * idle;
        this.head.xRot += Mth.cos(ageInTicks * 0.085F + 1.1F) * 0.026F * idle;
        this.head.yRot += drift * 0.10F * idle;

        // ears flick on their own clocks
        this.ear_left.zRot += Mth.cos(ageInTicks * 0.23F) * 0.20F * idle;
        this.ear_right.zRot -= Mth.cos(ageInTicks * 0.19F + 0.7F) * 0.20F * idle;
        this.ear_left.xRot += Mth.sin(ageInTicks * 0.13F) * 0.12F * idle;
        this.ear_right.xRot += Mth.sin(ageInTicks * 0.11F + 1.3F) * 0.12F * idle;

        // crown lag + blossom jiggle
        float antlerSway = Mth.cos(ageInTicks * 0.048F + 0.4F) * 0.032F * idle;
        this.antler_left.zRot += antlerSway;
        this.antler_right.zRot -= antlerSway;
        this.antler_left_mid.zRot -= antlerSway * 1.5F;
        this.antler_right_mid.zRot += antlerSway * 1.5F;
        this.antler_left_tip.zRot += antlerSway * 2.0F;
        this.antler_right_tip.zRot -= antlerSway * 2.0F;

        float jiggle = Mth.cos(ageInTicks * 0.17F) * 0.13F * idle;
        this.antler_left_flower.yRot += jiggle;
        this.antler_right_flower.yRot -= jiggle;
        this.antler_left_flower_low.zRot -= jiggle * 0.6F;
        this.antler_right_flower_low.zRot += jiggle * 0.6F;

        // travelling ripple down the ice blades, neck first
        ModelPart[] blades = {
                this.mane_neck_high, this.mane_neck_low, this.mane_a, this.mane_b,
                this.mane_c, this.mane_d, this.mane_e
        };
        for (int i = 0; i < blades.length; i++) {
            float wave = ageInTicks * 0.11F - i * 0.55F;
            blades[i].xRot += Mth.cos(wave) * 0.10F * idle;
            blades[i].zRot += Mth.sin(wave * 0.8F + i) * 0.09F * idle;
        }

        this.tail.xRot += Mth.cos(ageInTicks * 0.115F) * 0.06F * idle;
        this.tail.yRot += Mth.sin(ageInTicks * 0.10F) * 0.30F * idle;
        this.tail_tip.xRot += Mth.cos(ageInTicks * 0.14F - 0.4F) * 0.09F * idle;
        this.tail_tip.yRot += Mth.sin(ageInTicks * 0.10F - 0.7F) * 0.38F * idle;
    }

    /** Flinch away from a hit, with a short shake that decays over ten ticks. */
    private void applyFlinch(float amount, float ageInTicks) {
        float f = Mth.clamp(amount, 0.0F, 1.0F);
        float shake = Mth.sin(ageInTicks * 1.7F) * f;
        this.body.xRot -= 0.07F * f;
        this.body.zRot += 0.06F * shake;
        this.body.y += 0.35F * f;
        this.neck.xRot += 0.18F * f;
        this.neck_upper.xRot += 0.12F * f;
        this.head.xRot -= 0.24F * f;
        this.head.yRot += 0.14F * shake;
        this.ear_left.zRot += 0.30F * f;
        this.ear_right.zRot -= 0.30F * f;
        this.rune.y += 0.16F * f;
        this.tail.xRot += 0.24F * f;
        for (ModelPart blade : new ModelPart[]{this.mane_a, this.mane_b, this.mane_c, this.mane_d, this.mane_e}) {
            blade.xRot += 0.26F * f;
        }
        this.left_front_leg.xRot -= 0.20F * f;
        this.right_hind_leg.xRot += 0.18F * f;
    }

    /** Stagger, then the legs fold in sequence and the head comes down last. */
    private void applyDeathPose(float deathTicks) {
        float t = Mth.clamp(deathTicks / 30.0F, 0.0F, 1.0F);
        float front = ease(Mth.clamp(t / 0.55F, 0.0F, 1.0F));
        float back = ease(Mth.clamp((t - 0.2F) / 0.55F, 0.0F, 1.0F));
        float head = ease(Mth.clamp((t - 0.35F) / 0.6F, 0.0F, 1.0F));
        float fall = ease(t);

        this.body.y += fall * 5.0F;
        this.body.xRot += fall * 0.16F;
        this.body.zRot += Mth.sin(deathTicks * 0.45F) * 0.06F * (1.0F - t);

        this.left_front_leg.xRot -= front * 1.15F;
        this.right_front_leg.xRot -= front * 0.95F;
        this.left_front_shin.xRot -= front * 1.45F;
        this.right_front_shin.xRot -= front * 1.30F;
        this.left_front_hoof.xRot += front * 0.85F;
        this.right_front_hoof.xRot += front * 0.75F;

        this.left_hind_leg.xRot += back * 1.05F;
        this.right_hind_leg.xRot += back * 1.20F;
        this.left_hind_shin.xRot -= back * 1.20F;
        this.right_hind_shin.xRot -= back * 1.35F;
        this.left_hind_hoof.xRot += back * 0.70F;
        this.right_hind_hoof.xRot += back * 0.80F;

        this.neck.xRot += head * 0.75F;
        this.neck_upper.xRot += head * 0.30F;
        this.head.xRot += head * 0.55F;
        this.tail.xRot -= fall * 0.60F;
        this.tail_tip.xRot += fall * 0.45F;

        for (int i = 0; i < 5; i++) {
            ModelPart blade = new ModelPart[]{this.mane_a, this.mane_b, this.mane_c, this.mane_d, this.mane_e}[i];
            blade.xRot += fall * (0.28F + i * 0.05F);
        }
        this.antler_left.zRot -= fall * 0.18F;
        this.antler_right.zRot += fall * 0.18F;
    }


    /** Per-skill body language, staged as windup 闁?strike 闁?recovery off the synced animation id. */
    private void applyCombatPose(MoonDeerEntity entity, float ageInTicks, float limbSwingAmount) {
        NMACombatAnimation animation = entity.getCombatAnimation();
        if (animation == null || animation == NMACombatAnimation.IDLE) {
            return;
        }
        int elapsed = animationTicks(entity, animation);
        float p;
        switch (animation) {
            case DASH -> {
                p = Mth.clamp(elapsed / 36.0F, 0.0F, 1.0F);
                float wind = stage(p, 0.0F, 0.30F);
                float thrust = stage(p, 0.28F, 0.45F) - stage(p, 0.62F, 1.0F);
                // gather, then flatten out and drive forward
                this.body.xRot += wind * -0.10F + thrust * 0.30F;
                this.body.y += wind * 0.8F + thrust * 1.1F;
                this.neck.xRot += wind * 0.28F - thrust * 0.46F;
                this.neck_upper.xRot += wind * 0.20F - thrust * 0.34F;
                this.head.xRot += -wind * 0.22F + thrust * 0.46F;
                this.left_front_leg.xRot += wind * 0.45F - thrust * 1.25F;
                this.left_front_shin.xRot -= thrust * 0.55F;
                this.right_front_leg.xRot += wind * 0.35F - thrust * 0.95F;
                this.right_front_shin.xRot -= thrust * 0.40F;
                this.left_hind_leg.xRot -= wind * 0.30F - thrust * 1.15F;
                this.right_hind_leg.xRot -= wind * 0.25F - thrust * 1.00F;
                this.tail.xRot -= thrust * 0.45F;
                for (ModelPart blade : new ModelPart[]{this.mane_a, this.mane_b, this.mane_c, this.mane_d, this.mane_e}) {
                    blade.xRot += thrust * 0.35F;
                }
            }
            case RING -> {
                p = Mth.clamp(elapsed / 40.0F, 0.0F, 1.0F);
                float rear = stage(p, 0.0F, 0.38F) - stage(p, 0.70F, 1.0F);
                float slam = stage(p, 0.62F, 0.80F) - stage(p, 0.88F, 1.0F);
                this.body.xRot -= rear * 0.42F;
                this.body.y -= rear * 1.6F + slam * 1.2F;
                this.neck.xRot += rear * 0.55F - slam * 0.30F;
                this.neck_upper.xRot += rear * 0.18F;
                this.head.xRot -= rear * 0.38F + slam * 0.20F;
                this.left_front_leg.xRot -= rear * 1.65F - slam * 0.9F;
                this.left_front_shin.xRot -= rear * 1.20F;
                this.right_front_leg.xRot -= rear * 1.50F - slam * 0.8F;
                this.right_front_shin.xRot -= rear * 1.05F;
                this.left_hind_leg.xRot += rear * 0.36F;
                this.right_hind_leg.xRot += rear * 0.28F;
                this.left_hind_shin.xRot -= rear * 0.24F;
                this.right_hind_shin.xRot -= rear * 0.18F;
                this.tail.xRot -= rear * 0.60F;
                this.tail_tip.xRot -= rear * 0.35F;
                this.antler_left.zRot -= rear * 0.16F;
                this.antler_right.zRot += rear * 0.16F;
                for (ModelPart blade : new ModelPart[]{this.mane_a, this.mane_b, this.mane_c, this.mane_d, this.mane_e}) {
                    blade.xRot -= rear * 0.28F;
                }
            }
            case BARRAGE -> {
                p = Mth.clamp(elapsed / 28.0F, 0.0F, 1.0F);
                float ramp = stage(p, 0.0F, 0.18F) * (1.0F - stage(p, 0.80F, 1.0F));
                // three head punches, each with its own snap
                float beat = Mth.cos(elapsed * 0.42F);
                float punch = Math.max(0.0F, beat) * ramp;
                this.body.xRot += punch * 0.12F;
                this.body.y += punch * 0.5F;
                this.neck.xRot += punch * 0.55F;
                this.neck_upper.xRot -= punch * 0.30F;
                this.head.xRot -= punch * 0.32F;
                this.withers.y -= punch * 0.5F;
                this.rune.y -= punch * 0.4F;
                this.left_front_leg.xRot -= punch * 0.18F;
                this.right_front_leg.xRot -= punch * 0.14F;
                this.tail_tip.xRot += punch * 0.30F;
            }
            case BEAM -> {
                p = Mth.clamp(elapsed / 60.0F, 0.0F, 1.0F);
                float brace = stage(p, 0.0F, 0.32F) * (1.0F - stage(p, 0.80F, 1.0F));
                float charge = Mth.sin(elapsed * 0.35F) * 0.03F * brace;
                this.body.xRot += brace * 0.20F;
                this.body.y += brace * 1.7F;
                this.neck.xRot += brace * 0.24F + charge;
                this.neck_upper.xRot -= brace * 0.30F;
                this.head.xRot += brace * 0.18F - charge;
                this.left_front_leg.zRot -= brace * 0.14F;
                this.right_front_leg.zRot += brace * 0.14F;
                this.left_hind_leg.zRot -= brace * 0.11F;
                this.right_hind_leg.zRot += brace * 0.11F;
                this.left_front_shin.xRot -= brace * 0.34F;
                this.right_front_shin.xRot -= brace * 0.34F;
                this.left_hind_shin.xRot -= brace * 0.20F;
                this.right_hind_shin.xRot -= brace * 0.20F;
                for (ModelPart blade : new ModelPart[]{this.mane_a, this.mane_b, this.mane_c, this.mane_d, this.mane_e}) {
                    blade.xRot += brace * 0.30F;
                }
            }
            case MOON_RAIN -> {
                p = Mth.clamp(elapsed / 70.0F, 0.0F, 1.0F);
                float up = stage(p, 0.0F, 0.40F) * (1.0F - stage(p, 0.82F, 1.0F));
                float pulse = Mth.cos(elapsed * 0.12F) * up;
                this.body.xRot -= up * 0.18F;
                this.body.y -= up * 0.7F;
                this.neck.xRot -= up * 0.95F + pulse * 0.04F;
                this.neck_upper.xRot += up * 0.30F;
                this.head.xRot -= up * 0.60F;
                this.antler_left.zRot -= up * 0.22F;
                this.antler_right.zRot += up * 0.22F;
                this.antler_left_mid.zRot += up * 0.10F;
                this.antler_right_mid.zRot -= up * 0.10F;
                this.tail.xRot -= up * 0.75F;
                this.tail_tip.xRot -= up * 0.50F;
                this.left_front_leg.zRot += up * 0.08F;
                this.right_front_leg.zRot -= up * 0.08F;
                for (ModelPart blade : new ModelPart[]{this.mane_a, this.mane_b, this.mane_c, this.mane_d, this.mane_e}) {
                    blade.xRot -= up * 0.22F;
                }
            }
            case METEOR -> {
                // 0..0.5 wind up tall, 0.5..0.62 slam, then recover
                p = Mth.clamp(elapsed / 55.0F, 0.0F, 1.0F);
                float up = stage(p, 0.0F, 0.42F) * (1.0F - stage(p, 0.46F, 0.56F));
                float down = stage(p, 0.46F, 0.62F) * (1.0F - stage(p, 0.80F, 1.0F));
                this.body.xRot -= up * 0.38F - down * 0.40F;
                this.body.y += up * 2.0F - down * 2.6F;
                this.neck.xRot += up * 0.80F + down * 0.30F;
                this.neck_upper.xRot -= up * 0.34F - down * 0.20F;
                this.head.xRot -= up * 0.35F - down * 0.55F;
                this.left_front_leg.xRot -= up * 1.70F - down * 0.55F;
                this.right_front_leg.xRot -= up * 1.55F - down * 0.45F;
                this.left_front_shin.xRot -= up * 1.00F + down * 0.20F;
                this.right_front_shin.xRot -= up * 0.95F + down * 0.20F;
                this.left_hind_leg.xRot += up * 0.45F - down * 0.32F;
                this.right_hind_leg.xRot += up * 0.40F - down * 0.26F;
                this.tail.xRot -= up * 0.65F - down * 0.40F;
                for (ModelPart blade : new ModelPart[]{this.mane_a, this.mane_b, this.mane_c, this.mane_d, this.mane_e}) {
                    blade.xRot += up * 0.30F + down * 0.25F;
                }
            }
            case SHADOW -> {
                p = Mth.clamp(elapsed / 70.0F, 0.0F, 1.0F);
                float sink = stage(p, 0.0F, 0.35F) * (1.0F - stage(p, 0.78F, 1.0F));
                float creep = Mth.sin(elapsed * 0.10F) * 0.02F * sink;
                this.body.y += sink * 2.6F;
                this.body.xRot += sink * 0.12F + creep;
                this.neck.xRot -= sink * 0.22F;
                this.neck_upper.xRot += sink * 0.12F;
                this.head.xRot += sink * 0.14F;
                this.left_front_leg.xRot -= sink * 0.70F;
                this.right_front_leg.xRot -= sink * 0.70F;
                this.left_front_shin.xRot -= sink * 1.00F;
                this.right_front_shin.xRot -= sink * 1.00F;
                this.left_hind_leg.xRot += sink * 0.75F;
                this.right_hind_leg.xRot += sink * 0.75F;
                this.left_hind_shin.xRot -= sink * 0.90F;
                this.right_hind_shin.xRot -= sink * 0.90F;
                this.tail.xRot += sink * 0.60F;
                this.tail_tip.xRot += sink * 0.35F;
                for (ModelPart blade : new ModelPart[]{this.mane_a, this.mane_b, this.mane_c, this.mane_d, this.mane_e}) {
                    blade.xRot += sink * 1.25F;
                }
                this.antler_left.zRot -= sink * 0.35F;
                this.antler_right.zRot += sink * 0.35F;
            }
            case MOON_DUST -> {
                p = Mth.clamp(elapsed / 30.0F, 0.0F, 1.0F);
                float plant = stage(p, 0.0F, 0.30F) * (1.0F - stage(p, 0.82F, 1.0F));
                float sweep = Mth.sin(elapsed * 0.30F) * plant;
                this.body.y += plant * 0.9F;
                this.body.xRot += plant * 0.22F;
                this.neck.xRot += plant * 0.40F;
                this.neck_upper.xRot -= plant * 0.40F;
                this.head.xRot += plant * 0.22F;
                this.head.yRot += sweep * 0.26F;
                this.antler_left.zRot += plant * 0.34F + sweep * 0.10F;
                this.antler_right.zRot -= plant * 0.34F + sweep * 0.10F;
                this.left_front_leg.xRot -= plant * 0.60F;
                this.right_front_leg.xRot -= plant * 0.60F;
                this.left_front_shin.xRot -= plant * 0.60F;
                this.right_front_shin.xRot -= plant * 0.60F;
                this.left_hind_leg.xRot += plant * 0.50F;
                this.right_hind_leg.xRot += plant * 0.50F;
                this.tail.xRot -= plant * 0.35F;
                for (ModelPart blade : new ModelPart[]{this.mane_a, this.mane_b, this.mane_c, this.mane_d, this.mane_e}) {
                    blade.xRot += plant * 0.36F;
                }
            }
            default -> {
                // nothing extra
            }
        }
        this.rune.y += Mth.sin(ageInTicks * 0.15F) * 0.05F;
    }

    /** Exposed for render layers that want to hang effects off the crown. */
    public ModelPart getHead() {
        return this.head;
    }

    public ModelPart getLeftAntlerTip() {
        return this.antler_left_tip;
    }

    public ModelPart getRightAntlerTip() {
        return this.antler_right_tip;
    }
}
