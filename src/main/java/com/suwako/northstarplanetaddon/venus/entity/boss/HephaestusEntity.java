package com.suwako.northstarplanetaddon.venus.entity.boss;

import com.suwako.northstarplanetaddon.venus.combat.WeakPoint;
import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
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


/**
 * 赫菲斯托斯巨像（设计草案 4.3 节）：地表层 · 阵地战。
 *
 * <table>
 *   <tr><th>项</th><th>值</th></tr>
 *   <tr><td>本体 HP</td><td>800</td></tr>
 *   <tr><td>护甲板</td><td>6 × 150</td></tr>
 *   <tr><td>阶段</td><td>100–66% 熔岩拳 / 66–33% 蒸汽 / 33–0% 炉心过载</td></tr>
 *   <tr><td>弱点</td><td>背部炉心（仅 P3 暴露）+ 双肩散热口（常驻）</td></tr>
 * </table>
 *
 * <h2>招式（4.3 节招式表）</h2>
 * <ul>
 *   <li>熔岩拳 16 / 前摇 0.9s / 冷却 6s / 前方 5×5</li>
 *   <li>熔渣投掷 8 + 火 4s / 前摇 1.2s / 冷却 10s / 落点 3 格</li>
 *   <li>蒸汽喷发 10 + 击退 / 前摇 2s / 冷却 5s / 4 个喷口 ——
 *       <b>喷口被实心方块压住 = 封堵成功，Boss 立刻开窗 4s</b>（设计草案 P2 机制）</li>
 *   <li>地震踏 14 / 前摇 1.4s / 冷却 18s / 半径 8（跳起可躲）</li>
 *   <li>过载爆发 20 / 前摇 3s / 冷却 40s / 半径 12（仅 P3）</li>
 * </ul>
 */
public class HephaestusEntity extends AbstractBossEntity implements GeoEntity {

    // ------------------------------------------------------------ 动画（GeckoLib）
    /** 无招式动画。 */
    public static final int ACTION_NONE = 0;
    public static final int ACTION_PUNCH = 1;
    public static final int ACTION_SLAG = 2;
    public static final int ACTION_STOMP = 3;
    public static final int ACTION_STEAM = 4;
    public static final int ACTION_BURST = 5;
    public static final int ACTION_BACKHAND = 6;
    public static final int ACTION_PLATE_BREAK = 7;

    /** 招式动画长度（tick，按 ACTION_* 下标）：与 {@code hephaestus.animation.json} 的 animation_length 对齐。 */
    private static final int[] ACTION_ANIM_TICKS = {0, 20, 20, 26, 32, 44, 52, 16};

    private static final EntityDataAccessor<Integer> DATA_ACTION =
            SynchedEntityData.defineId(HephaestusEntity.class, EntityDataSerializers.INT);

    /** 服务端直驱前进时置位；客户端据此播放 walk（不依赖 deltaMovement / limbSwing）。 */
    private static final EntityDataAccessor<Boolean> DATA_MOVING =
            SynchedEntityData.defineId(HephaestusEntity.class, EntityDataSerializers.BOOLEAN);

    /** 已打碎的护甲板位掩码（bit i = 第 i 块）。 */
    private static final EntityDataAccessor<Integer> DATA_PLATE_MASK =
            SynchedEntityData.defineId(HephaestusEntity.class, EntityDataSerializers.INT);

