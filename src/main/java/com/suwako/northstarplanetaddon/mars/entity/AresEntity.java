package com.suwako.northstarplanetaddon.mars.entity;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.registry.ModEffects;
import com.suwako.northstarplanetaddon.mars.registry.ModEntities;
import com.suwako.northstarplanetaddon.mars.registry.ModItems;
import com.suwako.northstarplanetaddon.mars.world.KineticMachines;
import com.suwako.northstarplanetaddon.mars.world.Sandstorm;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 熔核巨像·阿瑞斯 —— 古代火星文明最后的自动锻造守卫。
 *
 * <p>继承原版铁傀儡以复用模型/贴图（熔核巨像 = 巨型机械傀儡），AI 与全部战斗逻辑重写。</p>
 *
 * <h2>核心温度</h2>
 * <ul>
 *     <li>{@code > 1500°C} 狂暴：减伤 60%、攻击 +50%、移速 +30%、岩浆喷发频率翻倍；</li>
 *     <li>{@code 500 ~ 1500°C} 正常；</li>
 *     <li>{@code < 500°C} 脆化：受到伤害 +200%、行动迟缓、核心暴露。</li>
 * </ul>
 * <p>v1.9 新增：{@code > 1500°C} 时展开**半透明护盾**（{@link #hasShield()}），护盾期间完全无敌且
 * 受击不再升温，破盾只能靠便携式冷却器降温；每次实际结算的伤害给核心温度 +20℃（{@link #HEAT_PER_HIT}）。</p>
 * 玩家用便携式冷却器（水 / 液氧）右击降温。
 *
 * <h2>阶段</h2>
 * <ol>
 *     <li>100% - 66% 熔火形态：岩浆拳（近战击飞）、地热喷发、召唤火星恶魔蛾；</li>
 *     <li>66% - 33% 风暴形态：升空 + 强化沙尘暴 + 鳞粉风暴；必须把核心温度压到 500°C 以下，
 *         否则持续回血；降温成功则坠落并进入 10 秒脆弱期；</li>
 *     <li>33% - 0% 过载形态：停止移动、全图地热喷发；60 秒内未击杀则自爆，
 *         摧毁战场内所有 Create 机械方块且不掉落稀有物品。</li>
 * </ol>
 */
public class AresEntity extends IronGolem implements GeoEntity {

    public static final int DEFAULT_CORE_TEMPERATURE = 1200;
    public static final int MAX_CORE_TEMPERATURE = 2200;
    public static final int OVERHEAT_TEMPERATURE = 1500;
    public static final int BRITTLE_TEMPERATURE = 500;

    /**
     * 护盾展开阈值（℃）：{@code coreTemperature > 1500} 时展开半透明护盾（与狂暴同阈值，见 {@link #hasShield()}）。
     */
    public static final int SHIELD_TEMPERATURE = OVERHEAT_TEMPERATURE;

    /** v1.9：每次**实际结算**的伤害给核心温度加的温（℃），见 {@link #hurt(DamageSource, float)}。 */
    public static final int HEAT_PER_HIT = 20;

    /** 狂暴状态的移速修饰符：+30%（基础值乘算），对应需求里的「移动速度 +30%」。 */
    private static final AttributeModifier ENRAGED_SPEED = new AttributeModifier(
            MarsModule.id("ares_enraged_speed"), 0.3D, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

    /** 阶段二悬停高度：文档「升至 Y+8」。 */
    private static final int HOVER_HEIGHT = 8;

    /** 脆弱期时长（10 秒）。 */
    private static final int VULNERABILITY_TICKS = 200;

    /** 阶段三自爆倒计时（60 秒）。 */
    private static final int SELF_DESTRUCT_TICKS = 1200;

    // ---------------------------------------------------------------- v1.11 技能数值（《Boss属性重新设计》）

    /** 岩浆拳：冷却 4 秒，前摇 0.8 秒（见 {@link AresMeleeGoal}）。 */
    private static final int MELEE_INTERVAL_TICKS = 80;
    private static final int MELEE_WINDUP_TICKS = 16;

    /** 地热喷发：冷却 8 秒，前摇 1.5 秒，随机 6 点、半径 2、8 点伤害、着火 5 秒。 */
    private static final int ERUPTION_CD = 160;
    private static final int ERUPTION_WINDUP = 30;
    private static final int ERUPTION_POINTS = 6;
    private static final double ERUPTION_RADIUS = 2.0D;
    private static final float ERUPTION_DAMAGE = 8.0F;
    private static final int ERUPTION_FIRE_TICKS = 100;
    /** 喷发点能在多大范围内找玩家。 */
    private static final double ERUPTION_SEARCH = 32.0D;

    /** 召唤火星恶魔蛾：冷却 45 秒，前摇 1 秒，2–4 只。 */
    private static final int SUMMON_CD = 900;
    private static final int SUMMON_WINDUP = 20;
    private static final int SUMMON_MIN = 2;
    private static final int SUMMON_MAX = 4;

    /** 鳞粉风暴：冷却 6 秒，前摇 1 秒，8 枚追踪、命中 6 点伤害、腐蚀 5 秒。 */
    private static final int SCALE_STORM_CD = 120;
    private static final int SCALE_STORM_WINDUP = 20;
    private static final int SCALE_STORM_MARKS = 8;
    private static final double SCALE_STORM_RANGE = 12.0D;
    private static final float SCALE_MARK_DAMAGE = 6.0F;
    private static final int SCALE_MARK_CORROSION_TICKS = 100;
    /** 单枚鳞粉的飞行速度（格/tick）与最长存活。 */
    private static final double SCALE_MARK_SPEED = 0.55D;
    private static final int SCALE_MARK_MAX_AGE = 80;

    /** 阶段三全图地热喷发：冷却 2 秒，前摇 1 秒，10 点伤害、半径 2、着火 5 秒。 */
    private static final int OVERLOAD_ERUPTION_CD = 40;
    private static final int OVERLOAD_ERUPTION_WINDUP = 20;
    private static final float OVERLOAD_DAMAGE = 10.0F;
    private static final double OVERLOAD_RADIUS = 2.0D;

    /** 阶段二冷却窗口：核心温度 &gt; {@link #BRITTLE_TEMPERATURE} 时回血 10 HP/s。 */
    private static final float COOLING_HEAL_PER_TICK = 5.0F;

    /** Boss 领域半径（格）：祭坛（首次 tick 的位置）周围 48 格内才会被选为目标。 */
    private static final double DOMAIN_RADIUS = 48.0D;

    /** 自爆：32 格内最多 100 点伤害（按距离线性衰减），爆炸威力 6（约摧毁周围 6 格方块）。 */
    private static final double SELF_DESTRUCT_RADIUS = 32.0D;
    private static final float SELF_DESTRUCT_MAX_DAMAGE = 100.0F;
    private static final float SELF_DESTRUCT_POWER = 6.0F;

    /** 阶段三「核心暴露」：护甲归零（基础值 20，所以是 -20 点加法修饰符）。 */
    private static final AttributeModifier BROKEN_ARMOR = new AttributeModifier(
            MarsModule.id("ares_core_exposed_armor"), -20.0D,
            AttributeModifier.Operation.ADD_VALUE);

    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    // ------------------------------------------------------------ 动画（GeckoLib）

    private static final byte ACTION_NONE = 0;
    private static final byte ACTION_ATTACK = 1;
    private static final byte ACTION_ERUPTION = 2;
    private static final byte ACTION_STORM = 3;
    private static final byte ACTION_OVERLOAD = 4;
    private static final byte ACTION_SLAM = 5;
    private static final byte ACTION_SUMMON = 6;
    private static final byte ACTION_CORE_PURGE = 7;

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ANIM_ATTACK =
            RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_ERUPTION =
            RawAnimation.begin().then("eruption", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_STORM = RawAnimation.begin().thenLoop("storm");
    private static final RawAnimation ANIM_OVERLOAD = RawAnimation.begin().thenLoop("overload");
    private static final RawAnimation ANIM_SELF_DESTRUCT = RawAnimation.begin().thenLoop("self_destruct");
    private static final RawAnimation ANIM_SLAM =
            RawAnimation.begin().then("slam", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_SUMMON =
            RawAnimation.begin().then("summon", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_CORE_PURGE =
            RawAnimation.begin().then("core_purge", Animation.LoopType.PLAY_ONCE);

    /** 客户端动画需要的状态（阶段 / 是否升空 / 自爆倒计时秒数 / 当前动作）。 */
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(AresEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_HOVERING =
            SynchedEntityData.defineId(AresEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_COUNTDOWN =
            SynchedEntityData.defineId(AresEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> DATA_ACTION =
            SynchedEntityData.defineId(AresEntity.class, EntityDataSerializers.BYTE);

    /**
     * v1.9 护盾状态（同步）：服务端每 tick 按核心温度刷新，客户端 {@code AresRenderer} 用它决定
     * 是否绘制护盾外壳。同理写成 {@code SynchedEntityData}，不在客户端重算温度。
     */
    private static final EntityDataAccessor<Boolean> DATA_SHIELD =
            SynchedEntityData.defineId(AresEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    /** 仅服务端使用的动作倒计时。 */
    private int actionTicks;

    private int coreTemperature = DEFAULT_CORE_TEMPERATURE;
    private int phase = 1;
    /** 阶段三自爆窗口内的近战节奏（其余阶段由 {@link AresMeleeGoal} 自己管冷却）。 */
    private int attackCooldown = 40;
    private int eruptionCooldown = ERUPTION_CD;
    private int summonCooldown = SUMMON_CD;
    private int scaleStormCooldown = SCALE_STORM_CD;
    private int overloadEruptionCooldown = OVERLOAD_ERUPTION_CD;
    private int vulnerabilityTicks;
    private int selfDestructTimer = -1;
    private boolean hovering;
    private boolean selfDestructed;

    // ---- v1.11：前摇调度 / 追踪鳞粉 / Boss 领域 ----
    /** 前摇剩余 tick（&gt;0 时 Boss 站定不动，前摇结束才结算技能）。 */
    private int windupTicks;
    private byte pendingSkill = ACTION_NONE;
    private final List<BlockPos> pendingTargets = new ArrayList<>();
    private final List<ScaleMark> scaleMarks = new ArrayList<>();
    /** Boss 领域中心：首次 tick 的位置（祭坛 / 召唤点），半径 {@link #DOMAIN_RADIUS}。 */
    private BlockPos domainCenter;

    public AresEntity(EntityType<? extends IronGolem> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        // 文档「不脱战」：Boss 永不自然消失（跟随范围 64 由属性负责）
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 800.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                // v1.11：按《Boss属性重新设计》——攻击力 18→12、移速 0.26→0.28、跟随 48→64
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                // v1.10：体型翻倍（Attributes.SCALE 在 1.21.1 存在，范围 0.0625~16）
                .add(Attributes.SCALE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // 自带冷却与前摇的岩浆拳（原版 MeleeAttackGoal 固定 20 tick 一次，达不到文档的 4 秒）
        this.goalSelector.addGoal(2, new AresMeleeGoal(this, 1.1D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 48.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * 岩浆拳的 AI 目标：冷却 {@value #MELEE_INTERVAL_TICKS} tick（4 秒）、前摇 {@value #MELEE_WINDUP_TICKS} tick（0.8 秒）。
     *
     * <p><b>为什么不继承 {@code MeleeAttackGoal}</b>：1.21.1 里它的冷却写在
     * {@code resetAttackCooldown()} 内部，直接对**私有**字段 {@code ticksUntilNextAttack} 赋
     * {@code adjustedTickDelay(20)}；而同样存在的 {@code protected int getAttackInterval()}
     * 在整个类里**没有任何调用点**（拿它当扩展点会静默失效 —— 冷却仍然是 1 秒）。
     * 所以这里自己写一个 {@link Goal}：冷却与前摇都在我们手里，触及范围由
     * {@link AresEntity#getAttackBoundingBox()} 的 4 格判定盒提供。</p>
     */
    private static final class AresMeleeGoal extends Goal {

        private final AresEntity ares;
        private final double speedModifier;
        /** 冷却计数器（tick）。 */
        private int cooldown;
        /** 前摇剩余 tick；-1 表示没有前摇在进行。 */
        private int windup = -1;

        AresMeleeGoal(AresEntity ares, double speedModifier) {
            this.ares = ares;
            this.speedModifier = speedModifier;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.ares.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.cooldown = 0;
            this.windup = -1;
        }

        @Override
        public void stop() {
            this.windup = -1;
            this.ares.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity target = this.ares.getTarget();
            if (target == null) {
                return;
            }
            this.ares.getLookControl().setLookAt(target, 30.0F, 30.0F);

            // 别的技能正在前摇：站定，别把预警动作打断
            if (this.ares.windupTicks > 0) {
                this.ares.getNavigation().stop();
                return;
            }

            // 挥拳前摇：站定不动，前摇结束才真正打出去（玩家这段时间能后撤）
            if (this.windup >= 0) {
                this.ares.getNavigation().stop();
                if (--this.windup == 0) {
                    this.ares.swing(InteractionHand.MAIN_HAND);
                    this.ares.doHurtTarget(target);
                    this.windup = -1;
                    this.cooldown = MELEE_INTERVAL_TICKS;
                }
                return;
            }

            this.ares.getNavigation().moveTo(target, this.speedModifier);
            if (this.cooldown > 0) {
                this.cooldown--;
                return;
            }
            // 4 格触及由 getAttackBoundingBox() 外扩 2 格提供
            if (this.ares.isWithinMeleeAttackRange(target)
                    && this.ares.getSensing().hasLineOfSight(target)) {
                this.windup = MELEE_WINDUP_TICKS;
                this.ares.beginMeleeWindup();
                this.ares.getNavigation().stop();
            }
        }
    }

    // ------------------------------------------------------------------ 状态

    public int getCoreTemperature() {
        return this.coreTemperature;
    }

    /**
     * v1.9 护盾是否展开：{@code coreTemperature > 1500℃}（{@link #SHIELD_TEMPERATURE}）。
     *
     * <p>读的是**同步数据** {@code DATA_SHIELD}，所以服务端（无敌判定）与客户端（护盾渲染）
     * 看到的是同一个值；服务端每 tick 在 {@link #aiStep()} 里刷新它。</p>
     */
    public boolean hasShield() {
        return this.entityData.get(DATA_SHIELD);
    }

    /**
     * 核心温度的**唯一写入口**：夹紧到 {@code [0, MAX_CORE_TEMPERATURE]}，并同时刷新护盾状态。
     *
     * <p>自然升温、冷却器降温、受击升温三条路径都走这里，所以护盾开关永远是温度的忠实函数，
     * 不会出现「温度已经掉到 1500 以下但护盾还挂着」的一 tick 以上延迟。</p>
     */
    private void setCoreTemperature(int value) {
        this.coreTemperature = Mth.clamp(value, 0, MAX_CORE_TEMPERATURE);
        this.entityData.set(DATA_SHIELD, this.coreTemperature > SHIELD_TEMPERATURE);
    }

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    /** 客户端动画用：是否处于升空的风暴形态。 */
    public boolean isHovering() {
        return this.entityData.get(DATA_HOVERING);
    }

    /** 客户端动画用：过载形态剩余自爆倒计时（秒）。 */
    public int getCountdown() {
        return this.entityData.get(DATA_COUNTDOWN);
    }

    public byte getAction() {
        return this.entityData.get(DATA_ACTION);
    }

    private void startAction(byte action, int ticks) {
        this.entityData.set(DATA_ACTION, action);
        this.actionTicks = ticks;
    }

    // ------------------------------------------------------------------ GeckoLib 接线

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, 1);
        builder.define(DATA_HOVERING, false);
        builder.define(DATA_COUNTDOWN, 0);
        builder.define(DATA_ACTION, ACTION_NONE);
        builder.define(DATA_SHIELD, false);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 6, this::mainController));
        controllers.add(new AnimationController<>(this, "action", 0, this::actionController));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }

    @Override
    public double getTick(Object object) {
        return this.tickCount;
    }

    private PlayState mainController(AnimationState<AresEntity> state) {
        if (this.getPhase() == 3) {
            if (this.getCountdown() > 0 && this.getCountdown() <= 10) {
                state.getController().setAnimationSpeed(2.0D).setAnimation(ANIM_SELF_DESTRUCT);
            } else {
                state.getController().setAnimationSpeed(1.0D).setAnimation(ANIM_OVERLOAD);
            }
        } else if (this.isHovering()) {
            state.getController().setAnimationSpeed(1.0D).setAnimation(ANIM_STORM);
        } else if (state.isMoving() && Math.abs(state.getLimbSwingAmount()) > 0.05F) {
            double speed = 0.6D + Math.min(1.4D, Math.abs(state.getLimbSwingAmount()) * 2.0D);
            state.getController().setAnimationSpeed(speed).setAnimation(ANIM_WALK);
        } else {
            state.getController().setAnimationSpeed(1.0D).setAnimation(ANIM_IDLE);
        }
        return PlayState.CONTINUE;
    }

    private PlayState actionController(AnimationState<AresEntity> state) {
        byte action = this.getAction();
        if (action == ACTION_ATTACK) {
            state.getController().setAnimation(ANIM_ATTACK);
            return PlayState.CONTINUE;
        }
        if (action == ACTION_ERUPTION) {
            state.getController().setAnimation(ANIM_ERUPTION);
            return PlayState.CONTINUE;
        }
        if (action == ACTION_STORM) {
            state.getController().setAnimation(ANIM_STORM);
            return PlayState.CONTINUE;
        }
        if (action == ACTION_OVERLOAD) {
            state.getController().setAnimation(ANIM_OVERLOAD);
            return PlayState.CONTINUE;
        }
        if (action == ACTION_SLAM) {
            state.getController().setAnimation(ANIM_SLAM);
            return PlayState.CONTINUE;
        }
        if (action == ACTION_SUMMON) {
            state.getController().setAnimation(ANIM_SUMMON);
            return PlayState.CONTINUE;
        }
        if (action == ACTION_CORE_PURGE) {
            state.getController().setAnimation(ANIM_CORE_PURGE);
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    public boolean isEnraged() {
        return this.coreTemperature > OVERHEAT_TEMPERATURE;
    }

    public boolean isBrittle() {
        return this.coreTemperature < BRITTLE_TEMPERATURE;
    }

    /** 便携式冷却器调用：降低核心温度。 */
    public void applyCoolant(String coolant, int amount) {
        this.setCoreTemperature(this.coreTemperature - amount);
        // v1.9：按用户要求削减受击反馈特效 —— 冷却命中原来是 1.2 音量 + 40 个 CLOUD 粒子，
        // 而冷却枪每 2 tick 就命中一次（≈每秒 400 个粒子），视觉/听觉都过载；
        // 现在音量减到 0.45、粒子砍到 12（约 1/3），保留「打中了」的反馈但不再刷屏。
        //
        // v1.9 二次削减（本轮）：粒子再砍一半 12 → 6，并**删掉** Boss 自己那条 FIRE_EXTINGUISH ——
        // 冷却枪每次命中已经在玩家侧播了 GENERIC_SPLASH（0.20），Boss 再加一条每 2 tick 一次的
        // 灭火音纯属重复刷屏。命中反馈现在只剩 6 个 CLOUD 粒子 + 枪自己的水花声。
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    this.getX(), this.getY() + 1.5D, this.getZ(), 6, 0.8D, 0.8D, 0.8D, 0.06D);
        }
    }

    // ------------------------------------------------------------------ 每 tick

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.updatePhase(serverLevel);
        this.updateCoreTemperature();
        // v1.9：护盾状态刷新（温度可能刚被本 tick 的自然升温 / 冷却改变），再同步给客户端渲染
        this.entityData.set(DATA_SHIELD, this.coreTemperature > SHIELD_TEMPERATURE);
        this.updateStatusEffects();
        this.updateBossBar();
        // v1.11：Boss 领域（祭坛 48 格）与飞行中的追踪鳞粉
        this.enforceDomain();
        this.tickScaleMarks(serverLevel);

        // 动作动画计时（服务端）
        if (this.actionTicks > 0 && --this.actionTicks == 0) {
            this.entityData.set(DATA_ACTION, ACTION_NONE);
        }

        // 脆弱期：坠落、停滞、核心暴露
        if (this.vulnerabilityTicks > 0) {
            this.vulnerabilityTicks--;
            this.getNavigation().stop();
            if (this.tickCount % 10 == 0) {
                serverLevel.sendParticles(ParticleTypes.LAVA,
                        this.getX(), this.getY() + 1.0D, this.getZ(), 8, 0.6D, 0.6D, 0.6D, 0.0D);
            }
            return;
        }

        if (this.hovering) {
            this.tickHovering(serverLevel);
        }

        this.tickAttacks(serverLevel);
    }

    private void updatePhase(ServerLevel level) {
        float ratio = this.getHealth() / this.getMaxHealth();
        int newPhase = ratio > 0.66F ? 1 : (ratio > 0.33F ? 2 : 3);

        if (newPhase == this.phase) {
            return;
        }
        this.phase = newPhase;
        this.entityData.set(DATA_PHASE, newPhase);

        if (newPhase == 2) {
            // 风暴形态：升空并召唤强化沙尘暴
            this.hovering = true;
            this.setNoGravity(true);
            this.entityData.set(DATA_HOVERING, true);
            this.startAction(ACTION_STORM, 40);
            summonSandstorm(level);
            this.broadcast(level, "message.northstar_planet_addon.ares.phase2");
            // 仅表现：环形冲击波（不参与任何判定）
            this.spawnSkillFx(level, SkillFx.PHASE_SHOCKWAVE);
        } else if (newPhase == 3) {
            // 过载形态：停止移动、核心完全暴露、60 秒自爆倒计时
            this.hovering = false;
            this.setNoGravity(false);
            this.entityData.set(DATA_HOVERING, false);
            this.getNavigation().stop();
            this.selfDestructTimer = SELF_DESTRUCT_TICKS;
            // 文档「核心暴露」：护甲归零
            this.breakArmor();
            // 风暴形态结束 → 收回它召唤的沙尘暴（否则会一直刮到 10 分钟到期）
            this.endBossSandstorm();
            this.broadcast(level, "message.northstar_planet_addon.ares.phase3");
            // 仅表现：核心过载爆发 + core_purge 动作
            this.spawnSkillFx(level, SkillFx.CORE_OVERLOAD);
            this.startAction(ACTION_CORE_PURGE, 30);
        } else {
            this.broadcast(level, "message.northstar_planet_addon.ares.phase1");
        }
        this.playSound(SoundEvents.WITHER_AMBIENT, 2.0F, 0.5F);
    }

    private void updateCoreTemperature() {
        if (this.tickCount % 20 == 0) {
            this.setCoreTemperature(this.coreTemperature + 15 + this.phase * 5);
        }

        // 阶段二冷却窗口：核心温度 > 500℃ 时回血 10 HP/s（压到 500 以下才停）
        if (this.phase == 2 && this.hovering && !this.isBrittle() && this.tickCount % 10 == 0) {
            this.heal(COOLING_HEAL_PER_TICK);
        }
        if (this.phase == 2 && this.hovering && this.isBrittle()) {
            this.hovering = false;
            this.setNoGravity(false);
            this.entityData.set(DATA_HOVERING, false);
            this.vulnerabilityTicks = VULNERABILITY_TICKS;
            // 被冷却打落 = 风暴形态中断，同样收回沙尘暴
            this.endBossSandstorm();
            this.setDeltaMovement(this.getDeltaMovement().x, -0.8D, this.getDeltaMovement().z);
            if (this.level() instanceof ServerLevel serverLevel) {
                this.broadcast(serverLevel, "message.northstar_planet_addon.ares.cooled");
                // 仅表现：坠落砸地（slam 动作 + 地面尘浪），不改任何计时 / 伤害
                this.startAction(ACTION_SLAM, 24);
                this.spawnSkillFx(serverLevel, SkillFx.SLAM);
            }
        }
    }

    private void updateStatusEffects() {
        // 狂暴：移速 +30%（精确属性修饰符）；攻击力 +50% 由 doHurtTarget 负责
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (this.isEnraged()) {
                speed.addOrUpdateTransientModifier(ENRAGED_SPEED);
            } else {
                speed.removeModifier(ENRAGED_SPEED.id());
            }
        }
        if (this.isBrittle()) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
        }
    }

    private void updateBossBar() {
        this.bossEvent.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F));
        this.bossEvent.setName(Component.translatable("boss.northstar_planet_addon.ares.bar",
                Component.translatable("entity.northstar_planet_addon.ares"),
                Component.translatable("boss.northstar_planet_addon.ares.phase" + this.phase),
                this.coreTemperature));
    }

    private void tickHovering(ServerLevel level) {
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.getBlockX(), this.getBlockZ());
        double targetY = surface + HOVER_HEIGHT;
        double delta = Mth.clamp((targetY - this.getY()) * 0.1D, -0.4D, 0.4D);
        this.setDeltaMovement(this.getDeltaMovement().x, delta, this.getDeltaMovement().z);
        this.fallDistance = 0.0F;

        if (this.tickCount % 20 == 0 && !Sandstorm.isActive(this.level())) {
            summonSandstorm(level);
        }
    }

    /**
     * 召唤强化沙尘暴。
     *
     * <p>只在火星执行：沙尘暴是**维度级**状态（见 {@code world.Sandstorm}），
     * 用刷怪蛋把阿瑞斯放到主世界时 {@code trigger} 会拒绝并打警告 ——
     * 而 {@link #tickHovering} 每秒都会重试一次，所以这里必须先自己判维度，否则日志会被刷屏。</p>
     */
    private void summonSandstorm(ServerLevel level) {
        if (Sandstorm.isMars(level)) {
            Sandstorm.trigger(level, Sandstorm.DEFAULT_DURATION, true, Sandstorm.Origin.BOSS);
        }
    }

    /**
     * Boss 退场时收掉它召唤的沙尘暴。
     *
     * <p>用户反馈的 bug：Boss 死了、沙尘暴还在刮十分钟。原因是 {@link Sandstorm#trigger} 一开就是
     * {@link Sandstorm#DEFAULT_DURATION} tick，而阶段二里 {@link #tickHovering} 每秒还会续期，
     * Boss 一死就再没人去收它。现在三条退场路径都会调用这里：</p>
     * <ul>
     *   <li>{@link #die(DamageSource)}：被玩家打死 / 自爆；</li>
     *   <li>{@link #remove(Entity.RemovalReason)} 且原因是 {@code KILLED}：{@code /kill} 之类的直接移除
     *       （{@code Entity#kill()} 走的是 remove，不经过 die）；</li>
     *   <li>离开风暴形态（进入阶段三 / 被冷却打落）—— 形态结束了风暴就不该继续。</li>
     * </ul>
     * <p>都是幂等的：不是 Boss 召唤的那场沙尘暴不受影响。</p>
     */
    private void endBossSandstorm() {
        if (this.level() instanceof ServerLevel serverLevel) {
            Sandstorm.stopIfFromBoss(serverLevel);
        }
    }

    @Override
    public void die(DamageSource cause) {
        this.endBossSandstorm();
        super.die(cause);
    }

    // ------------------------------------------------------------------ v1.11：领域 / 免疫 / 前摇

    /** 由祭坛在召唤时指定领域中心（否则取第一次 tick 的位置）。 */
    public void setDomainCenter(BlockPos center) {
        this.domainCenter = center.immutable();
    }

    /** 目标是否落在 Boss 领域内（领域中心还没确定时一律算「在」）。 */
    private boolean isInsideDomain(Vec3 position) {
        return this.domainCenter == null
                || position.distanceToSqr(Vec3.atCenterOf(this.domainCenter)) <= DOMAIN_RADIUS * DOMAIN_RADIUS;
    }

    /**
     * 领域约束（文档：「不脱战，祭坛 48 格内为 Boss 领域」）。
     *
     * <ul>
     *   <li>领域中心 = Boss 第一次 tick 的位置（祭坛 / 召唤点）；</li>
     *   <li>跑出领域的目标会被解除锁定：Boss 不追出领域，但也不会因此脱战重置；</li>
     *   <li>没有目标时走回领域中心待机。</li>
     * </ul>
     */
    private void enforceDomain() {
        if (this.domainCenter == null) {
            this.domainCenter = this.blockPosition();
            return;
        }
        LivingEntity target = this.getTarget();
        if (target != null && !this.isInsideDomain(target.position())) {
            this.setTarget(null);
            target = null;
        }
        if (target == null && !this.hovering && this.vulnerabilityTicks <= 0
                && this.distanceToSqr(Vec3.atCenterOf(this.domainCenter)) > 64.0D) {
            this.getNavigation().moveTo(this.domainCenter.getX() + 0.5D, this.domainCenter.getY(),
                    this.domainCenter.getZ() + 0.5D, 1.0D);
        }
    }

    /**
     * 目标选择时就把领域外的玩家挡掉。
     *
     * <p>只靠 {@link #enforceDomain()} 也行，但那样 Boss 会先锁定再放弃，每隔几 tick 抖一次；
     * 在入口处拦掉更干净。</p>
     */
    @Override
    public void setTarget(LivingEntity target) {
        if (target != null && !this.isInsideDomain(target.position())) {
            return;
        }
        super.setTarget(target);
    }

    /** 免疫火焰 / 岩浆（{@code #minecraft:is_fire} 含 lava 与 hot_floor）。 */
    @Override
    public boolean fireImmune() {
        return true;
    }

    /** 免疫火焰、岩浆、溺水、坠落（文档「免疫」一栏）。 */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_DROWNING)
                || source.is(DamageTypeTags.IS_FALL)
                || super.isInvulnerableTo(source);
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    /** 岩浆拳的触及范围：文档「4 格」。体型已是 ×2，判定盒再外扩 2 格。 */
    @Override
    protected AABB getAttackBoundingBox() {
        return super.getAttackBoundingBox().inflate(2.0D);
    }

    /** 岩浆拳前摇（由 {@link AresMeleeGoal} 在挥拳前 0.8 秒调用）。 */
    private void beginMeleeWindup() {
        Level level = this.level();
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BLAZE_BURN, SoundSource.HOSTILE, 1.4F, 0.6F);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    this.getX(), this.getY() + 2.5D, this.getZ(), 24, 0.8D, 0.8D, 0.8D, 0.03D);
        }
    }

    private void tickAttacks(ServerLevel level) {
        // ---- 前摇中：站定不动，前摇结束才真的把技能放出去（玩家这段时间可以跑开 / 找掩体）----
        if (this.windupTicks > 0) {
            this.getNavigation().stop();
            if (this.tickCount % 5 == 0) {
                this.windupTelegraph(level);
            }
            if (--this.windupTicks == 0) {
                this.executePendingSkill(level);
            }
            return;
        }

        // 过载形态：不再移动，持续全图地热喷发，并跑自爆倒计时
        if (this.phase == 3) {
            this.getNavigation().stop();
            this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
            if (this.tickCount % 20 == 0) {
                this.entityData.set(DATA_COUNTDOWN, Math.max(0, this.selfDestructTimer / 20));
            }
            // 仅表现：过载窗口内持续抖动（overload 动作），最后 10 秒交给 self_destruct
            if (this.actionTicks <= 0 && this.selfDestructTimer > 200) {
                this.startAction(ACTION_OVERLOAD, 40);
            }
            if (this.overloadEruptionCooldown-- <= 0) {
                this.overloadEruptionCooldown = OVERLOAD_ERUPTION_CD;
                this.startWindup(ACTION_CORE_PURGE, OVERLOAD_ERUPTION_WINDUP,
                        this.pickPointsNearPlayers(level));
            }
            if (this.selfDestructTimer > 0 && --this.selfDestructTimer == 0) {
                this.selfDestruct(level);
            }
            return;
        }

        // 阶段一 / 二：岩浆拳由 AresMeleeGoal 自己跑（冷却 4 秒、前摇 0.8 秒）
        this.attackCooldown = 0;

        if (this.eruptionCooldown-- <= 0) {
            this.eruptionCooldown = ERUPTION_CD;
            this.startWindup(ACTION_ERUPTION, ERUPTION_WINDUP,
                    this.pickEruptionPoints(level, ERUPTION_POINTS));
        }
        if (this.summonCooldown-- <= 0) {
            this.summonCooldown = SUMMON_CD;
            this.startWindup(ACTION_SUMMON, SUMMON_WINDUP, List.of());
        }
        if (this.phase == 2 && this.scaleStormCooldown-- <= 0) {
            this.scaleStormCooldown = SCALE_STORM_CD;
            this.startWindup(ACTION_STORM, SCALE_STORM_WINDUP, List.of());
        }
    }

    // ------------------------------------------------------------------ v1.11：前摇调度

    /** 起一次前摇：站定、播动作、记下已经选好的落点。 */
    private void startWindup(byte skill, int ticks, List<BlockPos> targets) {
        this.pendingSkill = skill;
        this.pendingTargets.clear();
        this.pendingTargets.addAll(targets);
        this.windupTicks = ticks;
        this.startAction(skill, ticks + 12);
        this.getNavigation().stop();
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.BLAZE_BURN, SoundSource.HOSTILE, 1.6F, 0.7F);
    }

    /** 前摇期间每 5 tick 画一次预警：落点冒烟、Boss 身边起鳞粉。 */
    private void windupTelegraph(ServerLevel level) {
        switch (this.pendingSkill) {
            case ACTION_ERUPTION, ACTION_CORE_PURGE -> {
                for (BlockPos pos : this.pendingTargets) {
                    level.sendParticles(ParticleTypes.SMOKE,
                            pos.getX() + 0.5D, pos.getY() + 0.25D, pos.getZ() + 0.5D,
                            4, 0.45D, 0.1D, 0.45D, 0.01D);
                    level.sendParticles(ParticleTypes.LAVA,
                            pos.getX() + 0.5D, pos.getY() + 0.25D, pos.getZ() + 0.5D,
                            1, 0.3D, 0.05D, 0.3D, 0.0D);
                }
            }
            case ACTION_SUMMON -> level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    this.getX(), this.getY() + 1.5D, this.getZ(), 40, 2.5D, 1.0D, 2.5D, 0.02D);
            case ACTION_STORM -> level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    this.getX(), this.getY() + 2.0D, this.getZ(), 60, 5.0D, 2.0D, 5.0D, 0.05D);
            default -> {
            }
        }
    }

    /** 前摇结束：把技能真正放出去。 */
    private void executePendingSkill(ServerLevel level) {
        byte skill = this.pendingSkill;
        this.pendingSkill = ACTION_NONE;
        List<BlockPos> targets = List.copyOf(this.pendingTargets);
        this.pendingTargets.clear();
        switch (skill) {
            case ACTION_ERUPTION -> this.eruptAt(level, targets,
                    ERUPTION_DAMAGE, ERUPTION_RADIUS, ERUPTION_FIRE_TICKS);
            case ACTION_CORE_PURGE -> this.eruptAt(level, targets,
                    OVERLOAD_DAMAGE, OVERLOAD_RADIUS, ERUPTION_FIRE_TICKS);
            case ACTION_SUMMON -> this.summonMoths(level);
            case ACTION_STORM -> this.scaleStorm(level);
            default -> {
            }
        }
    }

    /** 阶段三「核心暴露」：护甲归零。 */
    private void breakArmor() {
        AttributeInstance armor = this.getAttribute(Attributes.ARMOR);
        if (armor != null && !armor.hasModifier(BROKEN_ARMOR.id())) {
            armor.addOrUpdateTransientModifier(BROKEN_ARMOR);
            MarsModule.LOGGER.info("Ares core exposed: armor dropped to 0");
        }
    }

    // ------------------------------------------------------------------ 技能

    /**
     * 地热喷发选点：文档「随机 6 点」，每点取某个玩家周围 ±4 格的地表。
     *
     * <p>选点在前摇**开始**时就定好，前摇期间会把落点画出来 —— 玩家能看着坑跑开，
     * 这就是文档「反制：移动离开预警」的实现方式。</p>
     */
    private List<BlockPos> pickEruptionPoints(ServerLevel level, int count) {
        List<Player> players = level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(ERUPTION_SEARCH));
        if (players.isEmpty()) {
            return List.of();
        }
        List<BlockPos> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Player player = players.get(this.random.nextInt(players.size()));
            int x = player.getBlockX() + this.random.nextInt(9) - 4;
            int z = player.getBlockZ() + this.random.nextInt(9) - 4;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            points.add(new BlockPos(x, y, z));
        }
        return points;
    }

    /** 阶段三「全图地热喷发」选点：每个玩家脚下各一个。 */
    private List<BlockPos> pickPointsNearPlayers(ServerLevel level) {
        List<Player> players = level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(ERUPTION_SEARCH));
        List<BlockPos> points = new ArrayList<>(players.size());
        for (Player player : players) {
            int x = player.getBlockX() + this.random.nextInt(5) - 2;
            int z = player.getBlockZ() + this.random.nextInt(5) - 2;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            points.add(new BlockPos(x, y, z));
        }
        return points;
    }

    /**
     * 在给定落点上喷发：半径内的非友方实体吃伤害并着火。
     *
     * <p>文档：地热喷发 8 点 / 半径 2 / 着火 5 秒；阶段三全图喷发 10 点 / 半径 2 / 着火 5 秒。</p>
     */
    private void eruptAt(ServerLevel level, List<BlockPos> points, float damage, double radius, int fireTicks) {
        if (points.isEmpty()) {
            return;
        }
        // 仅表现：以 Boss 为中心的一圈熔岩 / 火焰爆开
        this.spawnSkillFx(level, SkillFx.ERUPT);
        for (BlockPos pos : points) {
            double x = pos.getX() + 0.5D;
            double y = pos.getY() + 0.5D;
            double z = pos.getZ() + 0.5D;
            level.sendParticles(ParticleTypes.LAVA, x, y, z, 25, radius * 0.3D, 0.8D, radius * 0.3D, 0.0D);
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 15, radius * 0.25D, 1.0D, radius * 0.25D, 0.02D);
            level.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.HOSTILE, 1.0F, 0.8F);

            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(pos).inflate(radius), e -> !(e instanceof AresEntity))) {
                // 友军保护：地热喷发的 AoE 不波及恶魔蛾（hurt 不经过 canAttack）
                if (entity instanceof DemonMothEntity) {
                    continue;
                }
                entity.hurt(this.damageSources().inFire(), damage);
                entity.setRemainingFireTicks(fireTicks);
            }
        }
    }

    /** 召唤火星恶魔蛾：文档「2–4 只」。 */
    private void summonMoths(ServerLevel level) {
        // 仅表现：暗红漩涡
        this.spawnSkillFx(level, SkillFx.SUMMON);
        int amount = SUMMON_MIN + this.random.nextInt(SUMMON_MAX - SUMMON_MIN + 1);
        for (int i = 0; i < amount; i++) {
            DemonMothEntity moth = ModEntities.DEMON_MOTH.get().create(level);
            if (moth == null) {
                continue;
            }
            moth.moveTo(this.getX() + this.random.nextDouble() * 6.0D - 3.0D, this.getY() + 2.0D,
                    this.getZ() + this.random.nextDouble() * 6.0D - 3.0D, this.random.nextFloat() * 360.0F, 0.0F);
            moth.markSummoned();   // v1.10：Boss 的召唤物掉落与经验一律为无
            moth.setPhantomSize(3);
            level.addFreshEntity(moth);
        }
        MarsModule.LOGGER.info("Ares summoned {} demon moths", amount);
    }

    /**
     * 鳞粉风暴：文档「8 枚追踪、命中 6 点、持续伤害 5 秒」，同时维持强化沙尘暴。
     *
     * <p>8 枚鳞粉以 {@link ScaleMark} 表示（不是实体）：会持续朝目标飞，撞到方块（掩体）就消散 ——
     * 对应文档「反制：躲避、掩体」。</p>
     */
    private void scaleStorm(ServerLevel level) {
        level.sendParticles(ParticleTypes.CRIMSON_SPORE,
                this.getX(), this.getY() + 2.0D, this.getZ(), 240, 6.0D, 3.0D, 6.0D, 0.05D);
        level.playSound(null, this.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 2.0F, 0.5F);
        // 仅表现：storm 动作 + 半径上的沙尘环
        this.startAction(ACTION_STORM, 30);
        this.spawnSkillFx(level, SkillFx.STORM);

        List<Player> players = level.getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(SCALE_STORM_RANGE));
        for (int i = 0; i < SCALE_STORM_MARKS && !players.isEmpty(); i++) {
            Player target = players.get(i % players.size());
            double angle = i * (Math.PI * 2.0D / SCALE_STORM_MARKS);
            this.scaleMarks.add(new ScaleMark(target.getUUID(),
                    this.getX() + Math.cos(angle) * 3.0D,
                    this.getY() + 2.0D,
                    this.getZ() + Math.sin(angle) * 3.0D));
        }
        summonSandstorm(level);
    }

    /** 一枚追踪鳞粉：朝目标飞，撞到方块就消散，命中造成伤害并施加腐蚀。 */
    private static final class ScaleMark {

        final UUID target;
        double x;
        double y;
        double z;
        int age;

        ScaleMark(UUID target, double x, double y, double z) {
            this.target = target;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    /** 推进所有飞行中的鳞粉（见 {@link #scaleStorm}）。 */
    private void tickScaleMarks(ServerLevel level) {
        if (this.scaleMarks.isEmpty()) {
            return;
        }
        Iterator<ScaleMark> iterator = this.scaleMarks.iterator();
        while (iterator.hasNext()) {
            ScaleMark mark = iterator.next();
            Player player = level.getPlayerByUUID(mark.target);
            mark.age++;
            if (player == null || !player.isAlive() || player.isCreative()
                    || mark.age > SCALE_MARK_MAX_AGE) {
                iterator.remove();
                continue;
            }
            Vec3 from = new Vec3(mark.x, mark.y, mark.z);
            Vec3 to = player.getEyePosition();
            Vec3 delta = to.subtract(from);
            double distance = delta.length();

            // 掩体：射线被方块挡住 → 这枚鳞粉消散（文档「反制：躲避、掩体」）
            BlockHitResult blockHit = level.clip(new ClipContext(from, to,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (blockHit.getType() != HitResult.Type.MISS) {
                iterator.remove();
                continue;
            }

            if (distance <= 1.0D) {
                // 命中：6 点伤害；腐蚀是否生效由「耐腐蚀」决定（火星钢护甲 / 涂层）
                player.hurt(this.damageSources().magic(), SCALE_MARK_DAMAGE);
                if (!player.hasEffect(ModEffects.CORROSION_RESISTANCE)) {
                    player.addEffect(new MobEffectInstance(ModEffects.CORROSION,
                            SCALE_MARK_CORROSION_TICKS, 1), this);
                }
                iterator.remove();
                continue;
            }

            Vec3 step = delta.normalize().scale(Math.min(SCALE_MARK_SPEED, distance));
            mark.x += step.x;
            mark.y += step.y;
            mark.z += step.z;
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, mark.x, mark.y, mark.z,
                    3, 0.08D, 0.08D, 0.08D, 0.0D);
        }
    }

    // ------------------------------------------------------------------ 技能表现（特效）

    /** 技能特效类型：只用原版粒子 / 音效，不含任何战斗数值。 */
    private enum SkillFx {
        /** 熔岩环爆（地热喷发）。 */
        ERUPT,
        /** 暗红漩涡（召唤恶魔蛾）。 */
        SUMMON,
        /** 沙尘环（鳞粉风暴）。 */
        STORM,
        /** 阶段切换环形冲击波。 */
        PHASE_SHOCKWAVE,
        /** 核心过载爆发。 */
        CORE_OVERLOAD,
        /** 落地砸击。 */
        SLAM
    }

    // ---- 特效预算：这些特效每次技能触发只调用一次，单次调用超过预算会明显卡客户端 ----

    /** 单次技能特效用到的粒子总量上限。 */
    private static final int FX_PARTICLE_BUDGET = 1500;

    /** 熔火系三层同心冲击波：由内到外 LAVA / FLAME / LARGE_SMOKE。 */
    private static final ParticleOptions[] RING_HOT =
            {ParticleTypes.LAVA, ParticleTypes.FLAME, ParticleTypes.LARGE_SMOKE};

    /** 沙尘系三层同心冲击波：由内到外 WHITE_ASH / CLOUD / SMOKE。 */
    private static final ParticleOptions[] RING_DUST =
            {ParticleTypes.WHITE_ASH, ParticleTypes.CLOUD, ParticleTypes.SMOKE};

    /** 三层同心冲击波的半径（格），依次 3 / 7 / 12。 */
    private static final double[] RING_RADII = {3.0D, 7.0D, 12.0D};

    /** 每层的角度步长（度），全部 ≤ 6°：6° / 5° / 3° → 60 / 72 / 120 个采样点。 */
    private static final double[] RING_STEP_DEG = {6.0D, 5.0D, 3.0D};

    /** 每个采样点撒出的粒子数：(60 + 72 + 120) * 2 = 504 个。 */
    private static final int RING_PER_POINT = 2;

    /** 冲击波粒子的扩散速度。 */
    private static final double RING_SPEED = 0.03D;

    /** 垂直喷柱高度（格）与每格粒子数：8 * 3 = 24 个。 */
    private static final int JET_HEIGHT = 8;
    private static final int JET_PER_LEVEL = 3;

    /** 核心过载的螺旋上升：72 步 * 3 = 216 个粒子。 */
    private static final int OVERLOAD_SPIRAL_STEPS = 72;
    private static final int OVERLOAD_SPIRAL_PER_STEP = 3;

    /** 核心过载的膨胀环：内外两圈半径（格）与采样点数 → (84 + 60) * 2 = 288 个粒子。 */
    private static final double[] OVERLOAD_RING_RADII = {3.0D, 7.0D};
    private static final int[] OVERLOAD_RING_POINTS = {84, 60};
    private static final int OVERLOAD_RING_PER_POINT = 2;

    /** 核心过载的强闪光 / 爆点 / 浓烟：4 + 3 + 60 个粒子。 */
    private static final int OVERLOAD_FLASH_COUNT = 4;
    private static final int OVERLOAD_EMITTER_COUNT = 3;
    private static final int OVERLOAD_SMOKE_COUNT = 60;

    /** 召唤的螺旋臂：3 条 * 36 步 * 2 = 216 个粒子（臂的半径与高度都渐变）。 */
    private static final int SUMMON_ARMS = 3;
    private static final int SUMMON_ARM_STEPS = 36;
    private static final int SUMMON_ARM_PER_STEP = 2;

    /** 召唤的地面法阵：外圈 REVERSE_PORTAL + 内圈 PORTAL，72 * 2 * 2 = 288 个粒子。 */
    private static final int SUMMON_CIRCLE_POINTS = 72;
    private static final int SUMMON_CIRCLE_PER_POINT = 2;

    // 各技能的实发粒子数（集中写清楚，并由 particleCountOf 做预算自检）
    private static final int FX_PARTICLES_ERUPT = 528;           // 冲击波 504 + 喷柱 24
    private static final int FX_PARTICLES_SUMMON = 504;          // 螺旋臂 216 + 地面法阵 288
    private static final int FX_PARTICLES_STORM = 528;           // 冲击波 504 + 喷柱 24
    private static final int FX_PARTICLES_PHASE_SHOCKWAVE = 529; // 冲击波 504 + 喷柱 24 + 闪光 1
    private static final int FX_PARTICLES_CORE_OVERLOAD = 571;   // 螺旋 216 + 膨胀环 288 + 闪光 4 + 爆点 3 + 烟 60
    private static final int FX_PARTICLES_SLAM = 528;            // 冲击波 504 + 喷柱 24

    /** 各技能的实发粒子数，仅用于 {@link #FX_PARTICLE_BUDGET} 自检（超出才记一条 WARN）。 */
    private static int particleCountOf(SkillFx fx) {
        return switch (fx) {
            case ERUPT -> FX_PARTICLES_ERUPT;
            case SUMMON -> FX_PARTICLES_SUMMON;
            case STORM -> FX_PARTICLES_STORM;
            case PHASE_SHOCKWAVE -> FX_PARTICLES_PHASE_SHOCKWAVE;
            case CORE_OVERLOAD -> FX_PARTICLES_CORE_OVERLOAD;
            case SLAM -> FX_PARTICLES_SLAM;
        };
    }

    /**
     * 多层同心冲击波：3 圈（半径 3 / 7 / 12），每圈沿圆周撒粒子，角度步长 ≤ 6°，
     * 圈与圈用不同粒子类型（见 {@code types}）。合计 504 个粒子。
     */
    private void shockwaveRings(ServerLevel level, double ringY, ParticleOptions[] types, double speed) {
        for (int layer = 0; layer < RING_RADII.length; layer++) {
            double radius = RING_RADII[layer];
            int points = (int) Math.round(360.0D / RING_STEP_DEG[layer]);
            ParticleOptions type = types[layer % types.length];
            for (int i = 0; i < points; i++) {
                double angle = i * (Math.PI * 2.0D / points);
                double px = this.getX() + Math.cos(angle) * radius;
                double pz = this.getZ() + Math.sin(angle) * radius;
                level.sendParticles(type, px, ringY, pz, RING_PER_POINT, 0.25D, 0.20D, 0.25D, speed);
            }
        }
    }

    /**
     * 垂直喷柱：以 Boss 为中心向上喷 1 根粒子柱，高 {@link #JET_HEIGHT} 格、
     * 每格 {@link #JET_PER_LEVEL} 个粒子（合计 24 个）。
     */
    private void verticalJet(ServerLevel level, ParticleOptions type, double speed) {
        for (int i = 0; i < JET_HEIGHT; i++) {
            double py = this.getY() + 0.5D + i;
            double spread = 0.18D + i * 0.02D;
            level.sendParticles(type, this.getX(), py, this.getZ(), JET_PER_LEVEL, spread, 0.05D, spread, speed);
        }
    }

    /**
     * 统一的技能特效辅助方法：全部走服务端原版粒子 + 原版音效，
     * 不注册新粒子类型（避免额外资源文件），也不触碰任何战斗数值与状态机判定。
     */
    private void spawnSkillFx(ServerLevel level, SkillFx fx) {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        int particles = particleCountOf(fx);
        if (particles > FX_PARTICLE_BUDGET) {
            MarsModule.LOGGER.warn("Ares skill fx {} exceeds particle budget: {} > {}",
                    fx, particles, FX_PARTICLE_BUDGET);
        }
        switch (fx) {
            case ERUPT -> {
                // 熔岩环爆：三层同心冲击波（LAVA / FLAME / LARGE_SMOKE）+ 垂直火柱，共 528 个粒子
                level.playSound(null, x, y + 1.0D, z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.7F);
                level.playSound(null, x, y + 1.0D, z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.6F, 0.6F);
                level.playSound(null, x, y + 1.0D, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.2F, 1.5F);
                this.shockwaveRings(level, y + 0.4D, RING_HOT, RING_SPEED);
                this.verticalJet(level, ParticleTypes.FLAME, 0.06D);
            }
            case SUMMON -> {
                // 暗红漩涡：3 条半径渐变的螺旋臂 + 地面法阵（REVERSE_PORTAL / PORTAL），共 504 个粒子
                level.playSound(null, x, y + 1.5D, z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 2.0F, 0.6F);
                level.playSound(null, x, y + 1.5D, z, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.4F, 0.5F);
                level.playSound(null, x, y + 1.5D, z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 0.9F, 1.4F);
                for (int arm = 0; arm < SUMMON_ARMS; arm++) {
                    double armOffset = arm * (Math.PI * 2.0D / SUMMON_ARMS);
                    for (int i = 0; i < SUMMON_ARM_STEPS; i++) {
                        double t = i / (double) SUMMON_ARM_STEPS;
                        // 角度随步数递增 ≈ 1.5 圈，半径 1.2 → 4.8 格，同时螺旋上升
                        double angle = armOffset + t * Math.PI * 3.0D;
                        double radius = 1.2D + t * 3.6D;
                        double px = x + Math.cos(angle) * radius;
                        double pz = z + Math.sin(angle) * radius;
                        double py = y + 0.4D + t * 3.2D;
                        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz,
                                SUMMON_ARM_PER_STEP, 0.05D, 0.05D, 0.05D, 0.0D);
                    }
                }
                for (int i = 0; i < SUMMON_CIRCLE_POINTS; i++) {
                    double angle = i * (Math.PI * 2.0D / SUMMON_CIRCLE_POINTS);
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            x + Math.cos(angle) * 3.5D, y + 0.15D, z + Math.sin(angle) * 3.5D,
                            SUMMON_CIRCLE_PER_POINT, 0.05D, 0.05D, 0.05D, 0.0D);
                    level.sendParticles(ParticleTypes.PORTAL,
                            x + Math.cos(-angle) * 2.2D, y + 0.25D, z + Math.sin(-angle) * 2.2D,
                            SUMMON_CIRCLE_PER_POINT, 0.05D, 0.05D, 0.05D, 0.02D);
                }
            }
            case STORM -> {
                // 沙尘环：三层同心（WHITE_ASH / CLOUD / SMOKE）+ 垂直尘柱，共 528 个粒子
                level.playSound(null, x, y + 2.0D, z, SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 2.0F, 1.4F);
                level.playSound(null, x, y + 2.0D, z, SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.0F, 1.3F);
                level.playSound(null, x, y + 2.0D, z, SoundEvents.BLAZE_BURN, SoundSource.HOSTILE, 0.8F, 0.7F);
                this.shockwaveRings(level, y + 0.8D, RING_DUST, 0.04D);
                this.verticalJet(level, ParticleTypes.WHITE_ASH, 0.05D);
            }
            case PHASE_SHOCKWAVE -> {
                // 阶段二入场：三层同心冲击波 + 垂直尘柱 + 1 次强闪光，共 529 个粒子
                level.playSound(null, x, y + 1.0D, z, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3.0F, 0.6F);
                level.playSound(null, x, y + 1.0D, z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.5F, 0.5F);
                level.playSound(null, x, y + 1.0D, z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.4F, 0.5F);
                this.shockwaveRings(level, y + 0.6D, RING_HOT, RING_SPEED);
                this.verticalJet(level, ParticleTypes.LARGE_SMOKE, 0.05D);
                level.sendParticles(ParticleTypes.FLASH, x, y + 1.6D, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            case CORE_OVERLOAD -> {
                // 阶段三入场：核心位置螺旋上升 + 膨胀环 + 强闪光，共 571 个粒子
                level.playSound(null, x, y + 1.5D, z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2.5F, 0.7F);
                level.playSound(null, x, y + 1.5D, z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.2F, 0.6F);
                level.playSound(null, x, y + 1.5D, z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.8F, 0.4F);
                double coreY = y + 1.8D; // 模型上的星核（core）位置
                for (int i = 0; i < OVERLOAD_SPIRAL_STEPS; i++) {
                    double t = i / (double) OVERLOAD_SPIRAL_STEPS;
                    double angle = t * Math.PI * 6.0D; // 3 圈，角度随循环变量递增
                    double radius = 0.6D + t * 2.4D;
                    double px = x + Math.cos(angle) * radius;
                    double pz = z + Math.sin(angle) * radius;
                    double py = coreY - 0.6D + t * 4.0D;
                    level.sendParticles(ParticleTypes.LAVA, px, py, pz,
                            OVERLOAD_SPIRAL_PER_STEP, 0.05D, 0.05D, 0.05D, 0.02D);
                }
                for (int ring = 0; ring < OVERLOAD_RING_RADII.length; ring++) {
                    double radius = OVERLOAD_RING_RADII[ring];
                    int points = OVERLOAD_RING_POINTS[ring];
                    for (int i = 0; i < points; i++) {
                        double angle = i * (Math.PI * 2.0D / points);
                        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                x + Math.cos(angle) * radius, y + 0.5D, z + Math.sin(angle) * radius,
                                OVERLOAD_RING_PER_POINT, 0.2D, 0.2D, 0.2D, 0.05D);
                    }
                }
                level.sendParticles(ParticleTypes.FLASH, x, coreY, z,
                        OVERLOAD_FLASH_COUNT, 0.6D, 0.6D, 0.6D, 0.0D);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, coreY, z,
                        OVERLOAD_EMITTER_COUNT, 1.0D, 0.6D, 1.0D, 0.0D);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, x, coreY, z,
                        OVERLOAD_SMOKE_COUNT, 1.2D, 1.2D, 1.2D, 0.03D);
            }
            case SLAM -> {
                // 落地砸击：贴地三层同心尘浪 + 垂直尘柱，共 528 个粒子
                level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 2.0F, 0.6F);
                level.playSound(null, x, y, z, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.8F, 0.7F);
                level.playSound(null, x, y, z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 2.0F, 0.8F);
                this.shockwaveRings(level, y + 0.2D, RING_DUST, 0.05D);
                this.verticalJet(level, ParticleTypes.LARGE_SMOKE, 0.04D);
            }
        }
    }

    /** 自爆：32 格内最多 100 点伤害、摧毁周围约 6 格方块，且不掉落稀有物品。 */
    private void selfDestruct(ServerLevel level) {
        this.selfDestructed = true;
        BlockPos pos = this.blockPosition();
        MarsModule.LOGGER.info("Ares self-destructed at {}", pos);
        // 爆炸威力 6 = 文档的「摧毁周围 6 格方块」；实体的爆炸免疫仍然靠
        // DemonMothEntity#isInvulnerableTo（level.explode 无法按实体过滤）。
        level.explode(this, this.getX(), this.getY() + 1.0D, this.getZ(), SELF_DESTRUCT_POWER,
                Level.ExplosionInteraction.MOB);

        // 文档「32 格、100 伤害」：原版爆炸的伤害衰减极快、半径也只有威力×2，
        // 这里按距离线性衰减再结算一次（中心 100、边缘 0），保证远处的玩家也吃到自爆。
        AABB area = this.getBoundingBox().inflate(SELF_DESTRUCT_RADIUS);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            if (entity instanceof DemonMothEntity || entity instanceof AresEntity) {
                continue;
            }
            double distance = Math.sqrt(entity.distanceToSqr(this));
            float amount = (float) (SELF_DESTRUCT_MAX_DAMAGE
                    * Math.max(0.0D, 1.0D - distance / SELF_DESTRUCT_RADIUS));
            if (amount <= 0.0F) {
                continue;
            }
            entity.hurt(level.damageSources().explosion(this, this), amount);
            entity.setRemainingFireTicks(120);
        }

        int destroyed = KineticMachines.destroyNearby(level, pos, 16.0D);
        if (destroyed > 0) {
            MarsModule.LOGGER.info("Ares self-destruct destroyed {} kinetic blocks", destroyed);
        }
        // v1.13：不能再用 kill()！LivingEntity#kill() = hurt(genericKill, MAX_VALUE)，而本类覆写的
        // hurt() 在护盾期间直接 return false —— 自爆时核心温度通常 > 1500℃（护盾正开着），
        // 于是 kill() 被自己的护盾挡掉，Boss 会带着 selfDestructed=true 继续活着（掉落已免、沙尘暴也不收）。
        // 这里直接走死亡流程，保证自爆一定结束。
        if (!this.isDeadOrDying()) {
            this.setHealth(0.0F);
            this.die(level.damageSources().explosion(this, this));
        }
    }

    // ------------------------------------------------------------------ 战斗

    /** 岩浆拳：近战范围攻击并把玩家击飞。 */
    /**
     * 防击退：显式屏蔽一切外部推力。
     *
     * <p>属性 {@code Attributes.KNOCKBACK_RESISTANCE = 1.0} 只对走 {@code LivingEntity.knockback()}
     * 的那条路径有效；而 {@code Entity.push(...)}、爆炸的 {@code addDeltaMovement(...)} 等
     * <b>直接改速度</b>，完全绕过属性 —— 冷却枪原来就是用 {@code push()} 推它。
     * 所以这里连 {@code push} 一起忽略，Boss 才真正推不动。</p>
     */
    @Override
    public void push(double x, double y, double z) {
        // Boss 不吃任何外力位移（保留自身 AI 的位移）
    }

    @Override
    public void knockback(double strength, double x, double z) {
        // 同上：连击退事件也一并忽略
    }

    /**
     * 召唤物友军判定：阿瑞斯不把恶魔蛾当作可攻击目标（目标选择、{@code HurtByTargetGoal}
     * 与 {@code TargetGoal} 都走这一条判定）。
     */
    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof DemonMothEntity) && super.canAttack(target);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // 友军保护：即使被 AI / 外部逻辑强行指派，也绝不伤害恶魔蛾
        if (target instanceof DemonMothEntity) {
            return false;
        }
        boolean hurt = super.doHurtTarget(target);
        float bonus = this.isEnraged() ? 0.5F : 0.0F;
        this.startAction(ACTION_ATTACK, 14);

        for (LivingEntity entity : this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(3.0D), e -> e != this && e != target)) {
            // AoE 走的是 entity.hurt(...)，不经过 canAttack，必须在循环里显式过滤
            if (entity instanceof DemonMothEntity) {
                continue;
            }
            entity.hurt(this.damageSources().mobAttack(this), 6.0F);
            entity.setRemainingFireTicks(60);
            // 需求变更（v1.7）：普通攻击**不再击飞**。
            // 原来这里对被波及的实体调用了 knockUp(entity)，把 3 格内所有人一起抛起来，
            // 玩家被摸一下就失控落地，近战完全没法打 —— 现在只保留「伤害 + 点燃」。
        }
        // 同上：主目标也不再击飞（原来这里是 knockUp(living)）。
        // 注意：击飞只是从**普通攻击**里去掉；技能（slam / eruption / storm 等）的位移表现
        // 不经过本方法，未做任何改动。
        if (hurt && bonus > 0.0F && target instanceof LivingEntity living) {
            living.hurt(this.damageSources().mobAttack(this),
                    (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * bonus);
        }
        return hurt;
    }

    /**
     * 把目标向上抛起并沿背离 Boss 的方向推开。
     *
     * <p><b>当前没有调用点</b>：普通攻击的击飞已按需求取消（见 {@link #doHurtTarget}）。
     * 方法保留是为了后续技能复用（例如给 slam 加回击飞），删除的话下次要重新调参。</p>
     */
    @SuppressWarnings("unused")
    private void knockUp(LivingEntity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement()
                .add(0.0D, 1.1D, 0.0D)
                .add(this.position().subtract(entity.position()).normalize().scale(0.6D)));
        entity.hurtMarked = true;
    }

    /** 狂暴减伤 60%，脆化 / 脆弱期受到伤害 +200%；护盾期间完全无敌；每次实际结算的伤害 +20℃。 */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // v1.9 任务 2：护盾期间 Boss 无敌 —— 直接 return false。不扣血、不走受伤动画（红闪/受击音）、
        // 不产生击退，也不会把攻击者写进 lastHurtByMob（HurtByTargetGoal 因此不会因无效攻击而仇恨）。
        // 取舍：护盾期间**温度也不再因受击上升**（下面那段 +20 根本不会执行）。否则会互相锁死 ——
        // 「被打 → 升温 → 护盾 → 免疫 → 温度不掉 → 永远无敌」；现在破盾的唯一手段是便携式冷却器降温
        // （applyCoolant 由物品直接调用，不经过 hurt，所以护盾期间依然有效）。
        if (this.hasShield()) {
            return false;
        }
        float modified = amount;
        if (this.vulnerabilityTicks > 0 || this.isBrittle()) {
            modified *= 3.0F;
        } else if (this.isEnraged()) {
            modified *= 0.4F;
        }
        // v1.9 任务 1：每次「伤害判定」给核心温度 +20℃（仍受 MAX_CORE_TEMPERATURE 上限约束，
        // 由 setCoreTemperature 统一夹紧）。
        //
        // 为什么 +=20 放在**这一层、且只在 super.hurt 返回 true 之后**：
        //  ① 唯一的覆写点在子类 hurt()，父类 LivingEntity#hurt 不会回调本方法（它只调
        //     actuallyHurt / hurtHelmet / hurtCurrentlyUsedShield，都不经过 hurt），
        //     所以不存在父子重复调用叠成 40 的可能；
        //  ② super.hurt 返回 true 才代表这次判定**真的结算了**：无敌帧内的重复命中
        //     （invulnerableTime > 10 且伤害不高于 lastHurt）会在父类里 return false，
        //     被无敌 / 免疫 / 已死挡掉的也都返回 false —— 放在返回之后，一次命中只加一次，
        //     且不会给「没打中」的攻击白送温度；
        //  ③ 出伤流程里只有这一处入口，AresEntity 的其它代码（技能 AoE）都不 hurt 自己。
        boolean landed = super.hurt(source, modified);
        if (landed) {
            this.setCoreTemperature(this.coreTemperature + HEAT_PER_HIT);
        }
        return landed;
    }

    /**
     * v1.9：按用户要求削减受击反馈 —— 被击中时的受伤音从原版铁傀儡的满音量（1.0）降到 0.45。
     *
     * <p><b>v1.9 二次削减（本轮）：0.45 → 0.22</b>。阶段一 Boss 会被高频近战命中，0.45 仍然盖过
     * 武器/命中音；0.22 保留「打中了」的听觉反馈但不再刷屏。</p>
     *
     * <p>只改「被打中」这一条音效路径：死亡音 / 脚步音 / 攻击音都走各自的方法或显式音量，
     * 不受影响。护盾期间连这个方法都不会被调用（见 {@link #hurt(DamageSource, float)} 的早退）。</p>
     */
    @Override
    protected void playHurtSound(DamageSource source) {
        this.playSound(this.getHurtSound(source), 0.22F, this.getVoicePitch());
    }

    // ------------------------------------------------------------------ 血条 / 掉落

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        // /kill 走的是 Entity#kill() → remove(KILLED)，不经过 die()，所以这里补一道
        if (reason == Entity.RemovalReason.KILLED) {
            this.endBossSandstorm();
        }
        this.bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    // ------------------------------------------------------------------ 存档

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("CoreTemperature", this.coreTemperature);
        compound.putInt("AresPhase", this.phase);
        compound.putInt("SelfDestructTimer", this.selfDestructTimer);
        compound.putInt("VulnerabilityTicks", this.vulnerabilityTicks);
        compound.putBoolean("Hovering", this.hovering);
        compound.putBoolean("SelfDestructed", this.selfDestructed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("CoreTemperature")) {
            this.setCoreTemperature(compound.getInt("CoreTemperature"));
        }
        if (compound.contains("AresPhase")) {
            this.phase = compound.getInt("AresPhase");
        }
        if (compound.contains("SelfDestructTimer")) {
            this.selfDestructTimer = compound.getInt("SelfDestructTimer");
        }
        if (compound.contains("VulnerabilityTicks")) {
            this.vulnerabilityTicks = compound.getInt("VulnerabilityTicks");
        }
        if (compound.contains("Hovering")) {
            this.hovering = compound.getBoolean("Hovering");
            this.setNoGravity(this.hovering);
        }
        if (compound.contains("SelfDestructed")) {
            this.selfDestructed = compound.getBoolean("SelfDestructed");
        }
        // 同步给客户端动画的状态
        this.entityData.set(DATA_PHASE, this.phase);
        this.entityData.set(DATA_HOVERING, this.hovering);
        this.entityData.set(DATA_COUNTDOWN, Math.max(0, this.selfDestructTimer / 20));
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (this.selfDestructed) {
            // 文档：自爆 → Boss 不掉落稀有物品
            return;
        }
        // 文档掉落表：星核碎片 2–4（100%）、火星钢 4–8（100%）、星核核心 1（15%）、经验 500（xpReward）
        this.spawnAtLocation(new ItemStack(ModItems.STAR_CORE_SHARD.get(), 2 + this.random.nextInt(3)));
        this.spawnMartianSteel(4 + this.random.nextInt(5));
        if (this.random.nextFloat() < 0.15F) {
            this.spawnAtLocation(new ItemStack(ModItems.STAR_CORE_HEART.get()));
        }
        // 保留少量旧材料（文档未列）：翼膜是「防腐蚀涂层」的唯一原料，全删会让那条配方断源。
        this.spawnAtLocation(new ItemStack(ModItems.DEMON_MOTH_WING_MEMBRANE.get(), 2));
        this.spawnAtLocation(new ItemStack(ModItems.DARK_RED_SCALE_DUST.get(), 4 + this.random.nextInt(5)));
    }

    /**
     * 掉落火星钢（{@code northstar:martian_steel_ingot}）。
     *
     * <p>本模组不依赖 Northstar 的编译期 API，所以按注册名从物品注册表取；取不到就退化成星核碎片，
     * 不会掉出空气。</p>
     */
    private void spawnMartianSteel(int count) {
        Item ingot = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("northstar", "martian_steel_ingot"));
        if (ingot == Items.AIR) {
            MarsModule.LOGGER.warn("northstar:martian_steel_ingot not found; dropping star core shards instead");
            this.spawnAtLocation(new ItemStack(ModItems.STAR_CORE_SHARD.get(), count));
            return;
        }
        this.spawnAtLocation(new ItemStack(ingot, count));
    }

    @Override
    public boolean shouldDropExperience() {
        return !this.selfDestructed;
    }

    private void broadcast(ServerLevel level, String translationKey) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, this.getBoundingBox().inflate(64.0D))) {
            player.displayClientMessage(Component.translatable(translationKey), false);
        }
    }
}
