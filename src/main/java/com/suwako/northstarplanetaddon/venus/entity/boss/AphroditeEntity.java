package com.suwako.northstarplanetaddon.venus.entity.boss;

import com.suwako.northstarplanetaddon.venus.combat.WeakPoint;
import com.suwako.northstarplanetaddon.venus.registry.ModEffects;
import com.suwako.northstarplanetaddon.venus.registry.ModEntities;
import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 阿佛洛狄忒，硫之女王（设计草案 4.4 节）：熔管层 · 终局。
 *
 * <table>
 *   <tr><th>项</th><th>值</th></tr>
 *   <tr><td>本体 HP</td><td>1000</td></tr>
 *   <tr><td>护甲板</td><td>4 × 200</td></tr>
 *   <tr><td>形态</td><td>100–66% 酸潮 / 66–33% 茧 / 33–0% 女王（限时 4 分钟）</td></tr>
 *   <tr><td>弱点</td><td>胸口硫晶核（常驻，半径 1.5）+ 背部双酸囊（P3，半径 1.2）</td></tr>
 * </table>
 *
 * <h2>三形态机制</h2>
 * <ul>
 *   <li><b>P1 酸潮</b>：每 15s 开窗 5s；酸浪、酸针、召唤 2 只酸潮水母；</li>
 *   <li><b>P2 茧</b>：结茧期间完全免疫，<b>但打茧会消耗茧护壁</b>（{@link #onImmuneHit}），
 *       护壁清空可提前破茧；酸位每秒上升，站低处会持续掉血；最迟 20s 自动破茧 + 开窗 8s；</li>
 *   <li><b>P3 女王</b>：体型×2、限时 4 分钟；硫火环 + 高压冲击波，每 20s 开窗 6s；
 *       超时触发全场 999 点酸爆。</li>
 * </ul>
 */
public class AphroditeEntity extends AbstractBossEntity implements GeoEntity {

    // ---------------------------------------------------------------- 数值
    private static final int ACID_WAVE_CD = 140;
    private static final int ACID_WAVE_WINDUP = 16;
    private static final float ACID_WAVE_DAMAGE = 12.0F;
    private static final int ACID_WAVE_CORROSION = 20;
    private static final double ACID_WAVE_REACH = 8.0D;

    private static final int ACID_NEEDLE_CD = 100;
    private static final int ACID_NEEDLE_WINDUP = 10;
    private static final float ACID_NEEDLE_DAMAGE = 6.0F;
    private static final int ACID_NEEDLE_SHOTS = 3;

    private static final int FIRE_RING_CD = 240;
    private static final int FIRE_RING_WINDUP = 30;
    private static final float FIRE_RING_DAMAGE = 8.0F;
    private static final double FIRE_RING_RADIUS = 10.0D;

    private static final int SHOCKWAVE_CD = 500;
    private static final int SHOCKWAVE_WINDUP = 50;
    private static final float SHOCKWAVE_DAMAGE = 20.0F;
    private static final double SHOCKWAVE_RADIUS = 15.0D;

    private static final int SUMMON_CD = 300;
    /** 召唤前摇 0.6 s：抬手 + 仰头，水母在前摇结束时出现。 */
    private static final int SUMMON_WINDUP = 12;
    /** 茧护壁总血量：4 根硫晶柱 × 3 次重击 ≈ 240。 */
    private static final float COCOON_HEALTH = 240.0F;
    private static final int COCOON_MAX_TICKS = 400;
    private static final int COCOON_BREAK_WINDOW = 160;
    /** P3 限时 4 分钟。 */
    private static final int ENRAGE_TICKS = 4800;
    /** 酸位上升：每 20 tick 上升 0.5 格（= 0.5 格/秒）。 */
    private static final double ACID_RISE_PER_SECOND = 0.5D;

    // ------------------------------------------------------------------ 动画（GeckoLib）
    private static final int ACTION_NONE = 0;
    private static final int ACTION_WAVE = 1;
    private static final int ACTION_NEEDLE = 2;
    private static final int ACTION_RING = 3;
    private static final int ACTION_SHOCK = 4;
    private static final int ACTION_COCOON_IN = 5;
    private static final int ACTION_COCOON_BREAK = 6;
    private static final int ACTION_PHASE = 7;
    private static final int ACTION_SUMMON = 8;
    private static final int ACTION_PLATE = 9;

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_FLOAT = RawAnimation.begin().thenLoop("float");
    private static final RawAnimation ANIM_WAVE =
            RawAnimation.begin().then("acid_wave", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_NEEDLE =
            RawAnimation.begin().then("acid_needle", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_RING =
            RawAnimation.begin().then("fire_ring", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_SHOCK =
            RawAnimation.begin().then("shockwave", Animation.LoopType.PLAY_ONCE);
    /** 召唤酸潮水母：双臂外展上抬 + 仰头，水母在收势时出现。 */
    private static final RawAnimation ANIM_SUMMON =
            RawAnimation.begin().then("summon", Animation.LoopType.PLAY_ONCE);
    /** 碎甲：一记后仰顿挫 + 对应肩甲外翻。 */
    private static final RawAnimation ANIM_PLATE =
            RawAnimation.begin().then("plate_break", Animation.LoopType.PLAY_ONCE);
    /** 结茧：停在最后一帧，破茧前一直保持茧内姿态。 */
    private static final RawAnimation ANIM_COCOON_IN =
            RawAnimation.begin().then("cocoon_in", Animation.LoopType.HOLD_ON_LAST_FRAME);
    private static final RawAnimation ANIM_COCOON_BREAK =
            RawAnimation.begin().then("cocoon_break", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_PHASE =
            RawAnimation.begin().then("phase_transition", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_DEATH =
            RawAnimation.begin().then("death", Animation.LoopType.HOLD_ON_LAST_FRAME);

    private static final EntityDataAccessor<Integer> DATA_ACTION =
            SynchedEntityData.defineId(AphroditeEntity.class, EntityDataSerializers.INT);
    /** 结茧状态同步位：客户端渲染层靠它决定要不要画茧壳。 */
    private static final EntityDataAccessor<Boolean> DATA_COCOONED =
            SynchedEntityData.defineId(AphroditeEntity.class, EntityDataSerializers.BOOLEAN);
    /** 茧护壁剩余值同步位：客户端用它算壳的染色（绿 → 黄 → 红）。 */
    private static final EntityDataAccessor<Float> DATA_COCOON_HP =
            SynchedEntityData.defineId(AphroditeEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    /** 一次性动作剩余 tick；<=0 表示「一直保持」（结茧）。 */
    private int actionTicks;

    private int acidWaveTicks = ACID_WAVE_CD;    private int needleTicks = ACID_NEEDLE_CD;
    private int fireRingTicks = FIRE_RING_CD;
    private int shockwaveTicks = SHOCKWAVE_CD;
    private int summonTicks = SUMMON_CD;
    private int windupTicks;
    private int windupMove;

    private float cocoonHealth;
    private int cocoonTicks;
    private boolean cocooned;
    private double acidLevel;
    private double acidBaseY;
    private int enrageTicks = ENRAGE_TICKS;
    private int windowTimerTicks = 300;

    public AphroditeEntity(EntityType<? extends AphroditeEntity> type, Level level) {
        super(type, level, 4, 200.0F, BossEvent.BossBarColor.PURPLE);
        // 腰核上移到裸腹：模型宝石中心 y=51 单位（≈3.19 格）、z=−8.8 单位，判定球跟着对齐
        this.addWeakPoint("sulfur_core", new Vec3(0.0D, 3.2D, -0.55D), 1.5D, WeakPoint.ALWAYS);
        // 双酸囊对齐模型 (±14, 36, 10) 单位 → (±0.875, 2.25, 0.625)
        this.addWeakPoint("acid_sac_left", new Vec3(-0.9D, 2.25D, 0.65D), 1.2D, WeakPoint.PHASE_3);
        this.addWeakPoint("acid_sac_right", new Vec3(0.9D, 2.25D, 0.65D), 1.2D, WeakPoint.PHASE_3);
    }

    // ------------------------------------------------------------------ GeckoLib 动画接线
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, ACTION_NONE);
        builder.define(DATA_COCOONED, false);
        builder.define(DATA_COCOON_HP, COCOON_HEALTH);
    }

    // ------------------------------------------------------------------ 结茧状态（给渲染层）
    /** 是否正在结茧（客户端可用：走同步位）。 */
    public boolean isCocooned() {
        return this.entityData.get(DATA_COCOONED);
    }

    /** 茧护壁剩余比例：1 = 满，0 = 即将破茧。客户端可用。 */
    public float getCocoonHealthRatio() {
        if (!this.isCocooned()) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, this.entityData.get(DATA_COCOON_HP) / COCOON_HEALTH));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 单控制器（多控制器会抢骨骼，见 minecraft-geckolib-animation-delivery §3）
        controllers.add(new AnimationController<>(this, "main", 4, this::mainController));
    }

    /** 优先级：死亡 > 一次性动作 > 结茧保持 > 移动/待机。 */
    private PlayState mainController(AnimationState<AphroditeEntity> state) {
        if (this.isDeadOrDying()) {
            state.getController().setAnimationSpeed(1.0D).setAnimation(ANIM_DEATH);
            return PlayState.CONTINUE;
        }
        RawAnimation action = switch (this.entityData.get(DATA_ACTION)) {
            case ACTION_WAVE -> ANIM_WAVE;
            case ACTION_NEEDLE -> ANIM_NEEDLE;
            case ACTION_RING -> ANIM_RING;
            case ACTION_SHOCK -> ANIM_SHOCK;
            case ACTION_COCOON_IN -> ANIM_COCOON_IN;
            case ACTION_COCOON_BREAK -> ANIM_COCOON_BREAK;
            case ACTION_PHASE -> ANIM_PHASE;
            case ACTION_SUMMON -> ANIM_SUMMON;
            case ACTION_PLATE -> ANIM_PLATE;
            default -> null;
        };
        if (action != null) {
            state.getController().setAnimationSpeed(1.0D).setAnimation(action);
            return PlayState.CONTINUE;
        }
        boolean moving = this.getDeltaMovement().horizontalDistanceSqr() > 4.0E-4D
                || (this.getTarget() != null && this.distanceToSqr(this.getTarget()) > 9.0D);
        state.getController().setAnimationSpeed(1.0D)
                .setAnimation(moving ? ANIM_FLOAT : ANIM_IDLE);
        return PlayState.CONTINUE;
    }

    /** 触发一次性动作：只走服务端，实体数据自动同步到客户端。ticks<=0 表示保持到下一次切换。 */
    private void playAction(int action, int ticks) {
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_ACTION, action);
            this.actionTicks = ticks;
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ARMOR, 24.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    private boolean isGiant() {
        return this.getPhase() >= 3;
    }

    /** P1/P2 体型 6×9×6，P3 巨大化 12×18×12（数据放大约 2 倍以便命中判定）。 */
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isGiant()
                ? EntityDimensions.scalable(8.0F, 12.0F)
                : EntityDimensions.scalable(4.0F, 6.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 48.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ================================================================== 阶段
    @Override
    protected void onPhaseChanged(int from, int to) {
        super.onPhaseChanged(from, to);
        if (to == 2) {
            this.enterCocoon();
        }
        if (to == 3) {
            this.cocooned = false;
            this.actionTicks = 0;
            this.acidLevel = 0.0D;
            this.refreshDimensions();
            this.enrageTicks = ENRAGE_TICKS;
            this.openWindow(120);
            this.playAction(ACTION_PHASE, 30);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        this.getX(), this.getY() + 4.0D, this.getZ(), 3, 2.0D, 2.0D, 2.0D, 0.0D);
                serverLevel.playSound(null, this.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE,
                        3.0F, 0.7F);
            }
        }
    }

    private void enterCocoon() {
        this.cocooned = true;
        this.cocoonHealth = COCOON_HEALTH;
        this.cocoonTicks = 0;
        this.acidBaseY = this.getY();
        this.acidLevel = 0.0D;
        this.refreshDimensions();
        this.playAction(ACTION_COCOON_IN, 0);
        this.entityData.set(DATA_COCOONED, true);
        this.entityData.set(DATA_COCOON_HP, COCOON_HEALTH);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 2.0D, this.getZ(),
                    60, 2.0D, 2.0D, 2.0D, 0.02D);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.HOSTILE,
                    2.0F, 0.5F);
        }
    }

    /** 茧期间免疫；伤害走 onImmuneHit 消耗茧护壁。 */
    @Override
    public boolean isDamageImmune() {
        return super.isDamageImmune() || this.cocooned;
    }

    @Override
    public void onImmuneHit(DamageSource source, float amount) {
        if (!this.cocooned) {
            return;
        }
        // 重击更有价值：近战/爆炸按原值，其它按 50%
        float factor = com.suwako.northstarplanetaddon.venus.combat.DamageClassifier.classify(source)
                == com.suwako.northstarplanetaddon.venus.combat.DamageKind.MELEE ? 1.0F : 0.5F;
        this.cocoonHealth -= Math.max(1.0F, amount * factor);
        this.entityData.set(DATA_COCOON_HP, Math.max(0.0F, this.cocoonHealth));
        if (this.cocoonHealth <= 0.0F) {
            this.breakCocoon("damage");
        }
    }

    private void breakCocoon(String reason) {
        this.cocooned = false;
        this.entityData.set(DATA_COCOONED, false);
        this.entityData.set(DATA_COCOON_HP, 0.0F);
        this.cocoonHealth = 0.0F;
        this.openWindow(COCOON_BREAK_WINDOW);
        this.acidLevel = 0.0D;
        this.playAction(ACTION_COCOON_BREAK, 40);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + 2.0D, this.getZ(), 2, 1.5D, 1.5D, 1.5D, 0.0D);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE,
                    2.0F, 0.6F);
            serverLevel.players().forEach(player -> {
                if (player.distanceToSqr(this) < 32.0D * 32.0D) {
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                            "message.northstar_planet_addon.aphrodite_cocoon_broken", reason)
                                    .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
                }
            });
        }
    }

    // ================================================================== AI
    @Override
    protected void tickBossAi() {
        // 一次性动作计时（结茧用 0 表示保持，不在这里清）
        if (!this.level().isClientSide && this.actionTicks > 0 && --this.actionTicks == 0) {
            this.entityData.set(DATA_ACTION, ACTION_NONE);
        }
        LivingEntity target = this.getTarget();

        if (this.cocooned) {
            this.tickCocoon();
            return;
        }
        if (this.getPhase() >= 3) {
            this.tickEnrage();
        }
        if (target == null || !target.isAlive()) {
            return;
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // 常规开窗节奏：P1 每 15s 开 5s；P3 每 20s 开 6s
        if (--this.windowTimerTicks <= 0) {
            this.windowTimerTicks = this.getPhase() >= 3 ? 400 : 300;
            this.openWindow(this.getPhase() >= 3 ? 120 : 100);
        }

        if (this.windupTicks > 0) {
            this.windupTicks--;
            if (this.windupTicks == 0) {
                this.executeWindup(target);
            }
            return;
        }

        double distance = this.distanceTo(target);
        if (distance > 3.0D && distance < 24.0D) {
            this.getNavigation().moveTo(target, 1.0D);
        }

        if (--this.acidWaveTicks <= 0 && distance <= ACID_WAVE_REACH) {
            this.acidWaveTicks = ACID_WAVE_CD;
            this.startWindup(ACID_WAVE_WINDUP, 0);
            return;
        }
        if (--this.needleTicks <= 0) {
            this.needleTicks = ACID_NEEDLE_CD;
            this.startWindup(ACID_NEEDLE_WINDUP, 1);
            return;
        }
        if (this.getPhase() >= 3 && --this.fireRingTicks <= 0) {
            this.fireRingTicks = FIRE_RING_CD;
            this.startWindup(FIRE_RING_WINDUP, 2);
            return;
        }
        if (this.getPhase() >= 3 && --this.shockwaveTicks <= 0) {
            this.shockwaveTicks = SHOCKWAVE_CD;
            this.startWindup(SHOCKWAVE_WINDUP, 3);
            return;
        }
        if (this.getPhase() == 1 && --this.summonTicks <= 0) {
            this.summonTicks = SUMMON_CD;
            // 召唤也要有前摇：0.6 s 抬手，玩家能提前走位
            this.startWindup(SUMMON_WINDUP, 4);
            return;
        }
    }

    private void tickCocoon() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.cocoonTicks++;
        // 酸位每秒 +0.5 格：站得比酸面低就持续掉血
        this.acidLevel += ACID_RISE_PER_SECOND / 20.0D;
        if (this.cocoonTicks % 20 == 0) {
            double acidY = this.acidBaseY + this.acidLevel;
            for (Player player : serverLevel.getPlayers(p -> p.isAlive() && !p.isSpectator()
                    && p.distanceToSqr(this) < 32.0D * 32.0D)) {
                if (player.getY() < acidY) {
                    player.hurt(serverLevel.damageSources().magic(), 2.0F);
                    player.addEffect(new MobEffectInstance(ModEffects.CORROSION, 100, 1));
                }
            }
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.acidBaseY + this.acidLevel,
                    this.getZ(), 30, 10.0D, 0.2D, 10.0D, 0.0D);
        }
        if (this.cocoonTicks >= COCOON_MAX_TICKS) {
            this.breakCocoon("timeout");
        }
    }

    private void tickEnrage() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (--this.enrageTicks > 0) {
            return;
        }
        // 4 分钟超时：全场 999 酸爆
        this.enrageTicks = ENRAGE_TICKS;
        AABB area = this.getBoundingBox().inflate(32.0D);
        for (Player player : serverLevel.getEntitiesOfClass(Player.class, area, Player::isAlive)) {
            player.hurt(serverLevel.damageSources().magic(), 999.0F);
        }
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 5.0D, this.getZ(),
                20, 10.0D, 4.0D, 10.0D, 0.0D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 5.0F, 0.4F);
    }

    /** 开窗粒子用酸绿（HAPPY_VILLAGER 的小绿星），和她的酸液配色一致。 */
    @Override
    protected net.minecraft.core.particles.SimpleParticleType windowParticleType() {
        return ParticleTypes.HAPPY_VILLAGER;
    }

    /**
     * 护甲板被打碎：播 `plate_break` 顿挫动作，并在对应甲片位置炸一簇碎屑 + 玻璃破碎音。
     *
     * <p>板序号与模型骨的对应：0/1 = 胸甲上/下段（正面 z −6），2/3 = 左/右肩甲（x ∓12）。</p>
     */
    @Override
    protected void onPlateBroken(int index) {
        super.onPlateBroken(index);
        this.playAction(ACTION_PLATE, 12);
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double localX = switch (index) {
            case 2 -> -12.0D;
            case 3 -> 12.0D;
            default -> 0.0D;
        };
        double localY = switch (index) {
            case 0 -> 58.0D;
            case 1 -> 50.0D;
            default -> 66.0D;
        };
        double localZ = index < 2 ? -6.0D : 0.0D;
        double yaw = Math.toRadians(this.yBodyRot);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double wx = this.getX() + localX * cos + localZ * sin;
        double wz = this.getZ() - localX * sin + localZ * cos;
        double wy = this.getY() + localY;
        serverLevel.sendParticles(ParticleTypes.CRIT, wx, wy, wz, 26, 0.7D, 0.7D, 0.7D, 0.28D);
        serverLevel.sendParticles(ParticleTypes.END_ROD, wx, wy, wz, 12, 0.45D, 0.45D, 0.45D, 0.06D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.6F, 0.8F);
    }

    private void startWindup(int ticks, int move) {
        this.windupTicks = ticks;
        this.windupMove = move;
        // 前摇动画时长按招式给足（>= 代码前摇），动作播完再回到 idle/float
        int action = switch (move) {
            case 0 -> ACTION_WAVE;
            case 1 -> ACTION_NEEDLE;
            case 2 -> ACTION_RING;
            case 4 -> ACTION_SUMMON;
            default -> ACTION_SHOCK;
        };
        int animTicks = switch (move) {
            case 0 -> 24;
            case 1 -> 16;
            case 2 -> 40;
            case 4 -> 24;
            default -> 60;
        };
        this.playAction(action, animTicks);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SNEEZE, this.getX(), this.getY() + 2.0D, this.getZ(),
                    12, 1.0D, 1.0D, 1.0D, 0.02D);
        }
    }

    private void executeWindup(LivingEntity target) {
        switch (this.windupMove) {
            case 0 -> this.acidWave(target);
            case 1 -> this.acidNeedles(target);
            case 2 -> this.fireRing();
            case 3 -> this.shockwave();
            case 4 -> this.summonJellies();
            default -> {
            }
        }
    }

    /** 酸浪：前方 8 格、12 伤害 + 腐蚀 20s。 */
    private void acidWave(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 look = this.getLookAngle().scale(3.0D);
        AABB area = this.getBoundingBox().inflate(ACID_WAVE_REACH * 0.6D).move(look.x, 0.0D, look.z);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this && entity.isAlive() && !(entity instanceof AbstractBossEntity)
                        && this.isInFront(entity, ACID_WAVE_REACH))) {
            victim.hurt(serverLevel.damageSources().mobAttack(this), ACID_WAVE_DAMAGE);
            victim.addEffect(new MobEffectInstance(ModEffects.CORROSION, ACID_WAVE_CORROSION * 20, 2));
        }
        serverLevel.sendParticles(ParticleTypes.SQUID_INK, this.getX() + look.x, this.getY() + 1.0D,
                this.getZ() + look.z, 40, 4.0D, 0.6D, 4.0D, 0.0D);
    }

    private boolean isInFront(LivingEntity entity, double reach) {
        Vec3 toEntity = entity.position().subtract(this.position()).normalize();
        return this.getLookAngle().dot(toEntity) > 0.3D && this.distanceTo(entity) <= reach;
    }

    /** 酸针：3 段追踪伤害。 */
    private void acidNeedles(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int shot = 0; shot < ACID_NEEDLE_SHOTS; shot++) {
            target.hurt(serverLevel.damageSources().mobAttack(this), ACID_NEEDLE_DAMAGE);
        }
        serverLevel.sendParticles(ParticleTypes.SNEEZE, target.getX(), target.getY() + 1.0D, target.getZ(),
                20, 0.4D, 0.6D, 0.4D, 0.05D);
        serverLevel.playSound(null, target.blockPosition(), SoundEvents.ARROW_HIT, SoundSource.HOSTILE,
                1.0F, 1.4F);
    }

    /** 硫火环：以 Boss 为圆心的环带伤害。 */
    private void fireRing() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(FIRE_RING_RADIUS),
                entity -> entity != this && entity.isAlive() && !(entity instanceof AbstractBossEntity))) {
            double distance = Math.sqrt(victim.distanceToSqr(this));
            if (distance < 2.0D || distance > FIRE_RING_RADIUS) {
                continue;
            }
            victim.hurt(serverLevel.damageSources().onFire(), FIRE_RING_DAMAGE);
            victim.igniteForSeconds(3.0F);
        }
        for (int radius = 2; radius <= (int) FIRE_RING_RADIUS; radius += 2) {
            for (int i = 0; i < 24; i++) {
                double angle = i * Math.PI / 12.0D;
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        this.getX() + Math.cos(angle) * radius, this.getY() + 0.5D,
                        this.getZ() + Math.sin(angle) * radius, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    /** 高压冲击波：半径 15、20 伤害。 */
    private void shockwave() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(SHOCKWAVE_RADIUS),
                entity -> entity != this && entity.isAlive() && !(entity instanceof AbstractBossEntity))) {
            victim.hurt(serverLevel.damageSources().mobAttack(this), SHOCKWAVE_DAMAGE);
            victim.push(0.0D, 1.2D, 0.0D);
        }
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.0D, this.getZ(),
                30, SHOCKWAVE_RADIUS * 0.6D, 0.5D, SHOCKWAVE_RADIUS * 0.6D, 0.0D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 3.5F, 0.5F);
    }

    private void summonJellies() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            var jelly = ModEntities.ACID_JELLY.get().create(serverLevel);
            if (jelly == null) {
                continue;
            }
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            jelly.moveTo(this.getX() + Math.cos(angle) * 3.0D, this.getY() + 1.0D,
                    this.getZ() + Math.sin(angle) * 3.0D, this.random.nextFloat() * 360.0F, 0.0F);
            serverLevel.addFreshEntity(jelly);
        }
    }

    // ================================================================== 掉落
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, source, hitByPlayer);
        // 第 4.4 节：硫心未注册 → 用硫晶块代替；启明星战甲蓝图未注册 → 用启明星核代替
        this.spawnAtLocation(new ItemStack(com.suwako.northstarplanetaddon.venus.registry.ModBlocks.SULFUR_CRYSTAL_BLOCK.get().asItem(), 1));
        this.spawnAtLocation(new ItemStack(ModItems.APHRODITE_TEAR.get(), 1 + this.random.nextInt(2)));
        this.spawnAtLocation(new ItemStack(ModItems.PRE_CIVILIZATION_ALLOY.get(), 4 + this.random.nextInt(3)));
        this.spawnAtLocation(new ItemStack(ModItems.SULFUR_CRYSTAL.get(), 2 + this.random.nextInt(3)));
        if (this.random.nextFloat() < 0.35F) {
            this.spawnAtLocation(new ItemStack(ModItems.PHOSPHOROS_CORE.get(), 1));
        }
    }

    @Override
    public String debugLines() {
        return super.debugLines() + " aphrodite cocoon=" + this.cocooned
                + " cocoonHp=" + String.format("%.1f", this.cocoonHealth)
                + " acidLevel=" + String.format("%.2f", this.acidLevel)
                + " enrage=" + this.enrageTicks;
    }
}
