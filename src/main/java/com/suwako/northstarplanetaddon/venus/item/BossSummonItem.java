package com.suwako.northstarplanetaddon.venus.item;

import com.suwako.northstarplanetaddon.venus.entity.boss.AbstractBossEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.function.Supplier;

/**
 * Boss 召唤物（设计草案 4.2–4.4 节的召唤条件）。
 *
 * <ul>
 *   <li>{@code whale_bone_horn} 鲸骨号角 → 涅斐勒（云港遗迹顶层）</li>
 *   <li>{@code pressure_valve} 压力阀 → 赫菲斯托斯（高压观测塔底层）</li>
 *   <li>{@code sulfur_heart} 硫心 → 阿佛洛狄忒（熔管城市核心层）</li>
 * </ul>
 *
 * <p>规则与草案一致：附近 64 格内已经有 Boss 时拒绝召唤，防止刷怪蛋式堆叠；
 * 召唤成功后消耗 1 个（创造模式不消耗）。</p>
 */
public class BossSummonItem extends Item {

    /** 召唤前检查的同屏半径（与 Boss 血条追踪距离一致）。 */
    private static final double CHECK_RADIUS = 64.0D;

    private final Supplier<EntityType<?>> bossType;

    public BossSummonItem(Properties properties, Supplier<EntityType<?>> bossType) {
        super(properties.stacksTo(1));
        this.bossType = bossType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (!(level instanceof ServerLevel) || player == null) {
            return InteractionResult.SUCCESS;
        }
        BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());

        if (!level.getEntitiesOfClass(AbstractBossEntity.class,
                new AABB(spawnPos).inflate(CHECK_RADIUS)).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.northstar_planet_addon.summon_blocked")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        Entity entity = this.bossType.get().create(level);
        if (!(entity instanceof AbstractBossEntity boss)) {
            return InteractionResult.FAIL;
        }
        boss.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                player.getYRot() + 180.0F, 0.0F);
        level.addFreshEntity(boss);

        level.playSound(null, spawnPos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.2F, 0.6F);
        player.displayClientMessage(Component.translatable("message.northstar_planet_addon.summon_success",
                        boss.getDisplayName())
                .withStyle(ChatFormatting.DARK_RED), false);

        if (!player.getAbilities().instabuild) {
            ItemStack stack = context.getItemInHand();
            stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
