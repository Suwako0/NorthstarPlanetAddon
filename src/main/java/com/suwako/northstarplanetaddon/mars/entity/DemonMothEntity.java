package com.suwako.northstarplanetaddon.mars.entity;

import com.suwako.northstarplanetaddon.mars.registry.ModEffects;
import com.suwako.northstarplanetaddon.mars.registry.ModItems;
import com.suwako.northstarplanetaddon.mars.world.KineticMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.Animation;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

/**
 * 火星恶魔蛾。
 *
 * <p>服务端逻辑：继承原版幻翼（复用其飞行 AI 与碰撞箱缩放），叠加鳞粉效果与「优先扑向
 * 运转中的机械方块」的取敌行为。**不再腐蚀机械结构**（鳞粉只作用于生物）。</p>
 *
 * <p>客户端表现：由 GeckoLib 驱动，模型 {@code geo/entity/demon_moth.geo.json}
 * （翼展约 4.9 格），动画 {@code animations/entity/demon_moth.animation.json}：
 * {@code idle}（停落）、{@code fly}（拍翅，速度随移动幅度）、{@code attack}（一次性俯冲撕咬）。</p>
 *
 * <ul>
 *     <li>生成：火山高地群系，夜晚；</li>
 *     <li>掉落：恶魔蛾翼膜 1-2、暗红鳞粉 0-2；</li>
 *     <li>对抗：穿 3 件以上火星钢护甲获得「耐腐蚀」，免疫鳞粉腐蚀。</li>
 * </ul>
 */
public class DemonMothEntity extends Phantom implements GeoEntity {

    /** 鳞粉喷洒间隔（tick）。 */
    private static final int SPRAY_INTERVAL = 40;

    /** 机械搜索半径。 */
    private static final double MACHINE_SEARCH_RADIUS = 12.0D;

