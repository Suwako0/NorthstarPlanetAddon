package com.suwako.northstarplanetaddon.mars.item;

import com.suwako.northstarplanetaddon.mars.entity.AresEntity;
import com.suwako.northstarplanetaddon.mars.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 便携式冷却器 —— 手持式高压冷却枪。
 *
 * <ul>
 *     <li><b>按住右键持续喷射</b>：枪口喷出高压水流/液氧射流，命中实体<b>灭火 + 击退 + 少量伤害</b>，
 *         不放置水源；命中阿瑞斯则同时给他降温。</li>
 *     <li><b>弹夹</b>：{@link CoolantTankItem}。潜行 + 右键把副手的罐装进去，
 *         一个罐 {@value #TANK_CAPACITY} mB；开火时按<b>时间</b>消耗（每 tick {@value #DRAIN_PER_TICK} mB，
 *         一个罐约 10 秒），打空后需要再装一罐。</li>
 *     <li>罐本身由机械动力的<b>注液</b>（Spout）灌装，见
 *         {@code data/northstar_planet_addon/recipe/*_coolant_tank.json}。</li>
 * </ul>
 *
 * <p>液态冷却剂用水的 2.67 倍效率：一个水罐 150 降温、一个液氧罐 400 降温（与旧版一致）。</p>
 */
public class PortableCoolerItem extends Item {

    /** 一个弹夹的容量（mB）。 */
    public static final int TANK_CAPACITY = 1000;
    /** 开火时每 tick 消耗的冷却液（mB）—— 一个罐约 200 tick = 10 秒。 */
    public static final int DRAIN_PER_TICK = 5;
    /** 每多少 tick 发一次射流（粒子/伤害节流）。 */
    public static final int FIRE_INTERVAL = 2;
    /** 射程（格）。 */
    public static final double RANGE = 12.0D;

    public static final String WATER = CoolantTankItem.WATER;
    public static final String LIQUID_OXYGEN = CoolantTankItem.LIQUID_OXYGEN;

    /**
     * 每 mB 的降温量（v1.7 起翻倍，用户要求「加强降温效率」）：
     * 水罐 1000 mB × 0.30 = <b>300</b> 降温，液氧罐 1000 mB × 0.80 = <b>800</b> 降温
     * （原来分别是 150 / 400）。
     */
    public static final float WATER_COOLING_PER_MB = 0.30F;
    public static final float OXYGEN_COOLING_PER_MB = 0.80F;

    public PortableCoolerItem(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------ 装填

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 潜行 + 副手拿着装满的罐 → 装填（已满时同样不消耗罐子）
        if (player.isShiftKeyDown()) {
            String kind = CoolantTankItem.kindOf(player.getOffhandItem());
            if (kind == null) {
                return InteractionResultHolder.pass(stack);
            }
            if (isFull(stack)) {
                if (!level.isClientSide) {
                    player.displayClientMessage(
                            Component.translatable("tooltip.northstar_planet_addon.cooler.full")
                                    .withStyle(ChatFormatting.GRAY), true);
                }
                return InteractionResultHolder.fail(stack);
            }
            if (!level.isClientSide) {
                stack.set(ModComponents.COOLANT, kind);
                stack.set(ModComponents.COOLANT_CHARGES, TANK_CAPACITY);
                player.getOffhandItem().shrink(1);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.7F, 1.4F);
            }
            return InteractionResultHolder.success(stack);
        }

        if (getAmount(stack) <= 0) {
            if (!level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.6F);
                player.displayClientMessage(
                        Component.translatable("tooltip.northstar_planet_addon.cooler.no_tank")
                                .withStyle(ChatFormatting.RED), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    // ------------------------------------------------------------------ 持续开火

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide || !(living instanceof Player player)) {
            return;
        }
        String kind = getCoolant(stack);
        int amount = getAmount(stack);
        if (amount <= 0 || kind.isEmpty()) {
            player.stopUsingItem();
            return;
        }
        setAmount(stack, amount - DRAIN_PER_TICK);
        if (remainingUseDuration % FIRE_INTERVAL == 0) {
            fireJet((ServerLevel) level, player, kind);
        }
        if (getAmount(stack) <= 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.2F);
            player.stopUsingItem();
        }
    }

    /** 高压射流：命中实体灭火 + 击退 + 少量伤害；命中阿瑞斯额外降温。不放水。 */
    private void fireJet(ServerLevel level, Player player, String kind) {
        Vec3 from = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 to = from.add(look.scale(RANGE));

        BlockHitResult blockHit = level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = blockHit.getType() == HitResult.Type.MISS ? to : blockHit.getLocation();

        // 射线上最近的实体
        LivingEntity target = null;
        double bestDistance = Double.MAX_VALUE;
        AABB search = player.getBoundingBox().expandTowards(look.scale(RANGE)).inflate(1.0D);
        for (Entity candidate : level.getEntities(player, search, Entity::isAlive)) {
            if (!(candidate instanceof LivingEntity livingCandidate)) {
                continue;
            }
            Optional<Vec3> hit = livingCandidate.getBoundingBox().inflate(0.35D).clip(from, end);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = from.distanceToSqr(hit.get());
            if (distance < bestDistance) {
                bestDistance = distance;
                target = livingCandidate;
                end = hit.get();
            }
        }

        // v1.9：按用户要求**恢复原状**（v1.8 削减得太狠，手感变差）。
        ParticleOptions particle = LIQUID_OXYGEN.equals(kind) ? ParticleTypes.SNOWFLAKE : ParticleTypes.SPLASH;
        Vec3 span = end.subtract(from);
        // v1.9：**枪口特效调小** —— 粒子密度减半，且从第 2 段才开始（枪口附近不再喷）
        int steps = Math.max(1, (int) (span.length() * 0.8D));
        for (int i = 2; i <= steps; i++) {
            Vec3 point = from.add(span.scale(i / (double) steps));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.20F, 1.7F);

        if (target != null) {
            target.clearFire();
            // v1.9：按用户要求**取消冷却器的伤害**（它现在纯粹是降温/灭火工具）
            // 注意：这里刻意**不做击退**。原来用 target.push(...) 直接改速度，
            // 会绕过 Attributes.KNOCKBACK_RESISTANCE（阿瑞斯是 1.0 也没用），
            // 对 Boss 不公平，也把「冷却」这个定位变成了推人道具。
            if (target instanceof AresEntity ares) {
                ares.applyCoolant(kind, Math.round(coolantPerMb(kind) * DRAIN_PER_TICK * FIRE_INTERVAL));
            }
        }
        // 对着自己脚下喷也能灭火
        player.clearFire();
    }

    public static float coolantPerMb(String kind) {
        return LIQUID_OXYGEN.equals(kind) ? OXYGEN_COOLING_PER_MB : WATER_COOLING_PER_MB;
    }

    // ------------------------------------------------------------------ 数据组件读写

    /** 找到玩家身上（主手 → 副手 → 背包）的冷却器物品本身。 */
    public static PortableCoolerItem findCooler(Player player) {
        return isCooler(player.getMainHandItem()) ? (PortableCoolerItem) player.getMainHandItem().getItem()
                : isCooler(player.getOffhandItem()) ? (PortableCoolerItem) player.getOffhandItem().getItem()
                : inventoryStacks(player).stream()
                        .filter(PortableCoolerItem::isCooler)
                        .map(stack -> (PortableCoolerItem) stack.getItem())
                        .findFirst().orElse(null);
    }

    public static boolean isCooler(ItemStack stack) {
        return stack.getItem() instanceof PortableCoolerItem;
    }

    /** 遍历背包（36 主格 + 4 护甲 + 1 副手）里所有非空物品。 */
    private static List<ItemStack> inventoryStacks(Player player) {
        List<ItemStack> result = new ArrayList<>();
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
        return result;
    }

    /**
     * 换弹结果。
     *
     * <p>为什么要分类而不是返回 boolean：三种「没换成」的原因需要给玩家**不同**的提示 ——
     * 以前一律说「没有装填冷却液罐」，满弹时按 R 会得到一句完全误导的提示。</p>
     */
    public enum ReloadResult {
        /** 手上和背包里都没有冷却器。 */
        NO_COOLER,
        /** 冷却器已经装满（{@value PortableCoolerItem#TANK_CAPACITY} mB）—— **满弹不换弹**。 */
        ALREADY_FULL,
        /** 没有找到装满的冷却液罐。 */
        NO_TANK,
        /** 装填成功。 */
        RELOADED
    }

    /** 冷却器是否已满（满了就不该换弹，省得白白吃掉一个罐子）。 */
    public static boolean isFull(ItemStack stack) {
        return getAmount(stack) >= TANK_CAPACITY;
    }

    /**
     * R 键换弹：从背包里找第一个装满的冷却液罐装进冷却器，并消耗掉那个罐。
     *
     * <p>优先装手上的冷却器；手上没有就找背包里的。</p>
     *
     * <p>v1.13：**冷却器已满时直接返回 {@link ReloadResult#ALREADY_FULL}，不消耗任何罐子**。</p>
     */
    public static ReloadResult reloadFromInventory(Player player) {
        ItemStack coolerStack = null;
        if (isCooler(player.getMainHandItem())) {
            coolerStack = player.getMainHandItem();
        } else if (isCooler(player.getOffhandItem())) {
            coolerStack = player.getOffhandItem();
        } else {
            for (ItemStack stack : inventoryStacks(player)) {
                if (isCooler(stack)) {
                    coolerStack = stack;
                    break;
                }
            }
        }
        if (coolerStack == null) {
            return ReloadResult.NO_COOLER;
        }
        // 满弹不换弹
        if (isFull(coolerStack)) {
            return ReloadResult.ALREADY_FULL;
        }

        for (ItemStack candidate : inventoryStacks(player)) {
            String kind = CoolantTankItem.kindOf(candidate);
            if (kind == null) {
                continue;
            }
            if (!player.level().isClientSide) {
                coolerStack.set(ModComponents.COOLANT, kind);
                coolerStack.set(ModComponents.COOLANT_CHARGES, TANK_CAPACITY);
                candidate.shrink(1);
            }
            return ReloadResult.RELOADED;
        }
        return ReloadResult.NO_TANK;
    }

    public static String getCoolant(ItemStack stack) {
        return stack.getOrDefault(ModComponents.COOLANT, "");
    }

    /** 剩余冷却液（mB）。 */
    public static int getAmount(ItemStack stack) {
        return stack.getOrDefault(ModComponents.COOLANT_CHARGES, 0);
    }

    public static void setAmount(ItemStack stack, int amount) {
        int clamped = Math.max(0, Math.min(TANK_CAPACITY, amount));
        if (clamped == 0) {
            stack.remove(ModComponents.COOLANT_CHARGES);
            stack.remove(ModComponents.COOLANT);
        } else {
            stack.set(ModComponents.COOLANT_CHARGES, clamped);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getAmount(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getAmount(stack) / TANK_CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return LIQUID_OXYGEN.equals(getCoolant(stack)) ? 0x66CCFF : 0x3399FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String coolant = getCoolant(stack);
        int amount = getAmount(stack);
        if (amount <= 0 || coolant.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.northstar_planet_addon.cooler.empty")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.northstar_planet_addon.cooler.filled",
                            Component.translatable("tooltip.northstar_planet_addon.coolant." + coolant),
                            amount, TANK_CAPACITY)
                    .withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.northstar_planet_addon.cooler.usage")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.northstar_planet_addon.cooler.reload")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
