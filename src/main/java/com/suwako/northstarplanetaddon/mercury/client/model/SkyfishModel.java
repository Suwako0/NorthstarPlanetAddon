package com.suwako.northstarplanetaddon.mercury.client.model;

import com.mojang.logging.LogUtils;
import com.suwako.northstarplanetaddon.mercury.entity.SunfishAttackMode;
import com.suwako.northstarplanetaddon.mercury.entity.SunfishEntity;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

/**
 * Shared skeleton and locomotion for the two Mercury sky-fish.
 * <p>
 * Both species are long serpentine fish floating with no gravity: a short neck carrying a horned
 * head, a heavy chest, eleven stepped body sections, a tapering tail and a flame fin. The geometry
 * comes from two generated mesh classes ({@link SunfishMesh} / {@link BlazingSunfishMesh}), so this
 * class only owns the parts and the motion.
 * <p>
 * Axis conventions: {@code x} across the fish, {@code y} up, {@code z} along the fish with the head at
 * {@code -z}. All bone pivots sit on the real joints (see {@code tools/fix-pivots.mjs}); the rest pose
 * is already the swimming S-curve, so the animation only adds a travelling wave on top of it.
 * <p>
 * Part tree (names the constructor looks up; the generated meshes and this class must stay in sync):
 * {@code body → neck1 → neck2 → head (→ jaw, horn_l_, horn_r_), fin_l (→ tip), fin_r (→ tip),
 * belly, spine3..7, ventral_l, ventral_r; neck2 → spine1; neck1 → spine2;
 * belly → tail1 → tail2 → tail3 → tail_fin → streamer_a..d}.
 * <p>
 * Lookups go through {@link #childOrEmpty(ModelPart, String)} so a name mismatch degrades to "that
 * fin does not animate" instead of throwing during a resource reload.
 */
public abstract class SkyfishModel<T extends SunfishEntity> extends HierarchicalModel<T> {

    private static final Logger LOGGER = LogUtils.getLogger();
    protected static final float DEGREES = (float) Math.PI / 180.0F;

    private static final String[] SPINE_NAMES = {"spine1", "spine2", "spine3", "spine4", "spine5", "spine6", "spine7"};

    private final ModelPart root;
    protected final ModelPart body;
    protected final ModelPart neck1;
    protected final ModelPart neck2;
    protected final ModelPart head;
    protected final ModelPart jaw;
    protected final ModelPart hornLeft;
    protected final ModelPart hornLeftMid;
    protected final ModelPart hornLeftTip;
    protected final ModelPart hornRight;
    protected final ModelPart hornRightMid;
    protected final ModelPart hornRightTip;
    protected final ModelPart finLeft;
    protected final ModelPart finLeftTip;
    protected final ModelPart finRight;
    protected final ModelPart finRightTip;
    protected final ModelPart belly;
    protected final ModelPart tail1;
    protected final ModelPart tail2;
    protected final ModelPart tail3;
    protected final ModelPart tailFin;
    protected final ModelPart streamerA;
    protected final ModelPart streamerB;
    protected final ModelPart streamerC;
    protected final ModelPart streamerD;
    protected final ModelPart[] spines;
    protected final ModelPart ventralLeft;
    protected final ModelPart ventralRight;

    private final float bodyRestY;

    protected SkyfishModel(ModelPart root) {
        this.root = root;
        this.body = childOrEmpty(root, "body");
        this.bodyRestY = this.body.y;
        this.neck1 = childOrEmpty(this.body, "neck1");
        this.neck2 = childOrEmpty(this.neck1, "neck2");
        this.head = childOrEmpty(this.neck2, "head");
        this.jaw = childOrEmpty(this.head, "jaw");
        this.hornLeft = childOrEmpty(this.head, "horn_l");
        this.hornLeftMid = childOrEmpty(this.hornLeft, "horn_l2");
        this.hornLeftTip = childOrEmpty(this.hornLeftMid, "horn_l3");
        this.hornRight = childOrEmpty(this.head, "horn_r");
        this.hornRightMid = childOrEmpty(this.hornRight, "horn_r2");
        this.hornRightTip = childOrEmpty(this.hornRightMid, "horn_r3");
        this.finLeft = childOrEmpty(this.body, "fin_l");
        this.finLeftTip = childOrEmpty(this.finLeft, "fin_l_tip");
        this.finRight = childOrEmpty(this.body, "fin_r");
        this.finRightTip = childOrEmpty(this.finRight, "fin_r_tip");
        this.belly = childOrEmpty(this.body, "belly");
        this.tail1 = childOrEmpty(this.belly, "tail1");
        this.tail2 = childOrEmpty(this.tail1, "tail2");
        this.tail3 = childOrEmpty(this.tail2, "tail3");
        this.tailFin = childOrEmpty(this.tail3, "tail_fin");
        this.streamerA = childOrEmpty(this.tailFin, "streamer_a");
        this.streamerB = childOrEmpty(this.tailFin, "streamer_b");
        this.streamerC = childOrEmpty(this.tailFin, "streamer_c");
        this.streamerD = childOrEmpty(this.tailFin, "streamer_d");
        this.ventralLeft = childOrEmpty(this.body, "ventral_l");
        this.ventralRight = childOrEmpty(this.body, "ventral_r");
        this.spines = new ModelPart[SPINE_NAMES.length];
        for (int index = 0; index < SPINE_NAMES.length; index++) {
            this.spines[index] = childOrEmpty(this.body, SPINE_NAMES[index]);
        }
    }

