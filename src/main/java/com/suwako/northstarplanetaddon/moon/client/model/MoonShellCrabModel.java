package com.suwako.northstarplanetaddon.moon.client.model;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.entity.MoonShellCrabEntity;
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
 * 月壳蟹 model — a low, wide, heavily armoured crab with a domed shell, two eye stalks and six legs.
 * <p>
 * Uses the standard Minecraft box UV convention on a 64x64 sheet; see {@link MoonRabbitModel} for
 * the layout note. The ground plane is model {@code y = 24} and the shell crest sits at
 * {@code y = 14.5}, so the crab is roughly 0.6 blocks tall.
 */
public class MoonShellCrabModel extends HierarchicalModel<MoonShellCrabEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MoonModule.NAMESPACE, "moon_shell_crab"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart shell;
    private final ModelPart shellTop;
    private final ModelPart leftClaw;
    private final ModelPart rightClaw;

    public MoonShellCrabModel(ModelPart root) {
        this.root = root;
        this.shell = root.getChild("shell");
        this.shellTop = root.getChild("shell_top");
        this.head = root.getChild("head");
        this.leftClaw = root.getChild("left_claw");
        this.rightClaw = root.getChild("right_claw");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("shell", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -3.0F, -4.0F, 8.0F, 3.0F, 8.0F),
                PartPose.offset(0.0F, 19.0F, 0.0F));

        root.addOrReplaceChild("shell_top", CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-3.0F, -2.0F, -3.0F, 6.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, 17.0F, 0.0F));

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.offset(0.0F, 18.0F, -3.5F));

        head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                        .texOffs(14, 12)
                        .addBox(-0.5F, -2.5F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(-1.0F, -1.0F, -0.5F));

        head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                        .texOffs(14, 16)
                        .addBox(-0.5F, -2.5F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(1.0F, -1.0F, -0.5F));

        root.addOrReplaceChild("left_claw", CubeListBuilder.create()
                        .texOffs(0, 20)
                        .addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 6.0F),
                PartPose.offset(-5.0F, 19.5F, -1.0F));

        root.addOrReplaceChild("right_claw", CubeListBuilder.create()
                        .texOffs(20, 20)
                        .addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 6.0F),
                PartPose.offset(5.0F, 19.5F, -1.0F));

        addLeg(root, "left_leg_front", 0, -4.0F, -2.0F);
        addLeg(root, "right_leg_front", 10, 4.0F, -2.0F);
        addLeg(root, "left_leg_mid", 20, -4.0F, 0.5F);
        addLeg(root, "right_leg_mid", 30, 4.0F, 0.5F);
        addLeg(root, "left_leg_back", 40, -4.0F, 3.0F);
        addLeg(root, "right_leg_back", 50, 4.0F, 3.0F);

        return LayerDefinition.create(mesh, 64, 64);
    }

    /** One of the six walking legs; {@code texOffsX} picks the UV slot on the sheet. */
    private static void addLeg(PartDefinition root, String name, int texOffsX, float xOffset, float zOffset) {
        root.addOrReplaceChild(name, CubeListBuilder.create()
                        .texOffs(texOffsX, 30)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offset(xOffset, 21.0F, zOffset));
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(MoonShellCrabEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);

        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;

        // Combat clips take precedence over peaceful idle/looking behaviour and deliberately suppress
        // the scuttle cycle, keeping the claw hit/release frames readable.
        if (entity.pinchAnimationState.isStarted()) {
            this.animate(entity.pinchAnimationState, NMAAnimations.MOON_SHELL_CRAB_PINCH, ageInTicks);
        } else if (entity.throwAnimationState.isStarted()) {
            this.animate(entity.throwAnimationState, NMAAnimations.MOON_SHELL_CRAB_THROW, ageInTicks);
        } else if (entity.gazeAnimationState.isStarted()) {
            this.animate(entity.gazeAnimationState, NMAAnimations.MOON_SHELL_CRAB_GAZE, ageInTicks);
        } else {
            this.animate(entity.idleAnimationState, NMAAnimations.MOON_SHELL_CRAB_IDLE, ageInTicks);
        }
        if (entity.getCombatAnimation() == com.suwako.northstarplanetaddon.moon.entity.MoonShellCrabAnimation.IDLE) {
            this.animateWalk(NMAAnimations.MOON_SHELL_CRAB_WALK, limbSwing, limbSwingAmount, 2.0F, 1.5F);
        }
    }

    public ModelPart getShell() {
        return this.shell;
    }

    public ModelPart getShellTop() {
        return this.shellTop;
    }

    public ModelPart getHead() {
        return this.head;
    }

    public ModelPart getLeftClaw() {
        return this.leftClaw;
    }

    public ModelPart getRightClaw() {
        return this.rightClaw;
    }
}
