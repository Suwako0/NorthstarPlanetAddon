package com.suwako.northstarplanetaddon.moon.entity;

import com.suwako.northstarplanetaddon.moon.registry.NMAEntityTypes;
import com.suwako.northstarplanetaddon.moon.registry.NMAItems;
import com.suwako.northstarplanetaddon.moon.registry.NMASounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 梦眠月鹿 — a standalone Moonforest boss. It never naturally spawns; summon it deliberately.
 * <p>
 * The fight is entirely entity/particle driven: no regeneration, shields, arena construction,
 * summoned pillars, safe zones or terrain-changing explosions are used.
 */
public class MoonDeerEntity extends Animal {

    private static final int PHASE_INVULNERABILITY_TICKS = 20;
    private static final int STUN_TICKS = 60;
    private static final int WEAKNESS_TICKS = 200;
    /** Boss combat acquisition range; Moon Dust uses the same exact 64-block radius. */
    private static final double TARGET_RANGE = 64.0D;
    private static final double MOON_DUST_RANGE = 64.0D;
    private static final int MOON_DUST_WARNING_TICKS = 4;
    private static final float MOON_DUST_DAMAGE = 6.0F;
    private static final double MOON_DUST_PULL_HORIZONTAL = 1.25D;
    private static final double MOON_DUST_PULL_VERTICAL = 0.20D;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            Component.translatable("entity.northstar_planet_addon.lunar_deer"),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.PROGRESS);
    private final EnumMap<Skill, Integer> cooldowns = new EnumMap<>(Skill.class);

    /**
     * Hit proxies that make the whole silhouette clickable instead of just one box around the
     * torso. The numbers are read straight off the model, in blocks measured from the hooves with
     * the deer facing -z: width x height, then lateral / forward / height offsets. The main box
     * already covers the legs and the torso, so the parts only fill in what sticks out of it.
     */
    /** Empty until the constructor runs, so a part lookup during super() can never NPE. */
    private MoonDeerPart[] parts = new MoonDeerPart[0];

    /**
     * Body language the client model plays. Skills are server-side logic, so the current one has to be
     * synced explicitly for the deer to visibly wind up, cast and recover.
     */
    private static final EntityDataAccessor<Byte> COMBAT_ANIMATION = SynchedEntityData.defineId(
            MoonDeerEntity.class, EntityDataSerializers.BYTE);
    private final List<Vec3> rainTargets = new ArrayList<>();
    private final Set<UUID> dashHitTargets = new HashSet<>();
    /** Players that received a visible Moon Dust warning and may be pulled four ticks later. */
    private final Set<UUID> moonDustTargets = new HashSet<>();

    private Phase phase = Phase.NEW_MOON;
    private Skill activeSkill = Skill.NONE;
    private Skill lastSkill = Skill.NONE;
    private int skillTicks;
    private int nextSkillTicks = 45;
    private int phaseInvulnerabilityTicks;
    private int stunnedTicks;
    private int weaknessTicks;
    private int meleeCooldown;
    private boolean meteorUsed;
    private Vec3 dashDirection = Vec3.ZERO;
    @Nullable
    private UUID lockedTarget;
    @Nullable
    private UUID lastSkillTarget;

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(COMBAT_ANIMATION, (byte) NMACombatAnimation.IDLE.ordinal());
    }

    /** The animation the client model should be playing right now. */
    public NMACombatAnimation getCombatAnimation() {
        return NMACombatAnimation.byId(this.entityData.get(COMBAT_ANIMATION));
    }

    private void setCombatAnimation(NMACombatAnimation animation) {
        this.entityData.set(COMBAT_ANIMATION, (byte) animation.ordinal());
    }

    /** Maps a combat skill onto the body language that sells it. */
    private static NMACombatAnimation animationFor(Skill skill) {
        return switch (skill) {
            case DASH -> NMACombatAnimation.DASH;
            case RING -> NMACombatAnimation.RING;
            case BARRAGE -> NMACombatAnimation.BARRAGE;
            case BEAM -> NMACombatAnimation.BEAM;
            case MOON_RAIN -> NMACombatAnimation.MOON_RAIN;
            case METEOR -> NMACombatAnimation.METEOR;
            case SHADOW -> NMACombatAnimation.SHADOW;
            case MOON_DUST -> NMACombatAnimation.MOON_DUST;
            case NONE -> NMACombatAnimation.IDLE;
        };
    }

    public MoonDeerEntity(EntityType<? extends MoonDeerEntity> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 2000;
        this.setPersistenceRequired();
        for (Skill skill : Skill.values()) {
            this.cooldowns.put(skill, 0);
        }
        this.bossEvent.setDarkenScreen(false);
        this.bossEvent.setPlayBossMusic(false);
        this.bossEvent.setCreateWorldFog(false);

        this.parts = new MoonDeerPart[]{
                new MoonDeerPart(this, "chest", 1.00F, 1.30F, 0.0D, 0.80D, 1.00D),
                new MoonDeerPart(this, "neck", 0.95F, 1.10F, 0.0D, 0.95D, 1.80D),
                new MoonDeerPart(this, "head", 1.00F, 1.20F, 0.0D, 1.40D, 2.25D),
                new MoonDeerPart(this, "antler_left", 1.00F, 1.10F, -0.50D, 1.44D, 2.60D),
                new MoonDeerPart(this, "antler_right", 1.00F, 1.10F, 0.50D, 1.44D, 2.60D),
                new MoonDeerPart(this, "haunch", 1.00F, 1.30F, 0.0D, -0.55D, 1.00D),
                new MoonDeerPart(this, "tail", 0.60F, 1.10F, 0.0D, -1.05D, 1.15D)
        };
        // Reserve the ids right after our own so a part can never collide with another entity.
        this.setId(ENTITY_COUNTER.getAndAdd(this.parts.length + 1) + 1);
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (this.parts != null) {
            for (int i = 0; i < this.parts.length; i++) {
                this.parts[i].setId(id + 1 + i);
            }
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public PartEntity<?>[] getParts() {
        return this.parts;
    }

    /**
     * The pick box is inflated a little on every side. The neck and head swing well over half a block
     * during the idle sweep and the combat wind-ups, so a hit box traced exactly on the rest pose
     * would let the head slide out from under the crosshair.
     */
    @Override
    public float getPickRadius() {
        return 0.5F;
    }

    /** Keeps every hit proxy glued to the animated model, on both sides. */
    private void tickParts() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double forwardX = -Mth.sin(yaw);
        double forwardZ = Mth.cos(yaw);
        double rightX = -Mth.cos(yaw);
        double rightZ = -Mth.sin(yaw);
        for (MoonDeerPart part : this.parts) {
            double forward = part.getForwardOffset();
            double lateral = part.getLateralOffset();
            part.setPos(this.getX() + forwardX * forward + rightX * lateral,
                    this.getY() + part.getHeightOffset(),
                    this.getZ() + forwardZ * forward + rightZ * lateral);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.SAFE_FALL_DISTANCE, 10.0D)
                .add(Attributes.STEP_HEIGHT, 1.1D)
                .add(Attributes.FOLLOW_RANGE, TARGET_RANGE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public @Nullable MoonDeerEntity getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return null;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.phaseInvulnerabilityTicks > 0) {
            return false;
        }
        if (this.stunnedTicks > 0) {
            amount *= 1.5F;
        }
        if (this.weaknessTicks > 0) {
            amount *= 2.0F;
        }

        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide()) {
            this.updatePhase();
        }
        return hurt;
    }

    @Override
    public void tick() {
        super.tick();
        this.tickParts();
        if (this.level().isClientSide()) {
            return;
        }

        if (!this.isAlive()) {
            this.cancelActiveSkill();
            return;
        }

        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.updateBossBarName();
        this.tickCooldowns();

        if (this.phaseInvulnerabilityTicks > 0) {
            this.phaseInvulnerabilityTicks--;
        }
        if (this.stunnedTicks > 0) {
            this.stunnedTicks--;
            this.getNavigation().stop();
            this.emitStunParticles();
            return;
        }
        if (this.weaknessTicks > 0) {
            this.weaknessTicks--;
        }

        Player target = this.findCombatTarget();
        this.setTarget(target);
        if (target == null) {
            this.cancelActiveSkill();
            return;
        }

        if (this.activeSkill != Skill.NONE) {
            this.tickSkill(target);
            return;
        }

        if (this.meleeCooldown > 0) {
            this.meleeCooldown--;
        }
        double distance = this.distanceTo(target);
        if (distance <= 3.5D && this.meleeCooldown == 0) {
            this.doHurtTarget(target);
            this.meleeCooldown = 20;
        } else {
            this.getNavigation().moveTo(target, 0.9D);
        }

        if (--this.nextSkillTicks <= 0) {
            this.startNextSkill(target, distance);
        }
    }

    private void tickCooldowns() {
        for (Skill skill : Skill.values()) {
            int cooldown = this.cooldowns.getOrDefault(skill, 0);
            if (cooldown > 0) {
                this.cooldowns.put(skill, cooldown - 1);
            }
        }
    }

    private void updatePhase() {
        Phase next = Phase.forHealth(this.getHealth() / this.getMaxHealth());
        if (next != this.phase) {
            this.phase = next;
            this.phaseInvulnerabilityTicks = PHASE_INVULNERABILITY_TICKS;
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 1));
            this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
            this.cancelActiveSkill();
            this.getNavigation().stop();
            this.playSound(SoundEvents.ENDERMAN_STARE, 1.5F, 0.65F + this.phase.ordinal() * 0.12F);
            this.emitPhaseParticles();
        }
    }

    private void updateBossBarName() {
        this.bossEvent.setName(Component.translatable("entity.northstar_planet_addon.lunar_deer")
                .append(Component.literal(" - "))
                .append(Component.translatable(this.phase.translationKey)));
    }

    private Player findCombatTarget() {
        List<Player> players = this.findEligibleCombatPlayers(TARGET_RANGE);
        if (players.isEmpty()) {
            return null;
        }
        players.sort((left, right) -> Double.compare(this.distanceToSqr(left), this.distanceToSqr(right)));
        if (this.lastSkillTarget != null && players.size() > 1 && players.get(0).getUUID().equals(this.lastSkillTarget)) {
            return players.get(1);
        }
        return players.get(0);
    }

    /** Uniform player eligibility for every target, AoE and pull: exact spherical range, no observers. */
    private List<Player> findEligibleCombatPlayers(double range) {
        double rangeSqr = range * range;
        return this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(range),
                player -> isEligibleCombatPlayer(player) && this.distanceToSqr(player) <= rangeSqr);
    }

    private static boolean isEligibleCombatPlayer(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    private void startNextSkill(Player target, double distance) {
        Skill selected = this.chooseSkill(distance);
        if (selected == Skill.NONE) {
            this.nextSkillTicks = 20;
            return;
        }

        this.activeSkill = selected;
        this.lastSkill = selected;
        this.setCombatAnimation(animationFor(selected));
        this.skillTicks = 0;
        this.lockedTarget = target.getUUID();
        this.lastSkillTarget = target.getUUID();
        this.cooldowns.put(selected, selected.cooldown);
        this.getNavigation().stop();
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0F, 0.75F + this.random.nextFloat() * 0.15F);

        if (selected == Skill.DASH) {
            this.dashDirection = target.position().subtract(this.position()).normalize();
            this.dashHitTargets.clear();
        } else if (selected == Skill.MOON_RAIN) {
            this.prepareMoonRain(target);
        } else if (selected == Skill.MOON_DUST) {
            this.prepareMoonDustPull();
        } else if (selected == Skill.METEOR) {
            this.meteorUsed = true;
        }
    }

    private Skill chooseSkill(double distance) {
        if (this.phase == Phase.ECLIPSE && this.getHealth() / this.getMaxHealth() < 0.10F && !this.meteorUsed) {
            return Skill.METEOR;
        }

        List<Skill> candidates = new ArrayList<>();
        for (Skill skill : Skill.values()) {
            if (skill == Skill.NONE || skill == this.activeSkill || skill == this.lastSkill
                    || this.cooldowns.getOrDefault(skill, 0) > 0 || !skill.availableIn(this.phase)) {
                continue;
            }
            if (skill == Skill.DASH && distance <= 6.0D) {
                continue;
            }
            candidates.add(skill);
        }
        if (candidates.isEmpty()) {
            return Skill.NONE;
        }

        List<Skill> preferred = new ArrayList<>();
        for (Skill skill : candidates) {
            if (distance < 4.0D && skill == Skill.RING) {
                preferred.add(skill);
            } else if (distance >= 4.0D && distance <= 10.0D && (skill == Skill.DASH || skill == Skill.SHADOW)) {
                preferred.add(skill);
            } else if (distance > 10.0D && (skill == Skill.BARRAGE || skill == Skill.BEAM
                    || skill == Skill.MOON_RAIN || skill == Skill.MOON_DUST)) {
                preferred.add(skill);
            }
        }
        List<Skill> pool = preferred.isEmpty() ? candidates : preferred;
        return pool.get(this.random.nextInt(pool.size()));
    }

    private void tickSkill(Player fallbackTarget) {
        this.skillTicks++;
        Player target = this.findLockedTarget(fallbackTarget);
        switch (this.activeSkill) {
            case DASH -> tickDash(target);
            case BARRAGE -> tickBarrage(target);
            case RING -> tickRing();
            case BEAM -> tickBeam(target);
            case MOON_RAIN -> tickMoonRain();
            case SHADOW -> tickShadow(target);
            case MOON_DUST -> tickMoonDustPull();
            case METEOR -> tickMeteor();
            default -> finishSkill();
        }
    }

    private Player findLockedTarget(Player fallback) {
        if (this.lockedTarget != null && this.level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(this.lockedTarget);
            if (entity instanceof Player player && isEligibleCombatPlayer(player)) {
                return player;
            }
        }
        return fallback;
    }

    private void tickDash(Player target) {
        if (this.skillTicks <= 20) {
            this.getLookControl().setLookAt(target, 50.0F, 30.0F);
            this.emitChargeParticles();
            return;
        }
        if (this.skillTicks <= 36) {
            this.move(MoverType.SELF, this.dashDirection.scale(1.0D));
            this.hitDashedPlayers();
            if (this.horizontalCollision) {
                this.finishSkill();
                this.stunnedTicks = STUN_TICKS;
                this.playSound(SoundEvents.ANVIL_LAND, 1.0F, 0.85F);
                return;
            }
            return;
        }
        finishSkill();
    }

    private void hitDashedPlayers() {
        for (Player player : this.findEligibleCombatPlayers(4.0D)) {
            if (this.dashHitTargets.add(player.getUUID())) {
                player.hurt(this.damageSources().mobAttack(this), 10.0F);
                Vec3 push = player.position().subtract(this.position()).normalize();
                player.push(push.x * 1.8D, 0.45D, push.z * 1.8D);
            }
        }
    }

    private void tickBarrage(Player target) {
        if (this.skillTicks == 16) {
            if (this.phase == Phase.FULL_MOON || this.phase == Phase.ECLIPSE) {
                for (int index = 0; index < 12; index++) {
                    double angle = Math.PI * 2.0D * index / 12.0D;
                    fireMoonlightBolt(Math.cos(angle), 0.0D, Math.sin(angle));
                }
            } else {
                Vec3 direction = target.getEyePosition().subtract(this.getEyePosition()).normalize();
                for (int index = -2; index <= 2; index++) {
                    double angle = Math.toRadians(index * 12.0D);
                    fireMoonlightBolt(direction.x * Math.cos(angle) - direction.z * Math.sin(angle),
                            direction.y, direction.x * Math.sin(angle) + direction.z * Math.cos(angle));
                }
            }
            this.playSound(SoundEvents.WITHER_SHOOT, 1.0F, 1.25F);
        }
        if (this.skillTicks >= 28) {
            finishSkill();
        }
    }

    private void fireMoonlightBolt(double x, double y, double z) {
        MoonlightBoltEntity bolt = new MoonlightBoltEntity(this.level(), this, this.getX(), this.getEyeY() - 0.15D, this.getZ(), 4.0F);
        bolt.shoot(x, y, z, 1.1F, 0.0F);
        this.level().addFreshEntity(bolt);
    }

    private void tickRing() {
        if (this.skillTicks == 10) {
            damageRing(4.0D);
        }
        if (this.skillTicks == 28) {
            damageRing(8.0D);
        }
        emitRingParticles(this.skillTicks < 28 ? 4.0D : 8.0D);
        if (this.skillTicks >= 40) {
            finishSkill();
        }
    }

    private void damageRing(double radius) {
        for (Player player : this.findEligibleCombatPlayers(radius + 1.5D)) {
            double distance = Math.sqrt(this.distanceToSqr(player));
            if (Math.abs(distance - radius) <= 1.4D) {
                player.hurt(this.damageSources().magic(), 6.0F);
                Vec3 push = player.position().subtract(this.position()).normalize();
                player.push(push.x * 1.1D, 0.32D, push.z * 1.1D);
            }
        }
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.1F, 0.9F);
    }

    private void tickBeam(Player target) {
        if (this.skillTicks <= 40) {
            emitBeamWarning(target);
            return;
        }
        int beamTick = this.skillTicks - 40;
        double sweep = Math.toRadians(-35.0D + 70.0D * beamTick / 60.0D);
        Vec3 base = target.getEyePosition().subtract(this.getEyePosition()).normalize();
        Vec3 direction = new Vec3(base.x * Math.cos(sweep) - base.z * Math.sin(sweep), base.y,
                base.x * Math.sin(sweep) + base.z * Math.cos(sweep)).normalize();
        Vec3 start = this.getEyePosition();
        Vec3 desiredEnd = start.add(direction.scale(28.0D));
        BlockHitResult hit = this.level().clip(new ClipContext(start, desiredEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS ? desiredEnd : hit.getLocation();
        emitBeamParticles(start, end);
        if (beamTick % 10 == 0) {
            damageAlongBeam(start, end);
        }
        if (beamTick >= 60) {
            finishSkill();
        }
    }

    private void damageAlongBeam(Vec3 start, Vec3 end) {
        for (Player player : this.findEligibleCombatPlayers(30.0D)) {
            if (distanceToSegmentSqr(player.getEyePosition(), start, end) <= 1.0D) {
                player.hurt(this.damageSources().magic(), 3.0F);
            }
        }
    }

    private void tickMoonRain() {
        if (this.skillTicks <= 30) {
            for (Vec3 target : this.rainTargets) {
                emitGroundWarning(target, 1.3D);
            }
            return;
        }
        if (this.skillTicks == 31) {
            for (Vec3 point : this.rainTargets) {
                moonRainStrike(point);
            }
        }
        if (this.skillTicks >= 42) {
            finishSkill();
        }
    }

    private void prepareMoonRain(Player target) {
        this.rainTargets.clear();
        for (int index = 0; index < 7; index++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double radius = 1.5D + this.random.nextDouble() * 6.0D;
            Vec3 point = target.position().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            this.rainTargets.add(new Vec3(point.x, this.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (int) Math.floor(point.x), (int) Math.floor(point.z)), point.z));
        }
    }

    private void moonRainStrike(Vec3 point) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, point.x, point.y + 5.0D, point.z, 30, 0.25D, 2.5D, 0.25D, 0.05D);
        }
        for (Player player : this.level().getEntitiesOfClass(Player.class,
                new net.minecraft.world.phys.AABB(point.x - 1.5D, point.y - 1.0D, point.z - 1.5D, point.x + 1.5D, point.y + 6.0D, point.z + 1.5D),
                MoonDeerEntity::isEligibleCombatPlayer)) {
            player.hurt(this.damageSources().magic(), 7.0F);
        }
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9F, 1.4F);
    }

    /** Marks every eligible player in a true 64-block radius, then resolves the pull after 0.2 seconds. */
    private void prepareMoonDustPull() {
        this.moonDustTargets.clear();
        for (Player player : this.findEligibleCombatPlayers(MOON_DUST_RANGE)) {
            this.moonDustTargets.add(player.getUUID());
            emitMoonDustWarning(player);
        }
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.2F, 0.55F);
    }

    private void tickMoonDustPull() {
        this.getNavigation().stop();
        if (this.skillTicks < MOON_DUST_WARNING_TICKS) {
            // Repaint the warning on the player as they move, while retaining the initial UUID snapshot.
            for (UUID id : this.moonDustTargets) {
                if (this.level() instanceof ServerLevel serverLevel && serverLevel.getEntity(id) instanceof Player player
                        && isEligibleCombatPlayer(player)) {
                    emitMoonDustWarning(player);
                }
            }
            return;
        }
        if (this.skillTicks == MOON_DUST_WARNING_TICKS) {
            resolveMoonDustPull();
        }
        if (this.skillTicks >= 12) {
            finishSkill();
        }
    }

    private void resolveMoonDustPull() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (UUID id : this.moonDustTargets) {
            if (!(serverLevel.getEntity(id) instanceof Player player)
                    || !isEligibleCombatPlayer(player)
                    || this.distanceToSqr(player) > MOON_DUST_RANGE * MOON_DUST_RANGE) {
                continue;
            }

            player.hurt(this.damageSources().magic(), MOON_DUST_DAMAGE);
            Vec3 horizontalPull = new Vec3(this.getX() - player.getX(), 0.0D, this.getZ() - player.getZ());
            if (horizontalPull.lengthSqr() > 1.0E-6D) {
                Vec3 pull = horizontalPull.normalize().scale(MOON_DUST_PULL_HORIZONTAL);
                player.push(pull.x, MOON_DUST_PULL_VERTICAL, pull.z);
            }
            serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getBoundingBox().minY + 0.2D,
                    player.getZ(), 16, 0.35D, 0.15D, 0.35D, 0.025D);
        }
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.3F, 0.65F);
    }

    private void emitMoonDustWarning(Player player) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double x = player.getX();
        double y = player.getBoundingBox().minY + 0.05D;
        double z = player.getZ();
        for (int index = 0; index < 14; index++) {
            double angle = Math.PI * 2.0D * index / 14.0D;
            serverLevel.sendParticles(ParticleTypes.GLOW, x + Math.cos(angle) * 0.85D, y,
                    z + Math.sin(angle) * 0.85D, 1, 0.0D, 0.01D, 0.0D, 0.0D);
        }
        serverLevel.sendParticles(ParticleTypes.ENCHANT, x, y + 0.08D, z, 6, 0.28D, 0.02D, 0.28D, 0.01D);
    }

    private void tickShadow(Player target) {
        if (this.skillTicks <= 16) {
            this.setInvisible(true);
            emitGroundWarning(target.position(), 1.2D);
            return;
        }
        if (this.skillTicks == 17) {
            this.setInvisible(false);
            Vec3 behind = target.position().subtract(target.getLookAngle().normalize().scale(2.4D));
            this.teleportTo(behind.x, behind.y, behind.z);
            this.getLookControl().setLookAt(target, 80.0F, 30.0F);
            target.hurt(this.damageSources().mobAttack(this), 10.0F);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.8F);
        }
        if (this.skillTicks >= 30) {
            finishSkill();
        }
    }

    private void tickMeteor() {
        if (this.skillTicks <= 60) {
            this.setDeltaMovement(0.0D, 0.18D, 0.0D);
            emitGroundWarning(this.position(), 20.0D);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 8,
                        0.5D, 0.5D, 0.5D, 0.05D);
            }
            return;
        }
        if (this.skillTicks == 61) {
            for (Player player : this.findEligibleCombatPlayers(20.0D)) {
                player.hurt(this.damageSources().magic(), 18.0F);
                Vec3 push = player.position().subtract(this.position()).normalize();
                player.push(push.x * 1.8D, 1.0D, push.z * 1.8D);
            }
            this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.7F);
            this.weaknessTicks = WEAKNESS_TICKS;
            this.setDeltaMovement(Vec3.ZERO);
        }
        if (this.skillTicks >= 75) {
            finishSkill();
        }
    }

    /** Normal skill conclusion: clears transient state and schedules the next 2-4 second decision. */
    private void finishSkill() {
        this.clearTransientSkillState();
        this.nextSkillTicks = 40 + this.random.nextInt(41);
    }

    /** Cancellation path used for lost targets, death and interrupted casts; it never schedules a new cast. */
    private void cancelActiveSkill() {
        this.clearTransientSkillState();
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
    }

    private void clearTransientSkillState() {
        this.setInvisible(false);
        this.activeSkill = Skill.NONE;
        this.setCombatAnimation(NMACombatAnimation.IDLE);
        this.skillTicks = 0;
        this.lockedTarget = null;
        this.rainTargets.clear();
        this.moonDustTargets.clear();
        this.dashHitTargets.clear();
    }

    private void emitChargeParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getEyeY(), this.getZ(), 8,
                    0.7D, 0.7D, 0.7D, 0.02D);
        }
    }

    private void emitStunParticles() {
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getEyeY() + 0.6D, this.getZ(), 6,
                    0.45D, 0.2D, 0.45D, 0.0D);
        }
    }

    private void emitPhaseParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getEyeY(), this.getZ(), 40,
                    1.2D, 1.4D, 1.2D, 0.05D);
        }
    }

    private void emitRingParticles(double radius) {
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 2 == 0) {
            for (int index = 0; index < 24; index++) {
                double angle = Math.PI * 2.0D * index / 24.0D;
                serverLevel.sendParticles(ParticleTypes.GLOW, this.getX() + Math.cos(angle) * radius,
                        this.getY() + 0.15D, this.getZ() + Math.sin(angle) * radius,
                        1, 0.0D, 0.02D, 0.0D, 0.0D);
            }
        }
    }

    private void emitBeamWarning(Player target) {
        emitGroundWarning(target.position(), 1.5D);
        if (this.level() instanceof ServerLevel serverLevel && this.skillTicks % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getEyeY(), this.getZ(), 5,
                    0.45D, 0.45D, 0.45D, 0.01D);
        }
    }

    private void emitBeamParticles(Vec3 start, Vec3 end) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // One point per block is visually continuous while avoiding dozens of individual particle packets per tick.
        int points = Math.max(1, (int) Math.ceil(start.distanceTo(end)));
        for (int index = 0; index <= points; index++) {
            Vec3 point = start.lerp(end, index / (double) points);
            serverLevel.sendParticles(ParticleTypes.END_ROD, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private void emitGroundWarning(Vec3 point, double radius) {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.tickCount % 3 != 0) {
            return;
        }
        for (int index = 0; index < 20; index++) {
            double angle = Math.PI * 2.0D * index / 20.0D;
            serverLevel.sendParticles(ParticleTypes.GLOW, point.x + Math.cos(angle) * radius, point.y + 0.08D,
                    point.z + Math.sin(angle) * radius, 1, 0.0D, 0.01D, 0.0D, 0.0D);
        }
    }

    private static double distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-6D) {
            return point.distanceToSqr(start);
        }
        double progress = Math.max(0.0D, Math.min(1.0D, point.subtract(start).dot(segment) / lengthSqr));
        return point.distanceToSqr(start.add(segment.scale(progress)));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
        // Sent directly to this player in the MUSIC category so the OGG is treated as encounter music
        // rather than an attenuated positional effect at the Boss's feet.
        player.playNotifySound(NMASounds.MOON_DUEL.get(), SoundSource.MUSIC, 1.0F, 1.0F);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.stopBattleMusic(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void die(DamageSource source) {
        this.cancelActiveSkill();
        super.die(source);
        for (ServerPlayer player : this.bossEvent.getPlayers()) {
            this.stopBattleMusic(player);
        }
        this.bossEvent.setVisible(false);
        this.bossEvent.removeAllPlayers();
    }

    /** Stops only this Boss's streamed MUSIC event, leaving Moonforest ambience and vanilla music untouched. */
    private void stopBattleMusic(ServerPlayer player) {
        player.connection.send(new ClientboundStopSoundPacket(NMASounds.MOON_DUEL.get().getLocation(), SoundSource.MUSIC));
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return switch (this.random.nextInt(3)) {
            case 0 -> NMASounds.LUNAR_DEER_HIT_1.get();
            case 1 -> NMASounds.LUNAR_DEER_HIT_2.get();
            default -> NMASounds.LUNAR_DEER_HIT_3.get();
        };
    }

    @Override
    protected SoundEvent getDeathSound() {
        return NMASounds.LUNAR_DEER_DIE.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.HORSE_STEP, 0.25F, 0.8F);
    }

    private enum Phase {
        NEW_MOON("boss.northstar_planet_addon.moon_deer.new_moon"),
        FIRST_QUARTER("boss.northstar_planet_addon.moon_deer.first_quarter"),
        FULL_MOON("boss.northstar_planet_addon.moon_deer.full_moon"),
        ECLIPSE("boss.northstar_planet_addon.moon_deer.eclipse");

        private final String translationKey;

        Phase(String translationKey) {
            this.translationKey = translationKey;
        }

        private static Phase forHealth(float percent) {
            if (percent > 0.75F) return NEW_MOON;
            if (percent > 0.50F) return FIRST_QUARTER;
            if (percent > 0.25F) return FULL_MOON;
            return ECLIPSE;
        }
    }

    private enum Skill {
        NONE(0), DASH(160), BARRAGE(100), RING(120), BEAM(200), MOON_RAIN(240), SHADOW(200), MOON_DUST(180), METEOR(0);

        private final int cooldown;

        Skill(int cooldown) {
            this.cooldown = cooldown;
        }

        private boolean availableIn(Phase phase) {
            return switch (this) {
                case NONE, DASH, BARRAGE, RING, MOON_DUST -> true;
                case BEAM -> phase != Phase.NEW_MOON;
                case MOON_RAIN -> phase == Phase.FULL_MOON || phase == Phase.ECLIPSE;
                case SHADOW -> phase == Phase.FIRST_QUARTER || phase == Phase.ECLIPSE;
                case METEOR -> phase == Phase.ECLIPSE;
            };
        }
    }
}