    private static final byte ACTION_NONE = 0;
    private static final byte ACTION_ATTACK = 1;
    private static final int ATTACK_ANIMATION_TICKS = 12;

    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ANIM_FLY = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation ANIM_ATTACK =
            RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE);

    private static final EntityDataAccessor<Byte> DATA_ACTION =
            SynchedEntityData.defineId(DemonMothEntity.class, EntityDataSerializers.BYTE);

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    /** 仅服务端使用的动作倒计时。 */
    private int actionTicks;

    public DemonMothEntity(EntityType<? extends Phantom> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    // ------------------------------------------------------------------ 同步数据 / 动画

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, ACTION_NONE);
    }

    public byte getAction() {
        return this.entityData.get(DATA_ACTION);
    }

    private void startAction(byte action, int ticks) {
        this.entityData.set(DATA_ACTION, action);
        this.actionTicks = ticks;
    }

    @Override
    public void registerControllers(software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, this::mainController));
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

    private PlayState mainController(AnimationState<DemonMothEntity> state) {
        if (this.onGround()) {
            state.getController().setAnimationSpeed(1.0D).setAnimation(ANIM_IDLE);
        } else {
            double speed = 1.0D + Math.min(1.5D, Math.abs(state.getLimbSwingAmount()) * 4.0D);
            state.getController().setAnimationSpeed(speed).setAnimation(ANIM_FLY);
        }
        return PlayState.CONTINUE;
    }

    private PlayState actionController(AnimationState<DemonMothEntity> state) {
        if (this.getAction() == ACTION_ATTACK) {
            state.getController().setAnimation(ANIM_ATTACK);
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    // ------------------------------------------------------------------ 行为

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        // 大型飞行生物：幻翼体型 3（更大的碰撞箱）
        this.setPhantomSize(3);
        return data;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (this.actionTicks > 0 && --this.actionTicks == 0) {
            this.entityData.set(DATA_ACTION, ACTION_NONE);
        }
        if (this.tickCount % SPRAY_INTERVAL == 0) {
            this.sprayScales(serverLevel);
        }
    }

    /** 分泌鳞粉：只作用于生物（机械腐蚀已按需求移除）。 */
    private void sprayScales(ServerLevel level) {
        BlockPos pos = this.blockPosition();

        boolean sprayed = false;
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(6.0D))) {
            if (player.isCreative() || player.hasEffect(ModEffects.CORROSION_RESISTANCE)) {
                continue;
            }
            player.addEffect(new MobEffectInstance(ModEffects.CORROSION, 120, 0), this);
            sprayed = true;
        }
        if (sprayed) {
            level.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.HOSTILE, 0.7F, 0.6F);
        }

        // 仍然优先扑向正在运转的机械方块（原需求保留；若不需要，删掉这一行即可）
        this.huntMachines(level, pos);
    }

    private void huntMachines(ServerLevel level, BlockPos origin) {
        if (this.getTarget() != null) {
            return;
        }
        BlockPos machine = KineticMachines.findNearestKinetic(level, origin, MACHINE_SEARCH_RADIUS);
        if (machine == null) {
            return;
        }
        this.getMoveControl().setWantedPosition(machine.getX() + 0.5D, machine.getY() + 2.0D,
                machine.getZ() + 0.5D, 1.2D);
    }

    /**
     * 白天不燃烧 —— 这是「自动掉血」的根因。
     *
     * <p>{@code Phantom.tick()} 里有
     * {@code if (this.isAlive() && this.isSunBurnTick()) this.igniteForSeconds(8.0F);}，
     * 而 {@link Mob#isSunBurnTick()} 的默认实现是「白天 + 光照够 + 没淋雨」就返回 true。
     * 火星有自己的昼夜循环，所以幻翼在火星白天会被点燃并持续受火焰伤害。这里直接返回 false。</p>
     */
    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    /** 免疫火焰/岩浆本身（即使被别的来源点燃也不掉血）。 */
    @Override
    public boolean fireImmune() {
        return true;
    }

    /** 免掉一切环境伤害：白天黑夜都不掉血。<b>玩家与生物的攻击照旧有效</b>（仍然可以杀死）。 */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_DROWNING)
                || source.is(DamageTypeTags.IS_FREEZING)
                // 为了让 Boss（阿瑞斯）自毁时的 level.explode(...) 不伤害恶魔蛾 —— Explosion 是
                // 逐个对范围内实体造成伤害、无法按实体类型过滤，所以只能在这里整体豁免爆炸伤害。
                // 副作用：对 TNT、苦力怕等一切爆炸伤害也一并免疫。
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypes.CACTUS)
                || source.is(DamageTypes.SWEET_BERRY_BUSH)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.FALL)
                || super.isInvulnerableTo(source);
    }

    /** 不与 Boss 互相仇恨：阿瑞斯视为友军（目标选择 / HurtByTargetGoal 都走这条判定）。 */
    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof AresEntity) && super.canAttack(target);
    }

    // ------------------------------------------------------------------ v1.10：Boss 召唤物不掉落

    /** 由阿瑞斯召唤出来的个体：掉落与经验一律为无。 */
    private boolean summoned;

    public void markSummoned() {
        this.summoned = true;
    }

    public boolean isSummoned() {
        return this.summoned;
    }

    @Override
    public boolean shouldDropExperience() {
        return !this.summoned && super.shouldDropExperience();
    }

    @Override
    protected void dropFromLootTable(DamageSource source, boolean hitByPlayer) {
        if (this.summoned) {
            return;   // v1.10：召唤物不掉落任何战利品
        }
        super.dropFromLootTable(source, hitByPlayer);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // 友军保护：即使被 AI / 外部逻辑强行指派，也绝不伤害阿瑞斯
        if (target instanceof AresEntity) {
            return false;
        }
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            if (!this.level().isClientSide) {
                this.startAction(ACTION_ATTACK, ATTACK_ANIMATION_TICKS);
            }
            if (target instanceof LivingEntity living
                    && !living.hasEffect(ModEffects.CORROSION_RESISTANCE)) {
                living.addEffect(new MobEffectInstance(ModEffects.CORROSION, 100, 0), this);
            }
        }
        return hurt;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        if (this.summoned) {
            return;   // v1.10：Boss 的召唤物掉落为无
        }
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.DEMON_MOTH_WING_MEMBRANE.get(), 1 + this.random.nextInt(2)));
        // 暗红鳞粉 0-2
        int dust = this.random.nextInt(3);
        if (dust > 0) {
            this.spawnAtLocation(new ItemStack(ModItems.DARK_RED_SCALE_DUST.get(), dust));
        }
    }

    @Override
    protected int getBaseExperienceReward() {
        return this.summoned ? 0 : Mth.nextInt(this.random, 5, 10);   // v1.10：召唤物无经验
    }
}
