package com.suwako.northstarplanetaddon.venus.entity;

import com.suwako.northstarplanetaddon.venus.registry.ModEffects;
import com.suwako.northstarplanetaddon.venus.registry.ModEntities;
import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 酸蚀矛的投掷体（三叉戟式投掷的攻击方式）。
 *
 * <p>沿用三叉戟的三件事：</p>
 * <ul>
 *   <li><b>握持</b>：物品侧 {@code UseAnim.SPEAR}，玩家抬手蓄力；</li>
 *   <li><b>投掷</b>：蓄力 ≥10 tick 松开即飞出，命中留在地面可捡回；</li>
 *   <li><b>命中</b>：直接伤害 + 腐蚀叠层（与近战同一套叠层规则）。</li>
 * </ul>
 *
 * <p>与 {@code ThrownTrident} 的区别：不实现忠诚 / 激流 / 引雷——那些是原版三叉戟专属附魔，
 * 本模组的矛走「腐蚀」路线。</p>
 */
public class ThrownAcidSpear extends ThrowableItemProjectile {

    /** 投掷命中伤害（近战 9，投掷同样 9，但多一层腐蚀）。 */
    public static final float DAMAGE = 9.0F;
    /** 腐蚀持续 8s，与近战一致。 */
    private static final int CORROSION_TICKS = 160;
    /** 命中时追加中毒的概率（与近战 40% 对齐）。 */
    private static final int POISON_CHANCE = 40;

    public ThrownAcidSpear(EntityType<? extends ThrownAcidSpear> type, Level level) {
        super(type, level);
    }

    public ThrownAcidSpear(Level level, LivingEntity shooter, ItemStack stack) {
        super(ModEntities.THROWN_ACID_SPEAR.get(), shooter, level);
        this.setItem(stack.copy());
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.ACIDIC_SPEAR.get();
    }

    /** 三叉戟式抛物线：比雪球沉，能看出是「扔出去的重矛」。 */
    @Override
    protected double getDefaultGravity() {
        return 0.05D;
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.level().isClientSide) {
            return;
        }
        Entity owner = this.getOwner();

        if (result instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target) {
            target.hurt(this.damageSources().thrown(this, owner == null ? this : owner), DAMAGE);

            MobEffectInstance current = target.getEffect(ModEffects.CORROSION);
            int amplifier = current == null ? 0 : Math.min(current.getAmplifier() + 1, 4);
            target.addEffect(new MobEffectInstance(ModEffects.CORROSION, CORROSION_TICKS, amplifier));

            if (owner != null && this.random.nextInt(100) < POISON_CHANCE) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1), owner);
            }
        }

        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ITEM_SLIME,
                    this.getX(), this.getY(), this.getZ(), 12, 0.2, 0.2, 0.2, 0.05);
            server.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    this.getX(), this.getY(), this.getZ(), 2, 0.1, 0.1, 0.1, 0.0);
        }

        // 落点掉出本体，捡回来可以继续用；每投掷一次磨损 1 点（三叉戟同样落地）
        ItemStack drop = this.getItem().copy();
        if (!drop.isEmpty()) {
            drop.setDamageValue(drop.getDamageValue() + 1);
            if (drop.getDamageValue() < drop.getMaxDamage()) {
                this.spawnAtLocation(drop, 0.1F);
            }
        }
        this.discard();
    }
}