    /** Tolerant part lookup, so a mistyped name can never take down the whole resource stack. */
    protected static ModelPart childOrEmpty(ModelPart parent, String name) {
        try {
            return parent.getChild(name);
        } catch (NoSuchElementException exception) {
            LOGGER.warn("[{}] Sky-fish model has no part '{}'; it will stay still. "
                            + "Check that the generated mesh creates it under the right parent.",
                    "northstar_planet_addon", name);
            return new ModelPart(List.of(), Map.of());
        }
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    // ---------------------------------------------------------------------
    // Species tuning: the two fish share the rig but not the tempo.
    // ---------------------------------------------------------------------

    protected abstract float bobSpeed();

    protected abstract float bobAmplitude();

    protected abstract float swaySpeed();

    protected abstract float swayAmplitude();

    protected abstract float waveSpeed();

    protected abstract float waveAmplitude();

    protected abstract float finSpeed();

    protected abstract float finAmplitude();

    protected abstract float finRestAngle();

    /** Per-species idle detail (core pulse, manes, whiskers, crest stiffness...). */
    protected abstract void animateSpecies(T entity, float time, SunfishAttackMode mode);

    /** Species-specific dive spin rate (degrees per tick, as radians). */
    protected abstract float diveSpinSpeed();

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        float time = ageInTicks;
        SunfishAttackMode mode = entity.getAttackMode();

        // 俯仰跟随真实飞行方向：上升抬头、下潜低头。
        Vec3 motion = entity.getDeltaMovement();
        float pitch = Mth.clamp((float) (motion.y * 6.0D), -0.35F, 0.35F);

        // ---- 躯干：浮沉 + 缓慢侧摆，整条蛇形波从这里发出 ----
        this.body.y = this.bodyRestY + Mth.sin(time * bobSpeed()) * bobAmplitude();
        this.body.yRot = Mth.sin(time * swaySpeed()) * swayAmplitude();
        this.body.zRot = Mth.sin(time * swaySpeed() * 0.7F + 1.1F) * swayAmplitude() * 0.55F;
        this.body.xRot = pitch + Mth.sin(time * bobSpeed() * 0.8F) * 0.03F;

        // ---- 颈与头：反向补偿，头尽量保持水平，再叠加视线 ----
        float wave = time * waveSpeed();
        float amp = waveAmplitude();
        this.neck1.yRot = Mth.sin(wave + 0.6F) * amp * 0.45F;
        this.neck2.yRot = Mth.sin(wave + 1.15F) * amp * 0.6F;
        this.head.yRot = netHeadYaw * DEGREES * 0.55F - this.neck1.yRot - this.neck2.yRot * 0.7F;
        this.head.xRot = headPitch * DEGREES * 0.4F - pitch * 0.45F + Mth.sin(time * 0.07F) * 0.03F;
        this.neck1.xRot = Mth.sin(wave * 0.7F + 0.3F) * amp * 0.25F;
        this.neck2.xRot = Mth.sin(wave * 0.7F + 0.8F) * amp * 0.3F;
        this.jaw.xRot = 0.02F + Mth.sin(time * 0.09F) * 0.025F;

        // ---- 尾：相位依次延后的行波，尾鳍与焰须再各延后一档 ----
        this.tail1.yRot = Mth.sin(wave - 0.4F) * amp;
        this.tail2.yRot = Mth.sin(wave - 0.95F) * amp * 1.2F;
        this.tail3.yRot = Mth.sin(wave - 1.45F) * amp * 1.35F;
        this.tail1.xRot = Mth.sin(wave * 0.8F) * amp * 0.4F;
        this.tail2.xRot = Mth.sin(wave * 0.8F - 0.5F) * amp * 0.5F;
        this.tail3.xRot = Mth.sin(wave * 0.8F - 1.0F) * amp * 0.6F;
        this.tailFin.yRot = Mth.sin(wave - 1.95F) * amp * 1.5F;
        this.tailFin.xRot = Mth.sin(wave * 0.8F - 1.5F) * amp * 0.5F;

        float trail = amp * 1.7F;
        this.streamerA.yRot = Mth.sin(wave - 2.3F) * trail;
        this.streamerB.yRot = Mth.sin(wave - 2.4F) * trail;
        this.streamerC.yRot = Mth.sin(wave - 2.55F) * trail;
        this.streamerD.yRot = Mth.sin(wave - 2.7F) * trail;
        this.streamerA.xRot = Mth.sin(wave * 0.9F - 2.0F) * amp * 0.8F;
        this.streamerB.xRot = Mth.sin(wave * 0.9F - 2.2F) * amp * 0.7F;
        this.streamerC.xRot = Mth.sin(wave * 0.9F - 2.4F) * amp * 0.6F;
        this.streamerD.xRot = Mth.sin(wave * 0.9F - 2.6F) * amp * 0.5F;

        // ---- 胸鳍：根部主导、翼尖滞后 ----
        float fan = Mth.sin(time * finSpeed());
        float rest = finRestAngle();
        this.finRight.zRot = rest + fan * finAmplitude();
        this.finLeft.zRot = -rest - fan * finAmplitude();
        float tipFan = Mth.sin(time * finSpeed() - 0.65F);
        this.finRightTip.zRot = tipFan * finAmplitude() * 0.8F;
        this.finLeftTip.zRot = -tipFan * finAmplitude() * 0.8F;
        this.finRight.yRot = -Mth.cos(time * finSpeed() * 0.6F) * 0.08F;
        this.finLeft.yRot = Mth.cos(time * finSpeed() * 0.6F) * 0.08F;

        // ---- 背脊刺与腹鳍：细碎的跟随摆动 ----
        for (int index = 0; index < this.spines.length; index++) {
            this.spines[index].xRot = Mth.sin(time * 0.085F + index * 0.45F) * 0.1F;
            this.spines[index].zRot = Mth.cos(time * 0.065F + index * 0.4F) * 0.05F;
        }
        this.ventralLeft.xRot = Mth.sin(time * 0.1F) * 0.09F;
        this.ventralRight.xRot = -Mth.sin(time * 0.1F) * 0.09F;

        animateSpecies(entity, time, mode);
        applyAttackPose(entity, time, pitch, mode);
    }