    /** 过载状态（过载爆发之后为 true，炉盖常开）。 */
    private static final EntityDataAccessor<Boolean> DATA_OVERLOADED =
            SynchedEntityData.defineId(HephaestusEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_IDLE_P2 = RawAnimation.begin().thenLoop("idle_p2");
    private static final RawAnimation ANIM_IDLE_P3 = RawAnimation.begin().thenLoop("idle_p3");
    /** 过载后的常驻待机：炉盖锁定在全开位，炉心外露脉动。 */
    private static final RawAnimation ANIM_OVERLOAD_IDLE =
            RawAnimation.begin().thenLoop("overload_idle");
    private static final RawAnimation ANIM_WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ANIM_PUNCH =
            RawAnimation.begin().then("lava_punch", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_BACKHAND =
            RawAnimation.begin().then("backhand", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_SLAG =
            RawAnimation.begin().then("slag_throw", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_STOMP =
            RawAnimation.begin().then("quake_stomp", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_STEAM =
            RawAnimation.begin().then("steam_vent", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_BURST =
            RawAnimation.begin().then("overload_burst", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_PLATE_BREAK =
            RawAnimation.begin().then("plate_break", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ANIM_DEATH =
            RawAnimation.begin().then("death", Animation.LoopType.HOLD_ON_LAST_FRAME);

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    /** 招式动画剩余 tick：归零后从 action 控制器退回 idle/walk。 */
    private int actionTicks;

    /** 近战左右手交替：false = 右手熔岩拳，true = 左手横扫。 */
    private boolean meleeLeft;

    /** 直驱标记：true 时 {@link #travel(Vec3)} 用满前进输入（不走寻路）。 */
    private boolean forwardDrive;

    /** 每 tick 计算好的逼近向量（格/tick）；ZERO 表示站定。 */
    private Vec3 driveVector = Vec3.ZERO;

    /** 直驱生效的 tick 数（测试/调试用，确认 travel 真的被调用到）。 */
    private int travelDriveTicks;

    /** 最近一次直驱的实际水平位移（0 = 被方块挡住或输入为 0）。 */
    private double lastMoveActual;

    /**
     * 直驱移动：巨像 11 格宽，{@code PathFinder} 按碰撞盒宽度求通行空间，
     * 在熔岩平台上几乎处处「无路」—— 旧实现 {@code getNavigation().moveTo()} 的结果是原地不动，
     * 于是既不播 walk 动画也不出拳，玩家看到的就是「Boss 没有动作」。
     *
     * <p>这里绕开 {@code LivingEntity.travel} 的输入管线：竖直方向仍交给原版（重力/落地/流体），
     * 水平位移按 {@link #driveVector} 直接 {@code move()}，碰撞照旧由 {@code Entity#move} 处理。</p>
     */
    @Override
    public void travel(Vec3 travelVector) {
        if (this.forwardDrive && !this.isDeadOrDying()) {
            this.travelDriveTicks++;
            super.travel(Vec3.ZERO);                       // 只保留重力与摩擦
            double x0 = this.getX();
            double z0 = this.getZ();
            this.move(MoverType.SELF, new Vec3(this.driveVector.x, 0.0D, this.driveVector.z));
            this.lastMoveActual = Math.hypot(this.getX() - x0, this.getZ() - z0);
            return;
        }
        super.travel(travelVector);
    }

    /** 测试用：直驱生效了多少 tick（0 说明 travel 没被调用到，或 forwardDrive 一直是 false）。 */
    public int debugTravelDriveTicks() {
        return this.travelDriveTicks;
    }

    /** 测试用：直驱位移 / 碰撞状态的一行摘要。 */
    public String debugMoveInfo() {
        var bb = this.getBoundingBox();
        var bp = this.blockPosition();
        var shapes = this.level().getBlockCollisions(this, bb.expandTowards(0.0D, 0.0D, 0.5D));
        int n = 0;
        String first = "-";
        for (var shape : shapes) {
            if (n++ == 0) {
                var sb = shape.bounds();
                first = String.format(java.util.Locale.ROOT, "%.2f,%.2f,%.2f", sb.minX, sb.minY, sb.minZ);
            }
        }
        return String.format(java.util.Locale.ROOT,
                "pos=%.2f,%.2f,%.2f drive=%.3f,%.3f moved=%.4f onGround=%s hColl=%s vColl=%s "
                        + "bbY=[%.2f..%.2f] shapes=%d firstShape=%s blkFeet=%s blkAhead=%s",
                this.getX(), this.getY(), this.getZ(), this.driveVector.x, this.driveVector.z,
                this.lastMoveActual, this.onGround(), this.horizontalCollision, this.verticalCollision,
                bb.minY, bb.maxY, n, first,
                this.level().getBlockState(bp).getBlock(),
                this.level().getBlockState(bp.offset(0, 0, 2)).getBlock());
    }

    // ---------------------------------------------------------------- 招式参数
    private static final int FIST_CD = 120;
    private static final int FIST_WINDUP = 18;
    private static final float FIST_DAMAGE = 16.0F;
    private static final double FIST_REACH = 6.0D;

    private static final int SLAG_CD = 200;
    private static final int SLAG_WINDUP = 24;
    private static final float SLAG_DAMAGE = 8.0F;
    private static final double SLAG_RADIUS = 3.0D;
    private static final int SLAG_FIRE_TICKS = 80;

    private static final int STOMP_CD = 360;
    private static final int STOMP_WINDUP = 28;
    private static final float STOMP_DAMAGE = 14.0F;
    private static final double STOMP_RADIUS = 8.0D;

    private static final int STEAM_CD = 100;
    private static final int STEAM_WINDUP = 40;
    private static final float STEAM_DAMAGE = 10.0F;
    private static final int STEAM_COLUMN_HEIGHT = 8;
    private static final int STEAM_SEAL_WINDOW = 80;

    private static final int BURST_CD = 800;
    private static final int BURST_WINDUP = 60;
    private static final float BURST_DAMAGE = 20.0F;
    private static final double BURST_RADIUS = 12.0D;

    /** 4 个蒸汽喷口相对 Boss 的水平偏移。 */
    private static final int[][] VENTS = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}};

    private int fistTicks = FIST_CD;
    private int slagTicks = SLAG_CD;
    private int stompTicks = STOMP_CD;
    private int steamTicks = STEAM_CD;
    private int burstTicks = BURST_CD;
    private int windupTicks;
    private int windupMove;
    private Vec3 slagTarget = Vec3.ZERO;
    private int heatAuraTicks;

    public HephaestusEntity(EntityType<? extends HephaestusEntity> type, Level level) {
        super(type, level, 6, 150.0F, BossEvent.BossBarColor.RED);
        // 双肩散热口常驻；背部炉心只在 P3 暴露。
        // 偏移按 2026-01 重制版模型实测（1 格 = 16 模型单位，脚底 y=0）：
        // 散热口在肩甲正面 (±44, 132, -25) → (±2.75, 8.25, -1.56) 格；
        // 炉心在背部 (+28) 的开口中部 (0, 128, +28) → (0, 8.0, +1.75) 格。
        this.addWeakPoint("vent_left", new Vec3(-2.75D, 8.25D, 0.0D), 1.6D, WeakPoint.ALWAYS);
        this.addWeakPoint("vent_right", new Vec3(2.75D, 8.25D, 0.0D), 1.6D, WeakPoint.ALWAYS);
        this.addWeakPoint("core", new Vec3(0.0D, 8.0D, 1.7D), 1.6D, WeakPoint.PHASE_3);
        this.setPersistenceRequired();
    }

    // ------------------------------------------------------------------ GeckoLib 接线

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, ACTION_NONE);
        // 直驱位移不会反映到 deltaMovement 上，客户端只能用这个同步位判断「该不该播走路」
        builder.define(DATA_MOVING, false);
        // 已打碎的护甲板位掩码（bit i = 第 i 块板），客户端据此隐藏对应 plate_i 骨
        builder.define(DATA_PLATE_MASK, 0);
        // 进入过载后炉盖常开，客户端切到 overload_idle
        builder.define(DATA_OVERLOADED, false);
    }

    /** 已打碎的护甲板掩码。 */
    public int getPlateMask() {
        return this.entityData.get(DATA_PLATE_MASK);
    }

    /** 过载状态：炉盖锁定全开。 */
    public boolean isOverloaded() {
        return this.entityData.get(DATA_OVERLOADED);
    }

    /** 每块大甲片在模型中的位置（模型单位，1 格 = 16 单位），破碎时在这里冒碎屑。 */
    private static final double[][] PLATE_DEBRIS = {
            {46.0D, 132.0D, 0.0D},    // plate_1 右肩甲
            {-46.0D, 132.0D, 0.0D},   // plate_2 左肩甲
            {0.0D, 118.0D, -14.0D},   // plate_3 胸甲
            {0.0D, 88.0D, 0.0D},      // plate_4 腹甲
            {0.0D, 118.0D, 16.0D},    // plate_5 背甲
            {0.0D, 140.0D, 0.0D},     // plate_6 头甲
    };

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 只用一个控制器：两控制器（main + action）在 4.9.x 里会按注册顺序叠在同一批骨骼上，
        // 实测会出现「招式动画只推动了 idle 没动的骨头」（只有拳/脚动一下）的情况。
        // 单控制器 + 优先级分支，任何时刻只有一段动画在驱动骨骼。
        controllers.add(new AnimationController<>(this, "main", 4, this::mainController));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }

    public int getAction() {
        return this.entityData.get(DATA_ACTION);
    }

    private void setAction(int action) {
        this.entityData.set(DATA_ACTION, action);
    }

    /** 单控制器状态机：死亡 > 招式 > 走路 > 分阶段待机。 */
    private PlayState mainController(AnimationState<HephaestusEntity> state) {
        if (this.isDeadOrDying()) {
            state.getController().setAnimationSpeed(1.0D).setAnimation(ANIM_DEATH);
            return PlayState.CONTINUE;
        }
        RawAnimation action = switch (this.getAction()) {
            case ACTION_PUNCH -> ANIM_PUNCH;
            case ACTION_BACKHAND -> ANIM_BACKHAND;
            case ACTION_SLAG -> ANIM_SLAG;
            case ACTION_STOMP -> ANIM_STOMP;
            case ACTION_STEAM -> ANIM_STEAM;
            case ACTION_BURST -> ANIM_BURST;
            case ACTION_PLATE_BREAK -> ANIM_PLATE_BREAK;
            default -> null;
        };
        if (action != null) {
            state.getController().setAnimationSpeed(1.0D).setAnimation(action);
            return PlayState.CONTINUE;
        }
        // 走路判定：优先用同步位（直驱位移不进 deltaMovement，limbSwing 在整合包里不可靠）
        boolean walking = this.entityData.get(DATA_MOVING)
                || (state.isMoving() && Math.abs(state.getLimbSwingAmount()) > 0.03F);
        if (walking) {
            // 巨像走得慢：步频随移动量小幅变化，封顶 1.15 倍，别让它迈成小碎步
            double speed = Math.max(0.75D, 0.9D + Math.min(0.4D, Math.abs(state.getLimbSwingAmount()) * 1.2D));
            state.getController().setAnimationSpeed(speed).setAnimation(ANIM_WALK);
            return PlayState.CONTINUE;
        }
        // 待机：过载之后炉盖锁定全开（overload_idle）；否则按阶段分三档
        RawAnimation idle;
        if (this.isOverloaded()) {
            idle = ANIM_OVERLOAD_IDLE;
        } else {
            idle = switch (this.getPhase()) {
                case 3 -> ANIM_IDLE_P3;
                case 2 -> ANIM_IDLE_P2;
                default -> ANIM_IDLE;
            };
        }
        state.getController().setAnimationSpeed(1.0D).setAnimation(idle);
        return PlayState.CONTINUE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes()
                .add(Attributes.MAX_HEALTH, 800.0D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 30.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        // 移动由 tickBossAi 直接驱动：巨像只会缓慢逼近，不做绕圈寻路
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 48.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void tickBossAi() {
        // 演示 / 测试兜底：Monster 的原版目标选择在创造模式、旁观者过滤等情形下可能根本不锁定玩家，
        // 巨像就会站着不动 —— 表现成「Boss 完全没有动作」。
        this.acquireDemoTarget();
        // 招式动画播完（前摇 + 收招）才回到 idle / walk，否则挥到一半就被打断
        if (this.actionTicks > 0 && this.windupTicks == 0) {
            if (--this.actionTicks == 0) {
                this.setAction(ACTION_NONE);
            }
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        this.getLookControl().setLookAt(target, 20.0F, 20.0F);

        // -------------------------------------------------- 前摇中
        if (this.windupTicks > 0) {
            this.windupTicks--;
            this.getNavigation().stop();
            this.forwardDrive = false;
            this.entityData.set(DATA_MOVING, false);
            this.driveVector = Vec3.ZERO;
            this.zza = 0.0F;
            this.setDeltaMovement(0.0D, this.getDeltaMovement().y, 0.0D);
            if (this.windupTicks == 0) {
                this.executeWindup(target);
            }
            return;
        }

        // -------------------------------------------------- 缓慢逼近
        // 11 格宽的巨像走不了寻路（PathFinder 按碰撞盒宽度求通行空间，熔岩平台上处处“无路”），
        // 所以按设计草案的做法直驱逼近：自己算方向 + 朝向，水平位移交给 travel。
        double distance = this.distanceTo(target);
        this.getNavigation().stop();
        if (distance > 3.5D) {
            Vec3 toward = new Vec3(target.getX() - this.getX(), 0.0D, target.getZ() - this.getZ());
            if (toward.lengthSqr() > 1.0E-4D) {
                double speed = Math.max(0.05D, this.getAttributeValue(Attributes.MOVEMENT_SPEED));
                this.driveVector = toward.normalize().scale(speed);
                this.forwardDrive = true;
                // 面朝行进方向（GeckoLib 模型按 yRot 渲染，原地转向也要同步）
                float yaw = (float) (Mth.atan2(toward.z, toward.x) * (180.0D / Math.PI)) - 90.0F;
                this.setYRot(yaw);
                this.yBodyRot = yaw;
                this.setYHeadRot(yaw);
            }
            this.zza = 0.92F;
            this.entityData.set(DATA_MOVING, true);
        } else {
            this.driveVector = Vec3.ZERO;
            this.forwardDrive = false;
            this.entityData.set(DATA_MOVING, false);
            this.zza = 0.0F;
            this.setSpeed(0.0F);
        }
        this.tickPhaseEffects(target);

        // -------------------------------------------------- 招式选择
        if (--this.fistTicks <= 0 && distance <= FIST_REACH) {
            this.fistTicks = FIST_CD;
            this.startWindup(FIST_WINDUP, 0);
            return;
        }
        if (--this.slagTicks <= 0) {
            this.slagTicks = SLAG_CD;
            this.slagTarget = target.position();
            this.startWindup(SLAG_WINDUP, 1);
            return;
        }
        if (--this.stompTicks <= 0 && distance <= STOMP_RADIUS) {
            this.stompTicks = STOMP_CD;
            this.startWindup(STOMP_WINDUP, 2);
            return;
        }
        if (this.getPhase() >= 2 && --this.steamTicks <= 0) {
            this.steamTicks = STEAM_CD;
            this.startWindup(STEAM_WINDUP, 3);
            return;
        }
        if (this.getPhase() >= 3 && --this.burstTicks <= 0) {
            this.burstTicks = BURST_CD;
            this.startWindup(BURST_WINDUP, 4);
        }
    }

    /**
     * 没目标时主动锁定 48 格内最近的玩家（旁观者除外）。
     *
     * <p>为什么需要：原版 {@code NearestAttackableTargetGoal} 在创造模式/旁观者过滤、
     * 以及一些模组环境（Connector 等）下可能一直锁定不到玩家；测试 Boss 招式的人几乎都在创造模式，
     * 结果就是巨像原地不动 —— 既不走路也不出招，看起来「完全没有动作」。</p>
     */
    private void acquireDemoTarget() {
        if (this.getTarget() != null || this.level().isClientSide) {
            return;
        }
        Player nearest = this.level()
                .getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(48.0D),
                        player -> player.isAlive() && !player.isSpectator())
                .stream()
                .min(java.util.Comparator.comparingDouble(player -> this.distanceToSqr(player)))
                .orElse(null);
        if (nearest != null) {
            this.setTarget(nearest);
        }
    }

    private void startWindup(int ticks, int move) {
        this.windupTicks = ticks;
        this.windupMove = move;
        this.zza = 0.0F;
        // 招式动画：前摇期间播到击打帧，命中后继续播收招
        int action = move == 0
                ? (this.meleeLeft ? ACTION_BACKHAND : ACTION_PUNCH)
                : move + 1;
        this.setAction(action);
        this.actionTicks = ticks + ACTION_ANIM_TICKS[action];
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE,
                    2.5F, this.windupMove == 3 ? 1.4F : 0.6F);
            serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 3.0D, this.getZ(),
                    14, 1.2D, 1.0D, 1.2D, 0.0D);
        }
        // 蒸汽前摇：4 个喷口冒白烟（玩家必须在这 2s 内把喷口压住）
        if (this.windupMove == 3) {
            this.markVents(ParticleTypes.CLOUD, 8);
        }
    }

    private void executeWindup(LivingEntity target) {
        switch (this.windupMove) {
            case 0 -> this.lavaFist();
            case 1 -> this.slagThrow();
            case 2 -> this.earthquakeStomp();
            case 3 -> this.steamEruption();
            case 4 -> this.overloadBurst();
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ 手臂判定
    /** 单位：模型格（1 格 = 16 模型单位）。拳心在模型中 x=±66、y=40、z=-4。 */
    private static final double FIST_X_UNITS = 66.0D;
    private static final double FIST_Y_UNITS = 40.0D;
    private static final double FIST_Z_UNITS = -4.0D;
    /** 单拳判定半径（格）：拳头本身约 2.6 格宽，取 1.4 半宽 + 容错。 */
    private static final double FIST_HALF_EXTENT = 1.55D;

    /** 机体水平朝向前方（忽略俯仰角），长度 1。 */
    public Vec3 horizontalLook() {
        Vec3 look = this.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        return flat.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : flat.normalize();
    }

    /**
     * 把模型局部坐标（模型单位，脚底原点、-Z 为正面、+X 为模型自身右侧）换算成世界坐标。
     * 巨像的手臂判定完全靠它，动画里拳头打到哪，判定就落在哪。
     */
    public Vec3 localToWorld(double xUnits, double yUnits, double zUnits) {
        Vec3 forward = this.horizontalLook();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);   // 模型自身右侧
        return this.position()
                .add(right.scale(xUnits / 16.0D))
                .add(forward.scale(-zUnits / 16.0D))
                .add(0.0D, yUnits / 16.0D, 0.0D);
    }

    /** 左/右拳心的世界坐标。 */
    public Vec3 fistCenter(boolean leftArm) {
        return this.localToWorld(leftArm ? -FIST_X_UNITS : FIST_X_UNITS, FIST_Y_UNITS, FIST_Z_UNITS);
    }

    /** 左/右拳的世界碰撞盒（这就是玩家能打中、巨像能打出的“手臂判定箱”）。 */
    public AABB fistBounds(boolean leftArm) {
        return new AABB(this.fistCenter(leftArm), this.fistCenter(leftArm)).inflate(FIST_HALF_EXTENT);
    }

    private void damageVictims(AABB area, float damage, double knockback) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 forward = this.horizontalLook();
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this && entity.isAlive() && !(entity instanceof AbstractBossEntity))) {
            victim.hurt(serverLevel.damageSources().mobAttack(this), damage);
            victim.push(forward.x * knockback, 0.4D, forward.z * knockback);
        }
    }

