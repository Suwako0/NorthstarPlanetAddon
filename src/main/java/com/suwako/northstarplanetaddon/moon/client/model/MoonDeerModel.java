package com.suwako.northstarplanetaddon.moon.client.model;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.entity.MoonDeerEntity;
import com.suwako.northstarplanetaddon.moon.entity.NMACombatAnimation;
import net.minecraft.client.model.HierarchicalModel;
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
 * 梦眠月鹿 model. A quadruped a fifth taller than a horse at the shoulder, built from the same
 * builder API vanilla uses.
 * <p>
 * <b>Torso.</b> The body is not one box: it is an empty {@code body} pivot carrying four shaped
 * segments, so the deer reads as an animal rather than a cuboid and each segment can be animated on
 * its own — a raised {@code withers} hump over the shoulders, a broader {@code chest}, a narrower
 * {@code barrel} that gives the flank its tuck, and a fuller {@code haunch} that carries the rump.
 * <p>
 * <b>Texture layout</b> follows the standard Minecraft box UV convention on a 64x64 sheet: for a box
 * of {@code w x h x d} at {@code texOffs(u, v)} the top and bottom faces share the upper strip
 * ({@code y = v}, {@code x = u + d}), the four side faces share the lower one ({@code y = v + d}),
 * and the {@code -Z} (front) face lands at {@code x = u + d}. No two boxes overlap:
 *
 * <pre>
 * legs       (16x18 each)  y  0..17   x   0, 16, 32, 48
 * head       (20x11)       y 18..28   x   0
 * neck       (16x10)       y 18..27   x  20
 * tail       (12x10)       y 18..27   x  36
 * ears x2    ( 6x 4)       y 18..21   x  48, 54
 * antlers x2 ( 8x 7)       y 22..28   x  48, 56
 * chest      (34x16)       y 32..47   x   0
 * withers    (26x 8)       y 32..39   x  34
 * barrel     (30x15)       y 48..62   x   0
 * haunch     (34x16)       y 48..63   x  30
 * </pre>
 *
 * Ground plane is model {@code y = 24}; the torso back sits at {@code y = 1} and the antler tips
 * reach {@code y = -11}, so the deer stands about 35 model units — a little over two blocks — tall.
 */
