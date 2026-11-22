package com.suwako.northstarplanetaddon.mars.item;

import com.suwako.northstarplanetaddon.mars.world.StarcoreBombardment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;

import java.util.List;

/**
 * 星核炮（{@code starcore_cannon}）。
 *
 * <p>数值来自《Boss属性重新设计》：<b>攻击力 20</b>（{@code createAttributes(NETHERITE, 16, -2.6)} ⇒
 * 修饰符 16 + 3 = 19，游戏内显示 20）、<b>右键释放星核炮击</b>：在准心指向的方块处召唤星核，按
 * {@link StarcoreBombardment} 的规则轰炸 5 秒（半径 6、每 0.5 秒 20 点）。</p>
 *
 * <p><b>冷却 {@value #COOLDOWN_TICKS} tick = 30 秒</b>：文档只写了「攻击间隔 2」，我先按 2 秒实现，
 * 用户明确要求改成 30 秒 —— 5 秒轰炸配 2 秒冷却确实过强。</p>
 *
 * <p>另外保留「同一门炮在轰炸结束前不能再次开火」的判断：30 秒冷却下它其实永远不会触发
 * （轰炸只有 5 秒），留着是为了以后调低冷却时不会叠出多轮轰炸。消耗 1 点耐久。</p>
 */
public class StarcoreCannonItem extends Item {

    /** 右键技能冷却：30 秒（用户指定）。 */
    public static final int COOLDOWN_TICKS = 600;
    /** 瞄准距离：超出这个距离就打在射线上第一个方块处。 */
    private static final double AIM_RANGE = 48.0D;
    /** 被动层：手持时每 15 tick 一次粒子（attack.mjs 生成参数）。 */
    private static final int PASSIVE_EVERY = 20;

    public StarcoreCannonItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.northstar_planet_addon.starcore_cannon")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.northstar_planet_addon.starcore_cannon.ability",
                COOLDOWN_TICKS / 20).withStyle(ChatFormatting.YELLOW));
    }

    /**
     * 被动层（attack.mjs · flame 主题）：手持时每 15 tick 冒一簇熔岩粒子。
     *
     * <p>本物品的主动层是 {@link StarcoreBombardment} 星核炮击，命中层不适用（不是近战武器类），
     * 所以攻击模组只接被动层。</p>
     */
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
            server.sendParticles(ParticleTypes.LAVA,
                    player.getX(), player.getY() + 0.9, player.getZ(), 2, 0.25, 0.3, 0.25, 0.0);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }
        if (player.getCooldowns().isOnCooldown(stack.getItem())
                || StarcoreBombardment.isActiveFor(player)) {
            return InteractionResultHolder.fail(stack);
        }

        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getViewVector(1.0F).scale(AIM_RANGE));
        BlockHitResult hit = serverLevel.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 target = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();

        StarcoreBombardment.start(serverLevel, target, player);
        player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN_TICKS);
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.swing(hand, true);
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.4F, 0.5F);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