    /** 熔岩拳（右手） / 横扫（左手）：判定体是出拳那只拳头，不是躯干盒子。 */
    private void lavaFist() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean leftArm = this.getAction() == ACTION_BACKHAND;
        Vec3 forward = this.horizontalLook();
        Vec3 fist = this.fistCenter(leftArm);
        AABB area = this.fistBounds(leftArm).expandTowards(forward.scale(2.2D)).inflate(0.35D);
        this.damageVictims(area, FIST_DAMAGE, 0.7D);
        serverLevel.sendParticles(ParticleTypes.EXPLOSION, fist.x, fist.y, fist.z, 3, 0.8D, 0.6D, 0.8D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.LAVA, fist.x, fist.y, fist.z, 12, 0.7D, 0.5D, 0.7D, 0.0D);
        serverLevel.playSound(null, BlockPos.containing(fist), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 2.0F, 0.7F);
    }

    /** 熔渣投掷：落点 3 格内 8 点伤害 + 点燃 4s。 */
    private void slagThrow() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 center = this.slagTarget;
        AABB area = new AABB(center.add(-SLAG_RADIUS, -2.0D, -SLAG_RADIUS), center.add(SLAG_RADIUS, 2.0D, SLAG_RADIUS));
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this && entity.isAlive() && !(entity instanceof AbstractBossEntity))) {
            victim.hurt(serverLevel.damageSources().mobAttack(this), SLAG_DAMAGE);
            victim.igniteForSeconds(SLAG_FIRE_TICKS / 20.0F);
        }
        serverLevel.sendParticles(ParticleTypes.LAVA, center.x, center.y + 0.4D, center.z,
                25, 1.5D, 0.4D, 1.5D, 0.0D);
        serverLevel.playSound(null, BlockPos.containing(center), SoundEvents.LAVA_POP, SoundSource.HOSTILE,
                1.6F, 0.8F);
    }

    /** 地震踏：半径 8、14 点伤害；离地（跳跃中）的目标免疫。 */
    private void earthquakeStomp() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB area = this.getBoundingBox().inflate(STOMP_RADIUS);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this && entity.isAlive() && !(entity instanceof AbstractBossEntity))) {
            if (!victim.onGround()) {
                continue; // 跳出地面可躲
            }
            victim.hurt(serverLevel.damageSources().mobAttack(this), STOMP_DAMAGE);
            victim.push(0.0D, 0.6D, 0.0D);
        }
        serverLevel.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.2D, this.getZ(),
                60, STOMP_RADIUS, 0.3D, STOMP_RADIUS, 0.05D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 3.0F, 0.4F);
    }

    /**
     * 蒸汽喷发（P2 核心机制）：4 个喷口。
     *
     * <p>喷口位置若被实心方块压住（例如玩家在 2s 前摇里放下的冰 / 云岩），
     * 该喷口视为<b>封堵成功</b>：Boss 立刻开窗 4s，且不造成伤害。
     * 全部 4 个都被封堵时额外再开一次窗。</p>
     */
    private void steamEruption() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int sealed = 0;
        for (int[] vent : VENTS) {
            BlockPos ventPos = BlockPos.containing(this.getX() + vent[0], this.getY(), this.getZ() + vent[1]);
            BlockPos above = ventPos.above();
            if (!serverLevel.getBlockState(above).isAir()
                    && !serverLevel.getBlockState(above).getFluidState().is(Fluids.FLOWING_WATER)) {
                sealed++;
                continue;
            }
            AABB column = new AABB(ventPos.getX(), ventPos.getY(), ventPos.getZ(),
                    ventPos.getX() + 1.0D, ventPos.getY() + STEAM_COLUMN_HEIGHT, ventPos.getZ() + 1.0D);
            for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, column,
                    entity -> entity != this && entity.isAlive() && !(entity instanceof AbstractBossEntity))) {
                victim.hurt(serverLevel.damageSources().mobAttack(this), STEAM_DAMAGE);
                victim.push(0.0D, 0.8D, 0.0D);
            }
            serverLevel.sendParticles(ParticleTypes.CLOUD, ventPos.getX() + 0.5D, ventPos.getY() + 1.0D,
                    ventPos.getZ() + 0.5D, 20, 0.3D, 3.0D, 0.3D, 0.05D);
            serverLevel.playSound(null, ventPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.2F, 1.2F);
        }
        if (sealed > 0) {
            this.openWindow(STEAM_SEAL_WINDOW);
            serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 4.0D, this.getZ(),
                    20, 1.5D, 1.0D, 1.5D, 0.1D);
            serverLevel.playSound(null, this.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE,
                    1.0F, 0.6F);
        }
    }

    /** 过载爆发（P3）：半径 12、20 点伤害。 */
    private void overloadBurst() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // 过载爆发之后炉盖锁定全开：客户端切到 overload_idle 常驻姿态
        this.entityData.set(DATA_OVERLOADED, true);
        AABB area = this.getBoundingBox().inflate(BURST_RADIUS);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != this && entity.isAlive() && !(entity instanceof AbstractBossEntity))) {
            victim.hurt(serverLevel.damageSources().mobAttack(this), BURST_DAMAGE);
            victim.push(0.0D, 1.0D, 0.0D);
        }
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY() + 4.0D,
                this.getZ(), 6, BURST_RADIUS * 0.5D, 2.0D, BURST_RADIUS * 0.5D, 0.0D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 4.0F, 0.5F);
    }

    /** P3 热浪光环：全场 +80°C 的玩家侧表现（缓慢灼烧，无火焰抗性时生效）。 */
    private void tickPhaseEffects(LivingEntity target) {
        if (this.getPhase() < 3 || this.level().isClientSide) {
            return;
        }
        if (--this.heatAuraTicks > 0) {
            return;
        }
        this.heatAuraTicks = 20;
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (Player player : serverLevel.getPlayers(p -> p.isAlive() && !p.isSpectator()
                && p.distanceToSqr(this) < 16.0D * 16.0D)) {
            if (!player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
                player.hurt(serverLevel.damageSources().onFire(), 1.0F);
                player.igniteForSeconds(2.0F);
            }
        }
        serverLevel.sendParticles(ParticleTypes.SMALL_FLAME, this.getX(), this.getY() + 2.0D, this.getZ(),
                20, 6.0D, 2.0D, 6.0D, 0.01D);
    }

    private void markVents(net.minecraft.core.particles.SimpleParticleType particle, int count) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (int[] vent : VENTS) {
            serverLevel.sendParticles(particle, this.getX() + vent[0], this.getY() + 0.5D, this.getZ() + vent[1],
                    count, 0.2D, 0.8D, 0.2D, 0.02D);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, source, hitByPlayer);
        // 第 4.3 节掉落表：炉心 → 巨像核心碎片；熔火齿轮暂用前文明合金替代（该物品未注册）
        this.spawnAtLocation(new ItemStack(ModItems.CORE_SHARD.get(), 2 + this.random.nextInt(2)));
        this.spawnAtLocation(new ItemStack(ModItems.ACID_PROOF_ALLOY_PLATE.get(), 3 + this.random.nextInt(3)));
        this.spawnAtLocation(new ItemStack(ModItems.PRE_CIVILIZATION_ALLOY.get(), 1 + this.random.nextInt(2)));
        if (this.random.nextFloat() < 0.25F) {
            this.spawnAtLocation(new ItemStack(ModItems.CORE_SHARD.get(), 1));
        }
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }

    /** 巨像不会被击退、不会被船/矿车带走（基类已免疫击退与载具）。 */
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.getY() < this.level().getMinBuildHeight()) {
            this.setPos(this.getX(), 200.0D, this.getZ());
        }
    }

    /**
     * 护甲板被打碎时插播「甲片崩裂」动作（设计草案 1.3 节：6 块大甲片要能被打掉）。
     *
     * <p>管线（{@code BossDamagePipeline} 第 6 步）每次伤害都会调用这里；只要有一块板
     * 从 &gt;0 掉到 0，就把 {@code plate_break} 塞进 action 控制器。</p>
     */
    @Override
    public float damagePlates(float amount) {
        int plates = this.getPlateCount();
        float[] before = new float[plates];
        for (int i = 0; i < plates; i++) {
            before[i] = this.getPlateHp(i);
        }
        float absorbed = super.damagePlates(amount);
        for (int i = 0; i < plates; i++) {
            if (before[i] > 0.0F && this.getPlateHp(i) <= 1.0E-4F) {
                this.entityData.set(DATA_PLATE_MASK, this.getPlateMask() | (1 << i));
                this.spawnPlateDebris(i);
                this.setAction(ACTION_PLATE_BREAK);
                this.actionTicks = ACTION_ANIM_TICKS[ACTION_PLATE_BREAK];
                break;
            }
        }
        return absorbed;
    }

    /** 甲片碎裂：在这块板的世界位置冒熔渣与黑烟，并响一声金属断裂。 */
    private void spawnPlateDebris(int plate) {
        if (!(this.level() instanceof ServerLevel serverLevel) || plate < 0 || plate >= PLATE_DEBRIS.length) {
            return;
        }
        double[] local = PLATE_DEBRIS[plate];
        Vec3 world = this.localToWorld(local[0], local[1], local[2]);
        serverLevel.sendParticles(ParticleTypes.LAVA, world.x, world.y, world.z, 12, 0.6D, 0.6D, 0.6D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, world.x, world.y, world.z, 14, 0.5D, 0.5D, 0.5D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.CRIT, world.x, world.y, world.z, 18, 0.7D, 0.7D, 0.7D, 0.3D);
        serverLevel.playSound(null, this.blockPosition(), SoundEvents.ANVIL_BREAK, SoundSource.HOSTILE, 2.0F, 0.55F);
    }

    /**
     * 测试 / 调试用：跳过冷却与距离判定，直接把某个招式推进前摇。
     *
     * @param move 0 熔岩拳（左右手交替） / 1 熔渣投掷 / 2 地震踏 / 3 蒸汽喷发 / 4 过载爆发
     */
    public void debugTriggerMove(int move) {
        this.startWindup(2, Math.max(0, Math.min(4, move)));
    }

    /** 测试用：跑一次招式状态机 + 直驱移动（等同于 {@code customServerAiStep} 里那一跳）。 */
    public void debugTickAi() {
        this.tickBossAi();
    }

    /** 测试用：当前前进输入（0 = 站定，&gt;0 = 正在逼近）。 */
    public float debugForwardInput() {
        return this.zza;
    }

    /** 让「P3 炉心暴露」的读条状态可用于调试输出。 */
    @Override
    public String debugLines() {
        return super.debugLines() + " hephaestusPhase=" + this.getPhase()
                + " windup=" + this.windupTicks + "/" + this.windupMove
                + " steam=" + this.steamTicks;
    }
}