public class MoonDeerModel extends HierarchicalModel<MoonDeerEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MoonModule.NAMESPACE, "moon_deer"), "main");

    /** Shoulder height of the torso pivot, in model units. */
    private static final float TORSO_Y = 10.0F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart chest;
    private final ModelPart withers;
    private final ModelPart barrel;
    private final ModelPart haunch;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart leftAntler;
    private final ModelPart rightAntler;
    private final ModelPart tail;
    private final ModelPart leftHindLeg;
    private final ModelPart rightHindLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;

    public MoonDeerModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.chest = this.body.getChild("chest");
        this.withers = this.body.getChild("withers");
        this.barrel = this.body.getChild("barrel");
        this.haunch = this.body.getChild("haunch");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.leftAntler = this.head.getChild("left_antler");
        this.rightAntler = this.head.getChild("right_antler");
        this.tail = this.body.getChild("tail");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightFrontLeg = root.getChild("right_front_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(),
                PartPose.offset(0.0F, TORSO_Y, 0.0F));

        // Broad chest and shoulder, with a raised withers hump on top of it.
        body.addOrReplaceChild("chest", CubeListBuilder.create()
                        .texOffs(0, 32)
                        .addBox(-5.0F, -9.0F, -11.0F, 10.0F, 9.0F, 7.0F),
                PartPose.ZERO);

        body.addOrReplaceChild("withers", CubeListBuilder.create()
                        .texOffs(34, 32)
                        .addBox(-4.0F, -12.0F, -10.0F, 8.0F, 3.0F, 5.0F),
                PartPose.ZERO);

        // Narrower middle third: this is what gives the flank its tucked look.
        body.addOrReplaceChild("barrel", CubeListBuilder.create()
                        .texOffs(0, 48)
                        .addBox(-4.0F, -8.5F, -4.0F, 8.0F, 8.0F, 7.0F),
                PartPose.ZERO);

        // Fuller hindquarter carrying the rump and the tail root.
        body.addOrReplaceChild("haunch", CubeListBuilder.create()
                        .texOffs(30, 48)
                        .addBox(-5.0F, -9.0F, 3.0F, 10.0F, 9.0F, 7.0F),
                PartPose.ZERO);

        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                        .texOffs(20, 18)
                        .addBox(-2.0F, -6.0F, -2.0F, 4.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -8.0F, -10.0F, 0.35F, 0.0F, 0.0F));

        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 18)
                        .addBox(-2.0F, -2.5F, -6.0F, 4.0F, 5.0F, 6.0F),
                PartPose.offset(0.0F, -6.0F, -3.0F));

        head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                        .texOffs(48, 18)
                        .addBox(-1.0F, -3.0F, 0.0F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-1.5F, -2.5F, -2.5F, 0.0F, 0.3F, -0.2F));

        head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                        .texOffs(54, 18)
                        .addBox(-1.0F, -3.0F, 0.0F, 2.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(1.5F, -2.5F, -2.5F, 0.0F, -0.3F, 0.2F));

        head.addOrReplaceChild("left_antler", CubeListBuilder.create()
                        .texOffs(48, 22)
                        .addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(-1.5F, -2.5F, -4.0F, -0.15F, 0.35F, -0.3F));

        head.addOrReplaceChild("right_antler", CubeListBuilder.create()
                        .texOffs(56, 22)
                        .addBox(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
                PartPose.offsetAndRotation(1.5F, -2.5F, -4.0F, -0.15F, -0.35F, 0.3F));

        body.addOrReplaceChild("tail", CubeListBuilder.create()
                        .texOffs(36, 18)
                        .addBox(-1.5F, 0.0F, 0.0F, 3.0F, 7.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -4.0F, 11.0F, 0.6F, 0.0F, 0.0F));

        root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 14.0F, 4.0F),
                PartPose.offset(3.0F, 10.0F, 8.0F));

        root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create()
                        .texOffs(16, 0)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 14.0F, 4.0F),
                PartPose.offset(-3.0F, 10.0F, 8.0F));

        root.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 14.0F, 4.0F),
                PartPose.offset(3.0F, 10.0F, -9.0F));

        root.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
                        .texOffs(48, 0)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 14.0F, 4.0F),
                PartPose.offset(-3.0F, 10.0F, -9.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(MoonDeerEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;

        float walk = Mth.cos(limbSwing * 0.6662F);
        float stride = 1.35F * limbSwingAmount;
        this.leftFrontLeg.xRot = walk * stride;
        this.rightHindLeg.xRot = walk * stride;
        this.rightFrontLeg.xRot = -walk * stride;
        this.leftHindLeg.xRot = -walk * stride;

        // Idle life: slow breathing through the torso, a shifting weight, ear flicks and a swishing
        // tail, all layered under whatever the walk cycle is doing.
        float breath = Mth.cos(ageInTicks * 0.11F);
        float sway = Mth.cos(ageInTicks * 0.05F);
        this.barrel.xRot = breath * 0.025F;
        this.chest.xRot = -breath * 0.02F;
        this.haunch.xRot = breath * 0.015F;
        this.withers.y = -breath * 0.3F;
        this.body.y = TORSO_Y - Math.abs(walk) * 0.45F * limbSwingAmount + breath * 0.2F;
        this.body.zRot = sway * 0.025F;
        this.neck.xRot = 0.35F + breath * 0.03F + stride * 0.22F;
        this.neck.yRot = sway * 0.06F;
        this.tail.yRot = Mth.cos(ageInTicks * 0.13F) * 0.28F;
        this.tail.xRot = 0.6F + limbSwingAmount * 0.35F;
        this.leftAntler.zRot = -0.3F + breath * 0.02F;
        this.rightAntler.zRot = 0.3F - breath * 0.02F;
        this.leftEar.zRot = -0.2F + Mth.cos(ageInTicks * 0.31F) * 0.12F;
        this.rightEar.zRot = 0.2F - Mth.cos(ageInTicks * 0.29F) * 0.12F;

        if (entity.hurtTime > 0) {
            float flinch = entity.hurtTime / 10.0F;
            this.body.xRot -= 0.07F * flinch;
            this.neck.xRot += 0.12F * flinch;
            this.head.xRot -= 0.14F * flinch;
            this.body.zRot += 0.06F * flinch;
        }

        if (entity.deathTime > 0) {
            applyDeathPose(entity.deathTime + (ageInTicks % 1.0F));
        } else {
            applyCombatPose(entity.getCombatAnimation(), ageInTicks, limbSwingAmount);
        }
    }

    /** Buckling legs, sagging torso and a drooping neck as the deer goes down. */
    private void applyDeathPose(float deathTicks) {
        float t = Mth.clamp(deathTicks / 20.0F, 0.0F, 1.0F);
        float ease = t * t;
        this.body.y = TORSO_Y + ease * 3.0F;
        this.body.xRot = ease * 0.12F;
        this.neck.xRot = 0.35F + ease * 0.55F;
        this.head.xRot = ease * 0.5F;
        this.tail.xRot = 0.6F - ease * 0.5F;
        this.leftFrontLeg.xRot = -ease * 0.9F;
        this.rightFrontLeg.xRot = -ease * 0.75F;
        this.leftHindLeg.xRot = ease * 0.8F;
        this.rightHindLeg.xRot = ease * 0.95F;
        this.leftFrontLeg.zRot = -ease * 0.35F;
        this.rightFrontLeg.zRot = ease * 0.35F;
    }

    /** Per-skill body language, driven by the animation id the entity syncs to clients. */
    private void applyCombatPose(NMACombatAnimation animation, float ageInTicks, float limbSwingAmount) {
        if (animation == null) {
            return;
        }
        switch (animation) {
            case DASH -> {
                // Flattened charge: nose down, antlers forward, legs stretched out of frame.
                this.body.xRot += 0.20F;
                this.body.y = TORSO_Y + 0.8F;
                this.neck.xRot = 0.12F;
                this.head.xRot = 0.22F;
                this.leftFrontLeg.xRot = -0.85F;
                this.rightFrontLeg.xRot = -0.55F;
                this.leftHindLeg.xRot = 0.95F;
                this.rightHindLeg.xRot = 0.75F;
                this.tail.xRot = 0.15F;
            }
            case RING -> {
                // Rears back, front hooves off the ground, head thrown up.
                this.body.xRot -= 0.38F;
                this.body.y = TORSO_Y - 0.6F;
                this.neck.xRot = 0.85F;
                this.head.xRot = -0.35F;
                this.leftFrontLeg.xRot = -1.35F;
                this.rightFrontLeg.xRot = -1.25F;
                this.leftHindLeg.xRot = 0.30F;
                this.rightHindLeg.xRot = 0.22F;
                this.tail.xRot = -0.25F;
            }
            case BARRAGE -> {
                // Rapid head thrusts, each one a thrown moon shard.
                float thrust = Mth.sin(ageInTicks * 0.85F);
                this.body.xRot += 0.10F + thrust * 0.05F;
                this.neck.xRot = 0.30F + thrust * 0.45F;
                this.head.xRot = -thrust * 0.30F;
                this.withers.y = -0.4F - thrust * 0.4F;
            }
            case BEAM -> {
                // Braced low, antlers levelled at the target.
                this.body.xRot += 0.16F;
                this.body.y = TORSO_Y + 1.4F;
                this.neck.xRot = 0.72F;
                this.head.xRot = 0.16F;
                this.leftFrontLeg.zRot = -0.12F;
                this.rightFrontLeg.zRot = 0.12F;
                this.leftHindLeg.zRot = -0.10F;
                this.rightHindLeg.zRot = 0.10F;
            }
            case MOON_RAIN -> {
                // 拜月: muzzle to the sky while the rain falls.
                this.body.xRot -= 0.14F;
                this.body.y = TORSO_Y - 0.4F;
                this.neck.xRot = -0.50F + Mth.cos(ageInTicks * 0.1F) * 0.04F;
                this.head.xRot = -0.42F;
                this.leftAntler.zRot = -0.15F;
                this.rightAntler.zRot = 0.15F;
                this.tail.xRot = 0.95F;
            }
            case METEOR -> {
                // Winds up tall, then slams down: the whole body drives the impact.
                float cycle = Mth.sin(ageInTicks * 0.22F);
                float up = Mth.clamp(cycle, 0.0F, 1.0F);
                float down = Mth.clamp(-cycle, 0.0F, 1.0F);
                this.body.xRot -= 0.30F * up + 0.34F * down;
                this.body.y = TORSO_Y - 1.6F * up + 2.2F * down;
                this.neck.xRot = 0.35F + 0.75F * up + 0.35F * down;
                this.head.xRot = -0.30F * up + 0.45F * down;
                this.leftFrontLeg.xRot = -1.5F * up + 0.5F * down;
                this.rightFrontLeg.xRot = -1.4F * up + 0.4F * down;
                this.leftHindLeg.xRot = 0.4F * up - 0.25F * down;
                this.rightHindLeg.xRot = 0.35F * up - 0.2F * down;
            }
            case SHADOW -> {
                // Melts into the dark: crouched low and flattened.
                this.body.y = TORSO_Y + 2.0F;
                this.body.xRot += 0.10F;
                this.neck.xRot = 0.10F;
                this.head.xRot = 0.05F;
                this.leftFrontLeg.xRot = -0.55F;
                this.rightFrontLeg.xRot = -0.55F;
                this.leftHindLeg.xRot = 0.55F;
                this.rightHindLeg.xRot = 0.55F;
                this.tail.xRot = 0.0F;
            }
            case MOON_DUST -> {
                // A planted, low antler sweep: clear visual telegraph for the 0.2-second pull warning.
                float gather = Mth.sin(ageInTicks * 0.55F) * 0.04F;
                this.body.y = TORSO_Y + 0.55F;
                this.body.xRot += 0.18F;
                this.neck.xRot = 0.70F + gather;
                this.head.xRot = 0.20F;
                this.leftAntler.zRot = -0.48F + gather;
                this.rightAntler.zRot = 0.48F - gather;
                this.leftFrontLeg.xRot = -0.45F;
                this.rightFrontLeg.xRot = -0.45F;
                this.leftHindLeg.xRot = 0.35F;
                this.rightHindLeg.xRot = 0.35F;
                this.tail.xRot = 0.30F;
            }
            default -> {
                // IDLE / WALK: nothing extra on top of the idle and stride layers.
            }
        }
    }

    /** Exposed so a future boss renderer can hang effects off the antlers. */
    public ModelPart getHead() {
        return this.head;
    }

    public ModelPart getLeftAntler() {
        return this.leftAntler;
    }

    public ModelPart getRightAntler() {
        return this.rightAntler;
    }
}