    /**
     * Attack poses override whatever the idle computed. The three modes are shared; which second attack
     * a species gets is decided server-side ({@code SunfishAttackGoal}).
     */
    protected void applyAttackPose(T entity, float time, float pitch, SunfishAttackMode mode) {
        if (mode == SunfishAttackMode.DIVE) {
            // 打着旋俯冲：绕自身轴连续旋转，双鳍后掠贴体，尾巴甩成鞭子。
            this.body.yRot = time * diveSpinSpeed();
            this.body.xRot = -0.5F + pitch * 0.3F;
            this.body.zRot = Mth.sin(time * 1.1F) * 0.12F;
            this.finRight.zRot = 0.95F;
            this.finLeft.zRot = -0.95F;
            this.finRight.yRot = -0.6F;
            this.finLeft.yRot = 0.6F;
            this.finRightTip.zRot = 0.55F;
            this.finLeftTip.zRot = -0.55F;
            float lash = time * 0.9F;
            this.tail1.yRot = Mth.sin(lash) * 0.45F;
            this.tail2.yRot = Mth.sin(lash - 0.5F) * 0.5F;
            this.tail3.yRot = Mth.sin(lash - 1.0F) * 0.55F;
            this.head.xRot = -0.35F;
        } else if (mode == SunfishAttackMode.WING_FLAP) {
            // 悬停急拍：频率远高于平时，翼尖保持相位滞后，身体压平。
            float beat = Mth.sin(time * 0.95F);
            this.body.xRot = -0.1F + pitch * 0.2F;
            this.finRight.zRot = 0.3F + beat * 1.0F;
            this.finLeft.zRot = -0.3F - beat * 1.0F;
            this.finRightTip.zRot = 0.2F + Mth.sin(time * 0.95F - 0.8F) * 0.8F;
            this.finLeftTip.zRot = -0.2F - Mth.sin(time * 0.95F - 0.8F) * 0.8F;
            this.body.y = this.bodyRestY + 0.45F;
            this.head.xRot = -0.15F;
        } else if (mode == SunfishAttackMode.RAY) {
            // 蓄能射线：躯干后仰、双鳍前张、头对准目标，尾巴收紧。
            this.body.xRot = 0.32F + Mth.sin(time * 0.4F) * 0.05F;
            this.body.y = this.bodyRestY + 0.3F;
            this.finRight.zRot = 0.5F;
            this.finLeft.zRot = -0.5F;
            this.finRight.yRot = 0.45F;
            this.finLeft.yRot = -0.45F;
            this.finRightTip.zRot = 0.35F;
            this.finLeftTip.zRot = -0.35F;
            this.head.xRot = -0.3F;
            this.jaw.xRot = 0.35F;
        } else {
            // 待机时把鳍的静止角还给物种自己的值（上面已写），无需处理。
        }
    }
}
