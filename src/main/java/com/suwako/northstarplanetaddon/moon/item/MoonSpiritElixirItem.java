package com.suwako.northstarplanetaddon.moon.item;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.registry.NMAAttachments;
import com.suwako.northstarplanetaddon.moon.state.MoonSpiritState;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 月灵丹药 —— 右键进食（消耗 1 个），永久 +2 点生命上限。
 *
 * <p>服用规则由 {@link MoonSpiritState} 决定：</p>
 * <ul>
 *   <li>每服用一枚：{@code uses + 1}，并暂时<b>不再允许</b>服用（{@code canConsume = false}）；</li>
 *   <li>要再吃，必须先让「月灵石坛」赐福把 {@code canConsume} 重新点亮；</li>
 *   <li>一辈子最多 {@link MoonSpiritState#MAX_USES} 次，吃满后连石坛也无法再给次数。</li>
 * </ul>
 *
 * <p>物品没有 {@code food(...)} 组件：食物组件会附带饱食度与饱和度，而这里「吃」只是表现手法。
 * 由此带来一个必须自己处理的细节 —— 1.21.1 的 {@code Item#finishUsingItem} 只在物品带 Food 组件时
 * 才扣数量，所以数量由本类自己 {@code shrink(1)}。</p>
 */
public class MoonSpiritElixirItem extends Item {

    /** 进食时长，与食物一致的 32 tick（1.6 秒）。 */
    private static final int USE_DURATION = 32;

    /**
     * 生命上限修饰符的 id。1.21.1 的 {@link AttributeModifier} 用 {@link ResourceLocation} 作 id，
     * 同 id 在同一属性上只保留一个，所以多次服用是「替换同一个修饰符的量」而不是叠加一堆。
     */
    public static final ResourceLocation BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(MoonModule.NAMESPACE, "moon_spirit_elixir_bonus");

    public MoonSpiritElixirItem(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------ 状态读写

    /** 读取玩家当前进度（附件是同步的，客户端拿到的与服务端一致）。 */
    public static MoonSpiritState state(Player player) {
        return player.getData(NMAAttachments.MOON_SPIRIT_STATE.get());
    }

    /** 写入进度；服务端写完立刻同步给本人，客户端不必写。 */
    public static void setState(ServerPlayer player, MoonSpiritState state) {
        player.setData(NMAAttachments.MOON_SPIRIT_STATE.get(), state);
        player.syncData(NMAAttachments.MOON_SPIRIT_STATE.get());
    }

    // ------------------------------------------------------------------ 物品行为

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        MoonSpiritState state = state(player);
        // 属性修饰符一定是同步的，附件在极端情况下（例如刚登录还没收到初始同步）可能落后一步；
        // 取两者的较大值，避免客户端显示"还能吃"而服务端拒绝的那段误动画。
        boolean atMax = Math.max(state.uses(), usesFromModifier(player)) >= MoonSpiritState.MAX_USES;

        // 两侧判定一致（附件同步），所以被拒绝时客户端也不会起进食动画。
        if (atMax) {
            if (!level.isClientSide()) {
                message(player, "item." + MoonModule.NAMESPACE + ".moon_spirit_elixir.at_max", ChatFormatting.GRAY);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!state.canConsume()) {
            if (!level.isClientSide()) {
                message(player, "item." + MoonModule.NAMESPACE + ".moon_spirit_elixir.needs_altar", ChatFormatting.GRAY);
            }
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /** 从生命上限修饰符反推"已经吃了几次"——作为附件的第二数据源。 */
    private static int usesFromModifier(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        AttributeModifier modifier = maxHealth == null ? null : maxHealth.getModifier(BONUS_ID);
        if (modifier == null) {
            return 0;
        }
        return (int) Math.round(modifier.amount() / MoonSpiritState.MAX_HEALTH_PER_USE);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity consumer) {
        ItemStack result = super.finishUsingItem(stack, level, consumer);
        if (level.isClientSide() || !(consumer instanceof ServerPlayer player)) {
            return result;
        }

        MoonSpiritState state = state(player);
        // 进食动画期间的兜底判定：状态可能已被石坛/指令改动。
        if (!state.canConsume() || state.atMaxUses()) {
            return result;
        }

        MoonSpiritState next = state.afterConsuming();
        setState(player, next);
        applyBonus(player, next.uses());
        // 上限刚抬高 2 点，当前生命还是旧值；补一次治疗，让新增的那颗心立刻是满的。
        player.heal(2.0F);

        result.shrink(1);
        return result;
    }

    // ------------------------------------------------------------------ 生命上限

    /**
     * 让玩家身上的「+2 × 服用次数」生命上限修饰符与进度一致。
     *
     * <p>用 {@code addOrReplacePermanentModifier} 而不是 {@code addPermanentModifier}：
     * 后者遇到同 id 会抛异常，而这里每次服用都要把同一个修饰符的量调大。</p>
     */
    public static void applyBonus(Player player, int uses) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        double amount = MoonSpiritState.MAX_HEALTH_PER_USE * uses;
        if (amount <= 0.0D) {
            maxHealth.removeModifier(BONUS_ID);
            return;
        }
        maxHealth.addOrReplacePermanentModifier(new AttributeModifier(
                BONUS_ID, amount, AttributeModifier.Operation.ADD_VALUE));
    }

    /**
     * 死亡重生 / 换维度后的兜底：玩家实体是新建的，属性与附件是否被带过来由原版与 NeoForge 的
     * 克隆逻辑决定。这里按「附件里的次数」重贴一次修饰符，幂等且不会重复累加。
     */
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MoonSpiritState state = state(player);
            applyBonus(player, state.uses());
        }
    }

    private static void message(Player player, String key, ChatFormatting style) {
        player.displayClientMessage(Component.translatable(key).withStyle(style), true);
    }
}
