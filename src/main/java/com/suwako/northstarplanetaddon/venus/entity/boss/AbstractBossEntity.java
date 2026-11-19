package com.suwako.northstarplanetaddon.venus.entity.boss;

import com.suwako.northstarplanetaddon.venus.combat.AntiCheeseHandler;
import com.suwako.northstarplanetaddon.venus.combat.DamageKind;
import com.suwako.northstarplanetaddon.venus.combat.DamageTrace;
import com.suwako.northstarplanetaddon.venus.combat.WeakPoint;
import com.suwako.northstarplanetaddon.venus.config.PhosphorosConfig;
import com.suwako.northstarplanetaddon.venus.item.BallisticCalibratorItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Boss 基类：护甲板 / 本体分离（R1）、窗口期（R5）、弱点与过载（2.4 节）、
 * 每人 DPS 滑动窗口（R4）与反逃课免疫（2.6 节）。
 *
 * <p>三个 Boss 都继承它，只需要提供：</p>
 * <ol>
 *   <li>{@link #createAttributes()} 属性；</li>
 *   <li>构造器里 {@code super(type, level, 护甲板数量, 单板血量, 血条颜色)}；</li>
 *   <li>{@link #registerGoals()} 普通寻路目标 + {@link #tickBossAi()} 招式状态机；</li>
 *   <li>{@link #addWeakPoint} 暴露弱点；</li>
 *   <li>可选覆盖 {@link #phaseThresholds()}（涅斐勒是 60% / 25%）。</li>
 * </ol>
 */
public abstract class AbstractBossEntity extends Monster {

    // ------------------------------------------------------------------ 同步数据
    private static final EntityDataAccessor<Float> DATA_PLATE_HP =
            SynchedEntityData.defineId(AbstractBossEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_PHASE =
            SynchedEntityData.defineId(AbstractBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_WINDOW =
            SynchedEntityData.defineId(AbstractBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_OVERLOAD =
            SynchedEntityData.defineId(AbstractBossEntity.class, EntityDataSerializers.INT);

    /** 单次结算每人每类伤害的滑动窗口容量（128 足够覆盖 20 tick 内的极高射速）。 */
    private static final int DPS_WINDOW_CAPACITY = 128;

    // ------------------------------------------------------------------ 运行状态
    private final float[] plateHp;
    private final float plateMaxHpEach;
    private final List<WeakPoint> weakPoints = new ArrayList<>();
    private final Map<UUID, DpsWindow[]> dpsWindows = new HashMap<>();
    private final ServerBossEvent bossEvent;

    private long[] weakPointHitTicks = new long[0];
    private double overloadProgress;
    private int overloadTicks;
    private long lastOverloadTick = Long.MIN_VALUE;
    private int windowTicks;
    private int transitionTicks;
    private int bossBarTicks;
    private int scalingTicks;
    private int lastPhase = 1;
    private DamageTrace lastTrace = DamageTrace.none();

    protected AbstractBossEntity(EntityType<? extends AbstractBossEntity> type, Level level,
                                 int plateCount, float plateHpEach, BossEvent.BossBarColor barColor) {
        super(type, level);
        this.plateMaxHpEach = plateHpEach;
        this.plateHp = new float[Math.max(0, plateCount)];
        Arrays.fill(this.plateHp, plateHpEach);
        this.bossEvent = new ServerBossEvent(this.getDisplayName(), barColor, BossEvent.BossBarOverlay.PROGRESS);
        this.bossEvent.setDarkenScreen(true);
        this.setPersistenceRequired();
        this.setNoAi(false);
    }

    // ================================================================== 注册期
    /** 与设计草案 4.x / 5 章数值对应的属性表。 */
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createBossAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 520.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 20.0D);
    }

    // ================================================================== 同步 / 存档
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PLATE_HP, 0.0F);
        builder.define(DATA_PHASE, 1);
        builder.define(DATA_WINDOW, false);
        builder.define(DATA_OVERLOAD, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag plates = new ListTag();
        for (float hp : this.plateHp) {
            plates.add(FloatTag.valueOf(hp));
        }
        tag.put("PhosphorosPlates", plates);
        tag.putDouble("PhosphorosOverload", this.overloadProgress);
        tag.putInt("PhosphorosOverloadTicks", this.overloadTicks);
        tag.putInt("PhosphorosWindowTicks", this.windowTicks);
        tag.putLong("PhosphorosLastOverload", this.lastOverloadTick);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("PhosphorosPlates")) {
            ListTag plates = tag.getList("PhosphorosPlates", 5); // 5 = FLOAT
            for (int i = 0; i < this.plateHp.length && i < plates.size(); i++) {
                this.plateHp[i] = plates.getFloat(i);
            }
        }
        this.overloadProgress = tag.getDouble("PhosphorosOverload");
        this.overloadTicks = tag.getInt("PhosphorosOverloadTicks");
        this.windowTicks = tag.getInt("PhosphorosWindowTicks");
        long last = tag.getLong("PhosphorosLastOverload");
        this.lastOverloadTick = tag.contains("PhosphorosLastOverload") ? last : Long.MIN_VALUE;
    }

    // ================================================================== tick
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.transitionTicks > 0) {
            this.transitionTicks--;
        }
        if (this.overloadTicks > 0) {
            this.overloadTicks--;
            if (this.overloadTicks == 0) {
                this.overloadProgress = 0.0D;
            }
        }
        if (this.windowTicks > 0) {
            this.windowTicks--;
        }

        int phase = this.getPhase();
        if (phase != this.lastPhase) {
            int previous = this.lastPhase;
            this.lastPhase = phase;
            this.onPhaseChanged(previous, phase);
        }

        this.entityData.set(DATA_PLATE_HP, this.getTotalPlateHp());
        this.entityData.set(DATA_PHASE, phase);
        this.entityData.set(DATA_WINDOW, this.windowTicks > 0);
        this.entityData.set(DATA_OVERLOAD, this.overloadTicks);

        if (--this.bossBarTicks <= 0) {
            this.bossBarTicks = 20;
            this.updateBossBar();
        }
        if (--this.scalingTicks <= 0) {
            this.scalingTicks = 100;
            this.applyMultiplayerScaling();
            this.pruneDpsWindows();
        }
    }

    /** 每 tick 由子类实现招式状态机。 */
    protected void tickBossAi() {
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.transitionTicks > 0) {
            this.getNavigation().stop();
            return;
        }
        this.tickBossAi();
    }

    /** 阶段切换：默认进入 1.5s 无敌演出并开窗。 */
    protected void onPhaseChanged(int from, int to) {
        this.transitionTicks = 30;
        this.openWindow(to == 3 ? 200 : 100);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** 过载触发（第 2.4 节：护甲板受伤 +50%，强制开窗）。 */
    protected void onOverloadTriggered() {
        this.openWindow(PhosphorosConfig.OVERLOAD_WINDOW_TICKS.get());
    }

    // ================================================================== 管线接口
    /** 相位切换 / 死亡演出期间的伤害豁免。 */
    public boolean isDamageImmune() {
        return this.transitionTicks > 0 || this.isDeadOrDying();
    }

    /**
     * 免疫期仍然收到伤害事件时的钩子（默认空实现）。
     *
     * <p>阿佛洛狄忒的「结茧」阶段靠它实现：茧本身免疫，但玩家打茧会消耗茧的护壁，
     * 打碎后提前进入开窗。</p>
     */
    public void onImmuneHit(net.minecraft.world.damagesource.DamageSource source, float amount) {
    }

    public boolean hasLivingPlate() {
        return this.getTotalPlateHp() > 1.0E-4F;
    }

    public float getTotalPlateHp() {
        float total = 0.0F;
        for (float hp : this.plateHp) {
            total += hp;
        }
        return total;
    }

    public int getPlateCount() {
        return this.plateHp.length;
    }

    public float getPlateHp(int index) {
        return index >= 0 && index < this.plateHp.length ? this.plateHp[index] : 0.0F;
    }

    public float getPlateMaxHpEach() {
        return this.plateMaxHpEach;
    }

    /**
     * 扣护甲板血量。
     *
     * @return 实际被护甲板吸收的伤害
     */
    public float damagePlates(float amount) {
        float remaining = amount;
        for (int i = 0; i < this.plateHp.length && remaining > 0.0F; i++) {
            float applied = Math.min(this.plateHp[i], remaining);
            this.plateHp[i] -= applied;
            remaining -= applied;
        }
        return amount - remaining;
    }

    public boolean isOverloaded() {
        return this.overloadTicks > 0;
    }

    public int getOverloadTicks() {
        return this.overloadTicks;
    }

    public double getOverloadProgress() {
        return this.overloadProgress;
    }

    public boolean isWindowOpen() {
        return this.windowTicks > 0;
    }

    public int getWindowTicks() {
        return this.windowTicks;
    }

    public void openWindow(int ticks) {
        this.windowTicks = Math.max(this.windowTicks, ticks);
    }

    public int getTransitionTicks() {
        return this.transitionTicks;
    }

    // ================================================================== 阶段
    public int getPhase() {
        float ratio = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
        float[] thresholds = this.phaseThresholds();
        if (ratio > thresholds[0]) {
            return 1;
        }
        return ratio > thresholds[1] ? 2 : 3;
    }

    /** P1→P2、P2→P3 的血量比例阈值。 */
    protected float[] phaseThresholds() {
        return new float[]{0.66F, 0.33F};
    }

    // ================================================================== 弱点 / 过载
    protected WeakPoint addWeakPoint(String id, Vec3 offset, double radius, int exposeIn) {
        WeakPoint weakPoint = new WeakPoint(id, offset, radius, exposeIn);
        this.weakPoints.add(weakPoint);
        this.weakPointHitTicks = new long[this.weakPoints.size()];
        return weakPoint;
    }

    public List<WeakPoint> getWeakPoints() {
        return this.weakPoints;
    }

    public boolean isWeakPointExposed(int index) {
        if (index < 0 || index >= this.weakPoints.size()) {
            return false;
        }
        if (this.isOverloaded() || this.isWindowOpen()) {
            return true;
        }
        return this.weakPoints.get(index).exposedIn(this.getPhase());
    }

    /**
     * 弱点命中判定：命中位置落在弱点球内且该点位冷却已过，则累加过载值。
     *
     * @return 是否命中弱点
     */
    public boolean tryHitWeakPoint(Vec3 hitPosition, @javax.annotation.Nullable LivingEntity attacker, DamageKind kind) {
        if (this.weakPoints.isEmpty()) {
            return false;
        }
        long now = this.level().getGameTime();
        int cooldown = PhosphorosConfig.WEAK_POINT_COOLDOWN_TICKS.get();
        for (int i = 0; i < this.weakPoints.size(); i++) {
            WeakPoint weakPoint = this.weakPoints.get(i);
            if (!this.isWeakPointExposed(i)) {
                continue;
            }
            Vec3 world = this.position().add(weakPoint.offset());
            double radius = Math.max(weakPoint.radius(), (double) PhosphorosConfig.WEAK_POINT_RADIUS.get());
            if (world.distanceToSqr(hitPosition) > radius * radius) {
                continue;
            }
            if (now - this.weakPointHitTicks[i] < cooldown) {
                return false;
            }
            this.weakPointHitTicks[i] = now;
            double gain = switch (kind) {
                case BALLISTIC -> 1.0D;
                case MELEE -> 0.5D;
                default -> 0.7D;
            };
            if (kind == DamageKind.BALLISTIC && attacker instanceof Player player
                    && BallisticCalibratorItem.isCarriedBy(player)) {
                gain += BallisticCalibratorItem.EXTRA_OVERLOAD;
            }
            this.addOverloadPoints(gain);
            return true;
        }
        return false;
    }

    /** 机制性过载（压力锤震荡波）。 */
    public void addOverloadFromMechanic(double points) {
        this.addOverloadPoints(points);
    }

    protected void addOverloadPoints(double points) {
        long now = this.level().getGameTime();
        if (this.overloadTicks > 0) {
            return;
        }
        int cooldown = PhosphorosConfig.OVERLOAD_INTERNAL_COOLDOWN_TICKS.get();
        if (this.lastOverloadTick != Long.MIN_VALUE && now - this.lastOverloadTick < cooldown) {
            return;
        }
        this.overloadProgress += points;
        double cap = PhosphorosConfig.OVERLOAD_POINTS.get();
        if (this.overloadProgress >= cap) {
            this.overloadProgress = 0.0D;
            this.overloadTicks = PhosphorosConfig.OVERLOAD_DURATION_TICKS.get();
            this.lastOverloadTick = now;
            this.onOverloadTriggered();
        }
    }

    // ================================================================== R4 DPS 窗口
    /**
     * 消耗某名玩家在当前滑动窗口内的剩余额度。
     *
     * @return 允许通过的伤害（已记入窗口）
     */
    public float consumeDpsBudget(UUID playerId, DamageKind kind, float requested) {
        float cap = kind.perPlayerDpsCap();
        if (cap <= 0.0F) {
            return 0.0F;
        }
        // 第 3.2 节：弹道上限随阶段 13 → 15 → 17，近战保持 45
        if (kind != DamageKind.MELEE) {
            cap *= 1.0F + 0.15F * (this.getPhase() - 1);
        }
        long now = this.level().getGameTime();
        int window = PhosphorosConfig.DPS_WINDOW_TICKS.get();
        DpsWindow dpsWindow = this.dpsWindows
                .computeIfAbsent(playerId, id -> {
                    DpsWindow[] windows = new DpsWindow[DamageKind.values().length];
                    for (int i = 0; i < windows.length; i++) {
                        windows[i] = new DpsWindow();
                    }
                    return windows;
                })[kind.ordinal()];

        dpsWindow.prune(now - window);
        float used = dpsWindow.sum();
        float allowed = Math.max(0.0F, Math.min(requested, cap - used));
        if (allowed > 0.0F) {
            dpsWindow.add(now, allowed);
        }
        return allowed;
    }

    /** 调试用：当前窗口已用额度 / 上限。 */
    public float[] debugDps(UUID playerId, DamageKind kind) {
        DpsWindow[] windows = this.dpsWindows.get(playerId);
        if (windows == null) {
            return new float[]{0.0F, kind.perPlayerDpsCap()};
        }
        long now = this.level().getGameTime();
        DpsWindow window = windows[kind.ordinal()];
        window.prune(now - PhosphorosConfig.DPS_WINDOW_TICKS.get());
        return new float[]{window.sum(), kind.perPlayerDpsCap()};
    }

    private void pruneDpsWindows() {
        long cutoff = this.level().getGameTime() - PhosphorosConfig.DPS_WINDOW_TICKS.get();
        Iterator<Map.Entry<UUID, DpsWindow[]>> iterator = this.dpsWindows.entrySet().iterator();
        while (iterator.hasNext()) {
            DpsWindow[] windows = iterator.next().getValue();
            boolean empty = true;
            for (DpsWindow window : windows) {
                window.prune(cutoff);
                if (window.sum() > 0.0F) {
                    empty = false;
                }
            }
            if (empty) {
                iterator.remove();
            }
        }
    }

    /** 无对象分配的环形缓冲：只存 (tick, damage)。 */
    private static final class DpsWindow {
        private final long[] ticks = new long[DPS_WINDOW_CAPACITY];
        private final float[] amounts = new float[DPS_WINDOW_CAPACITY];
        private int head;
        private int size;

        void add(long tick, float amount) {
            int index = (this.head + this.size) % DPS_WINDOW_CAPACITY;
            if (this.size < DPS_WINDOW_CAPACITY) {
                this.size++;
            } else {
                this.head = (this.head + 1) % DPS_WINDOW_CAPACITY;
            }
            this.ticks[index] = tick;
            this.amounts[index] = amount;
        }

        void prune(long minimumTick) {
            while (this.size > 0 && this.ticks[this.head] <= minimumTick) {
                this.amounts[this.head] = 0.0F;
                this.head = (this.head + 1) % DPS_WINDOW_CAPACITY;
                this.size--;
            }
        }

        float sum() {
            float total = 0.0F;
            for (int i = 0; i < this.size; i++) {
                total += this.amounts[(this.head + i) % DPS_WINDOW_CAPACITY];
            }
            return total;
        }
    }

    // ================================================================== 血条 / 缩放
    private void updateBossBar() {
        if (this.level() instanceof ServerLevel serverLevel) {
            this.bossEvent.setProgress(Math.max(0.0F, this.getHealth() / this.getMaxHealth()));
            this.bossEvent.setName(this.getDisplayName());
            this.bossEvent.setVisible(this.isAlive());
            for (ServerPlayer player : serverLevel.getPlayers(p -> p.distanceToSqr(this) < 64.0D * 64.0D)) {
                this.bossEvent.addPlayer(player);
            }
            for (ServerPlayer player : List.copyOf(this.bossEvent.getPlayers())) {
                if (player.distanceToSqr(this) >= 64.0D * 64.0D || player.isRemoved()) {
                    this.bossEvent.removePlayer(player);
                }
            }
        }
    }

    /** 第 2.5 节：HP × (1 + 0.5 × (人数 - 1))，人数变化时按血量比例平滑缩放。 */
    private void applyMultiplayerScaling() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        // 基准血量 = 首次缩放前的值，避免反复乘算
        if (this.baseMaxHealth <= 0.0D) {
            this.baseMaxHealth = maxHealth.getBaseValue();
        }
        int players = Math.max(1, serverLevel.getPlayers(p -> p.isAlive() && !p.isSpectator()
                && p.distanceToSqr(this) < 64.0D * 64.0D).size());
        double scale = 1.0D + PhosphorosConfig.BOSS_HP_PER_EXTRA_PLAYER.get() * (players - 1);
        double target = this.baseMaxHealth * scale;
        if (Math.abs(maxHealth.getBaseValue() - target) < 0.5D) {
            return;
        }
        float ratio = this.getHealth() / this.getMaxHealth();
        maxHealth.setBaseValue(target);
        this.setHealth(Math.max(1.0F, this.getMaxHealth() * ratio));
    }

    private double baseMaxHealth = -1.0D;

    @Override
    public void die(DamageSource source) {
        this.bossEvent.removeAllPlayers();
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                    this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ(), 4, 1.5D, 1.0D, 1.5D, 0.0D);
        }
        super.die(source);
    }

    // ================================================================== 反逃课
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        if (this.transitionTicks > 0) {
            return true;
        }
        return super.isInvulnerableTo(source) || AntiCheeseHandler.isBlocked(source);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void knockback(double strength, double x, double z) {
        // 免疫击退
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean startRiding(Entity entity, boolean force) {
        return false;
    }

    // ================================================================== 调试
    public DamageTrace getLastTrace() {
        return this.lastTrace;
    }

    public void setLastTrace(DamageTrace trace) {
        this.lastTrace = trace;
    }

    /** 让 {@code /phosphoros debug boss} 输出可读状态。 */
    public String debugLines() {
        StringBuilder builder = new StringBuilder();
        builder.append("phase=").append(this.getPhase())
                .append(" hp=").append(String.format("%.1f/%.1f", this.getHealth(), this.getMaxHealth()))
                .append(" plates=").append(String.format("%.1f", this.getTotalPlateHp()))
                .append("/").append(String.format("%.1f", this.getPlateCount() * this.plateMaxHpEach))
                .append(" window=").append(this.windowTicks)
                .append(" overload=").append(String.format("%.1f%%", 100.0D * this.overloadProgress
                        / Math.max(1.0D, PhosphorosConfig.OVERLOAD_POINTS.get())))
                .append(" overloadTicks=").append(this.overloadTicks)
                .append(" transition=").append(this.transitionTicks);
        DamageTrace trace = this.lastTrace;
        builder.append(" last=").append(trace.kind()).append("/").append(trace.note())
                .append(" raw=").append(String.format("%.2f", trace.raw()))
                .append(" core=").append(String.format("%.2f", trace.core()));
        return builder.toString();
    }

}
