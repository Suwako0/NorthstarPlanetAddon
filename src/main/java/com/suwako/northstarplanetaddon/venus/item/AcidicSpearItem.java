package com.suwako.northstarplanetaddon.venus.item;

import com.suwako.northstarplanetaddon.venus.entity.ThrownAcidSpear;
import com.suwako.northstarplanetaddon.venus.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 酸蚀矛（{@code acidic_spear}）—— 三层攻击模组 + 原有「腐蚀叠层」。
 *
 * <p>攻击模组由 {@code attack.mjs} 生成（poison 主题），三层结构：</p>
 * <ol>
 *   <li><b>命中层</b> {@link #hurtEnemy}：保留原设计的腐蚀叠层（每命中 +1 层，最多 5 层、8s），
 *       并在 35% 概率下追加 6 秒中毒；</li>
 *   <li><b>主动层</b> {@link #use}：右键半径 3 的范围技（4 点伤害 + 击退），命中后短时力量，
 *       冷却 100 tick；</li>
 *   <li><b>被动层</b> {@link #inventoryTick}：手持时每 25 tick 冒毒雾粒子。</li>
 * </ol>
 */
public class AcidicSpearItem extends SwordItem {

    /** 腐蚀持续 8s。 */
    public static final int CORROSION_DURATION = 160;
    /** 最大 5 层（amplifier 0–4）。 */
    public static final int CORROSION_MAX_LEVEL = 5;

    /* ---- 攻击模组参数（attack.mjs · poison） ---- */
    private static final int HIT_CHANCE = 40;
    private static final int HIT_DURATION = 120;
    private static final int HIT_AMPLIFIER = 1;
    private static final double ABILITY_RADIUS = 4;
    private static final double ABILITY_VERTICAL_RADIUS = 1;
    private static final float ABILITY_DAMAGE = 4F;
    private static final double ABILITY_KNOCKBACK = 0.6;
    private static final int ABILITY_COOLDOWN = 120;
    private static final int PASSIVE_EVERY = 20;

    /* ---- 三叉戟式投掷 ---- */
    /** 最短蓄力：低于这个 tick 数松手不投掷（与三叉戟一致）。 */
    private static final int MIN_THROW_CHARGE = 10;
    /** 投掷初速。 */
    private static final float THROW_SPEED = 2.4F;

    public AcidicSpearItem(Properties properties, Tier tier) {
        super(tier, properties);
    }

    public AcidicSpearItem(Properties properties) {
        this(properties, Tiers.NETHERITE);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.northstar_planet_addon.acidic_spear.desc")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.northstar_planet_addon.acidic_spear.throw").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.northstar_planet_addon.acidic_spear.ability",
                Math.round(ABILITY_DAMAGE), ABILITY_COOLDOWN / 20).withStyle(ChatFormatting.GOLD));
    }

    /** 命中层：腐蚀叠层（原设计）+ 35% 概率中毒。 */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean hit = super.hurtEnemy(stack, target, attacker);
        if (!hit) {
            return false;
        }
        MobEffectInstance current = target.getEffect(ModEffects.CORROSION);
        int amplifier = current == null ? 0 : Math.min(current.getAmplifier() + 1, CORROSION_MAX_LEVEL - 1);
        target.addEffect(new MobEffectInstance(ModEffects.CORROSION, CORROSION_DURATION, amplifier));

        Level level = attacker.level();
        if (!level.isClientSide && attacker.getRandom().nextInt(100) < HIT_CHANCE) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, HIT_DURATION, HIT_AMPLIFIER), attacker);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.ITEM_SLIME,
                        target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                        8, 0.35, 0.35, 0.35, 0.02);
            }
        }
        return true;
    }

    /** 主动层：右键蓄力（三叉戟式投掷）；潜行右键仍是「酸液迸发」。 */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            return this.acidBurst(level, player, stack, hand);
        }
        // 只剩 1 点耐久就别掷了，免得矛直接碎在半空（三叉戟同样拒绝）
        if (stack.getMaxDamage() - stack.getDamageValue() <= 1) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /** 握持姿势与三叉戟同款：抬手、蓄力、掷出。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    /** 蓄力 ≥10 tick 松手即掷出；矛离手，落点磨损 1 点。 */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }
        int charge = this.getUseDuration(stack, entity) - timeLeft;
        if (charge < MIN_THROW_CHARGE) {
            return;
        }
        if (!level.isClientSide) {
            ThrownAcidSpear spear = new ThrownAcidSpear(level, player, stack);
            spear.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, THROW_SPEED, 1.0F);
            level.addFreshEntity(spear);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.1F);
            // 三叉戟同款：矛离开手上，变成飞行体；落点掉出本体供捡回
            stack.shrink(1);
        }
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
    }

    /** 潜行 + 右键：范围「酸液迸发」。 */
    private InteractionResultHolder<ItemStack> acidBurst(Level level, Player player, ItemStack stack,
                                                         InteractionHand hand) {
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        double r = ABILITY_RADIUS;
        AABB box = new AABB(
                player.getX() + r, player.getY() + ABILITY_VERTICAL_RADIUS, player.getZ() + r,
                player.getX() - r, player.getY() - ABILITY_VERTICAL_RADIUS, player.getZ() - r);

        int hits = 0;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !player.isAlliedTo(e))) {
            hits++;
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
            target.setDeltaMovement(dx / len * ABILITY_KNOCKBACK, 0.45, dz / len * ABILITY_KNOCKBACK);
            target.hurtMarked = true; // 不加这句客户端不应用击退
            target.hurt(level.damageSources().playerAttack(player), ABILITY_DAMAGE);
        }

        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.ITEM_SLIME,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    24, r * 0.5, 0.6, r * 0.5, 0.15);
            server.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    player.getX(), player.getY() + 1.0, player.getZ(), 6, r * 0.4, 0.4, r * 0.4, 0.0);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 1.2F);

        if (hits > 0) {
            int ticks = Math.min(200, 40 + hits * 20);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, Math.min(2, hits / 2)), player);
        }
        player.getCooldowns().addCooldown(this, ABILITY_COOLDOWN);
        player.swing(hand, true);
        return InteractionResultHolder.success(stack);
    }

    /** 被动层：手持时每 25 tick 冒毒雾。 */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (!selected || level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        if (player.tickCount % PASSIVE_EVERY != 0) {
            return;
        }
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.WITCH,
                    player.getX(), player.getY() + 0.9, player.getZ(), 2, 0.25, 0.3, 0.25, 0.0);
        }
    }
}
