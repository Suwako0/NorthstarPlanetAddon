package com.suwako.northstarplanetaddon.mercury.client.model;

import com.mojang.logging.LogUtils;
import com.suwako.northstarplanetaddon.mercury.MercuryModule;
import com.suwako.northstarplanetaddon.mercury.entity.SunfishAttackMode;
import com.suwako.northstarplanetaddon.mercury.entity.SunfishEntity;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * 太阳鱼 skeleton: a rounded, tapered fish body with a snout, side-set eyes, a tall dorsal fin, a small
 * anal fin, two long wing-like pectoral fins that end in their own jointed tip, and a forked tail fan.
 * <p>
 * Axis conventions: {@code x} is across the fish, {@code y} is up and {@code z} runs along the fish
 * with the head at {@code -z}. The body pivot sits at {@code y = 8}, which centres the model inside the
 * entity's box instead of leaving it standing on the ground plane.
 * <p>
 * <b>Texture layout (64x64).</b> Every cube's UV slot follows Minecraft's box convention: for a box of
 * {@code w x h x d} at {@code texOffs(u, v)} the upper strip holds {@code DOWN} at {@code x = u + d}
 * and {@code UP} at {@code x = u + d + w}, while the lower strip holds, from left to right,
 * {@code WEST (-x)}, {@code NORTH (-z)}, {@code EAST (+x)} and {@code SOUTH (+z)}.
 *
 * <pre>
 * body          (0, 0)   8 x  7 x 12   flanks at (0,12)-(12,19) and (20,12)-(32,19)
 * head          (40, 0)  6 x  6 x  4   face at (44,4)-(50,10)
 * snout         (40,10)  4 x  2 x  2
 * eye           (52,10)  2 x  2 x  2   both eyes share this slot
 * dorsal fin    (0, 19)  1 x  7 x  6
 * pectoral wing (14,19) 10 x  1 x  6   both wings share this slot
 * pectoral tip  (14,26)  6 x  1 x  4
 * anal fin      (54,19)  1 x  4 x  4
 * tail base     (34,26)  4 x  4 x  4
 * tail fan      (0, 32)  2 x 10 x  3
 * fan lobe      (10,32)  1 x  5 x  3   upper and lower lobe share this slot
 * crest spike   (18,32)  1 x  5 x  1   炽烈太阳鱼 only
 * flank vent    (22,32)  1 x  5 x  3   炽烈太阳鱼 only
 * tail flame    (30,32)  1 x  6 x  4   炽烈太阳鱼 only
 * </pre>
 * <p>
 * Part tree (these are the names the constructor looks up, so the two must stay in sync):
 * {@code body → head (→ snout, eye_left, eye_right), dorsal_fin, pectoral_left (→ tip),
 * pectoral_right (→ tip), anal_fin, tail (→ base → fan → lobe_upper, lobe_lower)}.
 * {@link BlazingSunfishModel} adds {@code head → crest_1..4}, {@code body → vent_left, vent_right} and
 * {@code tail/base → flame_upper, flame_lower}.
 * <p>
 * Lookups go through {@link #childOrEmpty(ModelPart, String)} so a future mismatch degrades to "that
 * fin does not animate" instead of throwing during the resource reload.
 */
public class SunfishModel<T extends SunfishEntity> extends HierarchicalModel<T> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float DEGREES = (float) Math.PI / 180.0F;

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MercuryModule.NAMESPACE, "sunfish"), "main");

    protected static final float BODY_PIVOT_Y = 8.0F;

    private final ModelPart root;
    protected final ModelPart body;
    protected final ModelPart head;
    protected final ModelPart snout;
    protected final ModelPart eyeLeft;
    protected final ModelPart eyeRight;
    protected final ModelPart dorsalFin;
    protected final ModelPart pectoralLeft;
    protected final ModelPart pectoralRight;
    protected final ModelPart pectoralTipLeft;
    protected final ModelPart pectoralTipRight;
    protected final ModelPart analFin;
    protected final ModelPart tail;
    protected final ModelPart tailBase;
    protected final ModelPart tailFan;
    protected final ModelPart lobeUpper;
    protected final ModelPart lobeLower;

    public SunfishModel(ModelPart root) {
        this.root = root;
        this.body = childOrEmpty(root, "body");
        this.head = childOrEmpty(this.body, "head");
        this.snout = childOrEmpty(this.head, "snout");
        this.eyeLeft = childOrEmpty(this.head, "eye_left");
        this.eyeRight = childOrEmpty(this.head, "eye_right");
        this.dorsalFin = childOrEmpty(this.body, "dorsal_fin");
        this.pectoralLeft = childOrEmpty(this.body, "pectoral_left");
        this.pectoralRight = childOrEmpty(this.body, "pectoral_right");
        this.pectoralTipLeft = childOrEmpty(this.pectoralLeft, "tip");
        this.pectoralTipRight = childOrEmpty(this.pectoralRight, "tip");
        this.analFin = childOrEmpty(this.body, "anal_fin");
        this.tail = childOrEmpty(this.body, "tail");
        this.tailBase = childOrEmpty(this.tail, "base");
        this.tailFan = childOrEmpty(this.tailBase, "fan");
        this.lobeUpper = childOrEmpty(this.tailFan, "lobe_upper");
        this.lobeLower = childOrEmpty(this.tailFan, "lobe_lower");
    }

    /** Tolerant part lookup, so a mistyped name can never take down the whole resource stack. */
    protected static ModelPart childOrEmpty(ModelPart parent, String name) {
        try {
            return parent.getChild(name);
        } catch (NoSuchElementException exception) {
            LOGGER.warn("[{}] Sky-fish model has no part '{}'; it will stay still. "
                            + "Check that createBodyLayer() creates it under the right parent.",
                    MercuryModule.NAMESPACE, name);
            return new ModelPart(List.of(), Map.of());
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", bodyCubes(),
                PartPose.offset(0.0F, BODY_PIVOT_Y, 0.0F));
        addHead(body, false);
        addFins(body);
        addTail(body, false);
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** The trunk: one rounded box, tapered visually by the smaller head in front and the tail behind. */
    protected static CubeListBuilder bodyCubes() {
        return CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -3.5F, -6.0F, 8.0F, 7.0F, 12.0F);
    }

    /** Head, snout and the two eyes; {@code blazing} adds the four crest spikes. */
    protected static PartDefinition addHead(PartDefinition body, boolean blazing) {
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(40, 0).addBox(-3.0F, -3.0F, -4.0F, 6.0F, 6.0F, 4.0F)
                , PartPose.offset(0.0F, -0.5F, -6.0F));
        head.addOrReplaceChild("snout", CubeListBuilder.create()
                .texOffs(40, 10).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 2.0F, 2.0F)
                , PartPose.offset(0.0F, -1.0F, -4.0F));
        head.addOrReplaceChild("eye_left", CubeListBuilder.create()
                .texOffs(52, 10).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                , PartPose.offset(-3.2F, 1.5F, -2.0F));
        head.addOrReplaceChild("eye_right", CubeListBuilder.create()
                .texOffs(52, 10).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                , PartPose.offset(3.2F, 1.5F, -2.0F));

        if (blazing) {
            crest(head, 1, -2.4F, -3.0F);
            crest(head, 2, -0.6F, -1.4F);
            crest(head, 3, 0.4F, -1.4F);
            crest(head, 4, 2.2F, -3.0F);
        }
        return head;
    }

    private static void crest(PartDefinition head, int index, float x, float z) {
        head.addOrReplaceChild("crest_" + index, CubeListBuilder.create()
                .texOffs(18, 32).addBox(0.0F, 0.0F, 0.0F, 1.0F, 5.0F, 1.0F)
                , PartPose.offset(x, 3.0F, z));
    }

    /** Dorsal and anal fins plus the two long pectoral wings. */
    protected static void addFins(PartDefinition body) {
        body.addOrReplaceChild("dorsal_fin", CubeListBuilder.create()
                .texOffs(0, 19).addBox(-0.5F, 0.0F, -3.0F, 1.0F, 7.0F, 6.0F)
                , PartPose.offset(0.0F, 3.5F, 1.0F));
        body.addOrReplaceChild("anal_fin", CubeListBuilder.create()
                .texOffs(54, 19).addBox(-0.5F, -4.0F, -2.0F, 1.0F, 4.0F, 4.0F)
                , PartPose.offset(0.0F, -3.5F, 2.0F));

        PartDefinition wingLeft = body.addOrReplaceChild("pectoral_left", CubeListBuilder.create()
                .texOffs(14, 19).addBox(-10.0F, -0.5F, -3.0F, 10.0F, 1.0F, 6.0F)
                , PartPose.offset(-4.0F, 0.0F, -2.0F));
        wingLeft.addOrReplaceChild("tip", CubeListBuilder.create()
                .texOffs(14, 26).addBox(-6.0F, -0.5F, -2.0F, 6.0F, 1.0F, 4.0F)
                , PartPose.offset(-10.0F, 0.0F, 0.0F));
        PartDefinition wingRight = body.addOrReplaceChild("pectoral_right", CubeListBuilder.create()
                .texOffs(14, 19).mirror().addBox(0.0F, -0.5F, -3.0F, 10.0F, 1.0F, 6.0F)
                , PartPose.offset(4.0F, 0.0F, -2.0F));
        wingRight.addOrReplaceChild("tip", CubeListBuilder.create()
                .texOffs(14, 26).mirror().addBox(0.0F, -0.5F, -2.0F, 6.0F, 1.0F, 4.0F)
                , PartPose.offset(10.0F, 0.0F, 0.0F));
    }

    /** Tail base, the tall caudal fan and its two forked lobes. */
    protected static PartDefinition addTail(PartDefinition body, boolean blazing) {
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create(),
                PartPose.offset(0.0F, 0.0F, 6.0F));
        PartDefinition base = tail.addOrReplaceChild("base", CubeListBuilder.create()
                .texOffs(34, 26).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 4.0F)
                , PartPose.ZERO);
        PartDefinition fan = base.addOrReplaceChild("fan", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-1.0F, -5.0F, 0.0F, 2.0F, 10.0F, 3.0F)
                , PartPose.offset(0.0F, 0.0F, 4.0F));
        fan.addOrReplaceChild("lobe_upper", CubeListBuilder.create()
                .texOffs(10, 32).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 5.0F, 3.0F)
                , PartPose.offset(0.0F, 5.0F, 0.0F));
        fan.addOrReplaceChild("lobe_lower", CubeListBuilder.create()
                .texOffs(10, 32).mirror().addBox(-0.5F, 0.0F, 0.0F, 1.0F, 5.0F, 3.0F)
                , PartPose.offset(0.0F, -10.0F, 0.0F));

        if (blazing) {
            base.addOrReplaceChild("flame_upper", CubeListBuilder.create()
                    .texOffs(30, 32).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 6.0F, 4.0F)
                    , PartPose.offset(0.0F, 4.0F, 3.0F));
            base.addOrReplaceChild("flame_lower", CubeListBuilder.create()
                    .texOffs(30, 32).mirror().addBox(-0.5F, 0.0F, 0.0F, 1.0F, 6.0F, 4.0F)
                    , PartPose.offset(0.0F, -10.0F, 3.0F));
        }
        return tail;
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    /**
     * A weightless glide rather than a walk cycle: the body snakes and pitches with the actual flight
     * direction, the tail sweeps as a travelling wave from base to fan to the forked lobes, the
     * pectoral wings row with the tip lagging behind the shoulder, and the fins flex in counter-phase.
     */
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        float time = ageInTicks;

        // 整体浮沉 + 蛇行
        this.body.y = BODY_PIVOT_Y + Mth.sin(time * 0.09F) * 0.7F;
        this.body.yRot = Mth.sin(time * 0.11F) * 0.07F;
        this.body.zRot = Mth.sin(time * 0.07F) * 0.06F;

        // 俯仰跟随真实飞行方向：上升时抬头、下潜时低头。
        Vec3 motion = entity.getDeltaMovement();
        float pitchFromMotion = Mth.clamp((float) (-motion.y * 7.0D), -0.4F, 0.4F);
        this.body.xRot = pitchFromMotion + Mth.sin(time * 0.08F) * 0.03F;

        // 头：跟随视线，另有一点延迟摆动。
        this.head.yRot = netHeadYaw * DEGREES * 0.5F + Mth.sin(time * 0.13F) * 0.09F;
        this.head.xRot = headPitch * DEGREES * 0.35F - pitchFromMotion * 0.4F;
        this.snout.xRot = 0.05F + Mth.sin(time * 0.25F) * 0.07F;

        // 尾：从尾根到尾叶的波浪式摆动（相位依次延后）。
        float wave = time * 0.2F;
        this.tail.yRot = Mth.sin(wave) * 0.34F;
        this.tailBase.yRot = Mth.sin(wave - 0.35F) * 0.2F;
        this.tailFan.yRot = Mth.sin(wave - 0.7F) * 0.28F;
        this.lobeUpper.yRot = Mth.sin(wave - 1.1F) * 0.2F;
        this.lobeLower.yRot = -Mth.sin(wave - 1.1F) * 0.2F;
        this.lobeUpper.zRot = 0.12F;
        this.lobeLower.zRot = -0.12F;

        // 胸鳍：缓慢振翅，翼尖相位滞后。
        float flap = Mth.sin(time * 0.14F);
        this.pectoralLeft.zRot = 0.16F + flap * 0.34F;
        this.pectoralRight.zRot = -0.16F - flap * 0.34F;
        this.pectoralLeft.yRot = 0.1F + Mth.cos(time * 0.1F) * 0.09F;
        this.pectoralRight.yRot = -0.1F - Mth.cos(time * 0.1F) * 0.09F;
        float tipFlap = Mth.sin(time * 0.14F - 0.6F);
        this.pectoralTipLeft.zRot = 0.1F + tipFlap * 0.3F;
        this.pectoralTipRight.zRot = -0.1F - tipFlap * 0.3F;

        // 背鳍与臀鳍反向轻摆，像是在维持姿态。
        this.dorsalFin.zRot = Mth.sin(time * 0.16F) * 0.13F;
        this.analFin.zRot = -Mth.sin(time * 0.16F) * 0.13F;

        // 攻击姿态：由服务端同步的攻击模式驱动，三种模式各自改写上面算好的滑翔姿态。
        SunfishAttackMode attack = entity.getAttackMode();
        if (attack == SunfishAttackMode.DIVE) {
            // 绕纵轴连续旋转 + 低头收翼，就是「打着旋俯冲下来」。
            this.body.yRot = time * 1.35F;
            this.body.xRot = -0.45F + pitchFromMotion * 0.3F;
            this.body.zRot = Mth.sin(time * 0.9F) * 0.12F;
            this.pectoralLeft.zRot = 1.1F + Mth.sin(time * 0.8F) * 0.15F;
            this.pectoralRight.zRot = -1.1F - Mth.sin(time * 0.8F) * 0.15F;
            this.pectoralTipLeft.zRot = 0.55F;
            this.pectoralTipRight.zRot = -0.55F;
        } else if (attack == SunfishAttackMode.WING_FLAP) {
            // 悬停急拍翅膀：频率远高于平时的缓慢划水，翼尖保持相位滞后。
            float beat = Mth.sin(time * 0.95F);
            this.pectoralLeft.zRot = 0.35F + beat * 0.9F;
            this.pectoralRight.zRot = -0.35F - beat * 0.9F;
            this.pectoralTipLeft.zRot = 0.2F + Mth.sin(time * 0.95F - 0.8F) * 0.7F;
            this.pectoralTipRight.zRot = -0.2F - Mth.sin(time * 0.95F - 0.8F) * 0.7F;
            this.body.xRot = -0.12F;
        } else if (attack == SunfishAttackMode.RAY) {
            // 蓄能：身体后仰、双翼后掠，头压低瞄准。
            this.body.xRot = -0.22F + Mth.sin(time * 0.5F) * 0.06F;
            this.pectoralLeft.zRot = -0.35F;
            this.pectoralRight.zRot = 0.35F;
            this.pectoralTipLeft.zRot = -0.2F;
            this.pectoralTipRight.zRot = 0.2F;
            this.head.xRot = -0.25F;
        }

        this.animateExtras(time);
    }

    /** Animated by the subclass as well, so the extras follow the same clock. */
    protected void animateExtras(float ageInTicks) {
    }
}
