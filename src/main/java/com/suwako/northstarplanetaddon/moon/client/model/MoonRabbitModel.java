package com.suwako.northstarplanetaddon.moon.client.model;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.client.animation.NMAAnimationResources;
import com.suwako.northstarplanetaddon.moon.entity.MoonRabbitEntity;
import net.minecraft.client.animation.AnimationDefinition;
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
 * 月兔 model. Built from the same builder API vanilla uses, with the skeleton laid out so the
 * keyframe data in {@link NMAAnimations} can drive it.
 * <p>
 * Texture layout follows the standard Minecraft box UV convention on a 64x64 sheet: for a box of
 * {@code w x h x d} placed at {@code texOffs(u, v)} the top and bottom faces share the upper strip
 * and the four side faces share the lower one, with the {@code -Z} (front) face landing at
 * {@code x = u + d}.
 * <p>
 * Ground plane is model {@code y = 24}; the ears reach {@code y = 12}, so the rabbit stands a little
 * under three quarters of a block tall while keeping the {@code 0.45 x 0.5} hitbox.
 */
public class MoonRabbitModel extends HierarchicalModel<MoonRabbitEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MoonModule.NAMESPACE, "moon_rabbit"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftHaunch;
    private final ModelPart rightHaunch;
    private final ModelPart tail;

    public MoonRabbitModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.leftFrontLeg = this.body.getChild("left_front_leg");
        this.rightFrontLeg = this.body.getChild("right_front_leg");
        this.leftHaunch = this.body.getChild("left_haunch");
        this.rightHaunch = this.body.getChild("right_haunch");
        this.tail = this.body.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.5F, -3.0F, -4.0F, 5.0F, 5.0F, 8.0F),
                PartPose.offset(0.0F, 21.0F, 0.5F));

        // All visible limbs are children of the body. Keeping a real skeleton means a body pose
        // carries the head, paws and tail with it instead of leaving them behind when the rabbit
        // rises on its haunches.
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(28, 0)
                        .addBox(-2.0F, -3.5F, -3.5F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, -2.0F, -3.0F));

        head.addOrReplaceChild("left_ear", CubeListBuilder.create()
                        .texOffs(46, 0)
                        .addBox(-1.0F, -4.0F, -0.5F, 2.0F, 4.0F, 1.0F),
                PartPose.offset(-1.1F, -3.0F, -1.0F));

        head.addOrReplaceChild("right_ear", CubeListBuilder.create()
                        .texOffs(46, 6)
                        .addBox(-1.0F, -4.0F, -0.5F, 2.0F, 4.0F, 1.0F),
                PartPose.offset(1.1F, -3.0F, -1.0F));

        body.addOrReplaceChild("tail", CubeListBuilder.create()
                        .texOffs(0, 14)
                        .addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, -1.5F, 3.5F));

        body.addOrReplaceChild("left_front_leg", CubeListBuilder.create()
                        .texOffs(10, 14)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(-1.4F, -1.0F, -3.5F));

        body.addOrReplaceChild("right_front_leg", CubeListBuilder.create()
                        .texOffs(20, 14)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(1.4F, -1.0F, -3.5F));

        body.addOrReplaceChild("left_haunch", CubeListBuilder.create()
                        .texOffs(0, 22)
                        .addBox(-1.5F, 0.0F, -2.0F, 3.0F, 4.0F, 4.0F),
                PartPose.offset(-2.0F, -1.0F, 2.0F));

        body.addOrReplaceChild("right_haunch", CubeListBuilder.create()
                        .texOffs(16, 22)
                        .addBox(-1.5F, 0.0F, -2.0F, 3.0F, 4.0F, 4.0F),
                PartPose.offset(2.0F, -1.0F, 2.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(MoonRabbitEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;

        // The golden-carrot reaction and moon-gazing pose replace idle rather than stacking on top
        // of it, so their upright silhouettes stay clean. The cute action is resource-loaded from
        // entity_animations/moon_rabbit/golden_carrot_cute.json and can therefore be replaced by a resource pack.
        AnimationDefinition goldenCarrotEat = NMAAnimationResources.get(NMAAnimationResources.MOON_RABBIT_GOLDEN_CARROT_EAT);
        AnimationDefinition goldenCarrotCute = NMAAnimationResources.get(NMAAnimationResources.MOON_RABBIT_GOLDEN_CARROT);
        if (entity.goldenCarrotEatAnimationState.isStarted() && goldenCarrotEat != null) {
            this.animate(entity.goldenCarrotEatAnimationState, goldenCarrotEat, ageInTicks);
        } else if (entity.goldenCarrotCuteAnimationState.isStarted() && goldenCarrotCute != null) {
            this.animate(entity.goldenCarrotCuteAnimationState, goldenCarrotCute, ageInTicks);
        } else if (entity.gazeAnimationState.isStarted()) {
            this.animate(entity.gazeAnimationState, NMAAnimations.MOON_RABBIT_GAZE, ageInTicks);
        } else {
            this.animate(entity.idleAnimationState, NMAAnimations.MOON_RABBIT_IDLE, ageInTicks);
        }
        if (!entity.isGoldenCarrotCuteActionActive() && !entity.isEatingGoldenCarrot()) {
            this.animateWalk(NMAAnimations.MOON_RABBIT_HOP, limbSwing, limbSwingAmount, 2.5F, 2.0F);
        }
    }

    /** Exposed for future render layers that need to attach to the rabbit's head. */
    public ModelPart getHead() {
        return this.head;
    }

    public ModelPart getTail() {
        return this.tail;
    }

    public ModelPart getLeftFrontLeg() {
        return this.leftFrontLeg;
    }

    public ModelPart getRightFrontLeg() {
        return this.rightFrontLeg;
    }

    public ModelPart getLeftHaunch() {
        return this.leftHaunch;
    }

    public ModelPart getRightHaunch() {
        return this.rightHaunch;
    }
}
