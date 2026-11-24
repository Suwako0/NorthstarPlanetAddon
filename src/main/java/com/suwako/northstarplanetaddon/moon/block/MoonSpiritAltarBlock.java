package com.suwako.northstarplanetaddon.moon.block;

import com.suwako.northstarplanetaddon.moon.MoonModule;
import com.suwako.northstarplanetaddon.moon.item.MoonSpiritElixirItem;
import com.suwako.northstarplanetaddon.moon.registry.NMABlockEntities;
import com.suwako.northstarplanetaddon.moon.state.MoonSpiritState;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 月灵石坛 / Moon Spirit Altar —— 把「月灵丹药」的服用次数还给你。
 *
 * <p>交互规则（全部在服务端判定，玩家收到的都是动作条提示）：</p>
 * <ul>
 *   <li>石坛<b>已失活</b>：提示「已失去灵力」，什么都不做；</li>
 *   <li>玩家已吃满 {@link MoonSpiritState#MAX_USES} 次：提示「容纳不下」，石坛保持激活（不浪费）；</li>
 *   <li>玩家此刻<b>本来就能吃</b>（{@code canConsume}）：提示「先服一枚再来」，石坛保持激活；</li>
 *   <li>玩家刚吃过、正被挡住：赐福 —— 粒子从石坛汇聚到玩家身上，{@code canConsume} 重新点亮，
 *       石坛随即<b>失活</b>（一次性，参考珊瑚变死珊瑚的取舍）。</li>
 * </ul>
 *
 * <p><b>为什么用一个方块 + {@code active} 布尔属性，而不是像珊瑚那样拆成两个方块：</b>
 * 珊瑚拆两个方块是因为它有「活体 / 死体」两套掉落与两套贴图，并且要能被骨粉救活；
 * 这里只是同一件石坛的两种状态，拆成两个方块会带来多余的物品、掉落与存档迁移问题。
 * 用布尔属性同样能在模型上完全区分（{@code blockstates} 里按 {@code active} 指向两套模型），
 * 而且 {@code level.setBlock(..., state.setValue(ACTIVE, false), ...)} 一行就完成枯萎。</p>
 */
public class MoonSpiritAltarBlock extends BaseEntityBlock {

    /** 是否仍有灵力。放置时默认 true（激活），赐福一次后永久 false（失活）。 */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public MoonSpiritAltarBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        // 放下来永远是激活态，不会把旧状态带过来。
        return defaultBlockState().setValue(ACTIVE, true);
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(MoonSpiritAltarBlock::new);
    }

    // ------------------------------------------------------------------ 渲染

    /** {@link BaseEntityBlock} 默认返回 INVISIBLE（留给自定义渲染器），这里用普通方块模型。 */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ------------------------------------------------------------------ 交互

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        // 失活的石坛：只给提示。
        if (!state.getValue(ACTIVE)) {
            if (!level.isClientSide()) {
                message(player, "block." + MoonModule.NAMESPACE + ".moon_spirit_altar.exhausted", ChatFormatting.DARK_GRAY);
                level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 0.6F, 0.8F);
            }
            return InteractionResult.SUCCESS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        MoonSpiritState current = MoonSpiritElixirItem.state(serverPlayer);

        // 已经吃满 5 次：石坛帮不上忙，也不消耗自己的灵力。
        if (current.atMaxUses()) {
            message(player, "block." + MoonModule.NAMESPACE + ".moon_spirit_altar.at_max", ChatFormatting.GRAY);
            serverLevel.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 0.7F, 0.7F);
            return InteractionResult.CONSUME;
        }

        // 此刻本来就能吃（还没吃，或刚被赐福过）：提醒他先吃。
        if (current.canConsume()) {
            message(player, "block." + MoonModule.NAMESPACE + ".moon_spirit_altar.too_early", ChatFormatting.GRAY);
            serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8F, 0.6F);
            return InteractionResult.CONSUME;
        }

        // 赐福：重新允许服用，石坛随即失活。
        MoonSpiritElixirItem.setState(serverPlayer, current.afterAltarBlessing());
        serverLevel.setBlock(pos, state.setValue(ACTIVE, false), Block.UPDATE_ALL);

        serverLevel.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 1.35F);
        serverLevel.playSound(null, pos, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 0.9F, 0.55F);
        if (serverLevel.getBlockEntity(pos) instanceof MoonSpiritAltarBlockEntity altar) {
            altar.beginBlessing(serverPlayer);
        }
        message(player, "block." + MoonModule.NAMESPACE + ".moon_spirit_altar.blessed", ChatFormatting.AQUA);
        return InteractionResult.CONSUME;
    }

    private static void message(Player player, String key, ChatFormatting style) {
        player.displayClientMessage(Component.translatable(key).withStyle(style), true);
    }

    // ------------------------------------------------------------------ 环境特效

    /**
     * 激活状态下的环境星光：石坛上方偶尔浮起一两颗细小星点。
     *
     * <p>放在 {@link Block#animateTick} 而不是服务端 tick：它只在客户端、并且只对玩家附近的方块
     * 随机调用，既不需要方块实体也不产生网络开销。失活后立刻停止（状态判断在第一行）。</p>
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (!state.getValue(ACTIVE)) {
            return;
        }
        // 稀疏一点：大约每 8 次调用才冒一颗，才叫"少量小星光"而不是火花喷泉。
        if (random.nextInt(8) != 0) {
            return;
        }

        double x = pos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 0.9D;
        double y = pos.getY() + 1.0D + random.nextDouble() * 0.45D;
        double z = pos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 0.9D;
        // 极小上升速度 —— 星点是"浮起来"的，不是被喷出去的。
        level.addParticle(ParticleTypes.END_ROD, x, y, z, 0.0D, 0.012D + random.nextDouble() * 0.01D, 0.0D);

        // 偶尔叠一颗更暗的光点，闪烁感来自两种粒子交替。
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.GLOW, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    // ------------------------------------------------------------------ 方块实体

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MoonSpiritAltarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                           BlockEntityType<T> type) {
        // 粒子只在服务端发出（sendParticles），客户端不需要跑 ticker。
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, NMABlockEntities.MOON_SPIRIT_ALTAR.get(),
                (tickLevel, pos, tickState, blockEntity) -> blockEntity.serverTick());
    }
}
