package com.suwako.northstarplanetaddon.moon.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import io.netty.buffer.ByteBuf;

/**
 * 玩家身上的「月灵丹药」进度，作为 NeoForge 数据附件（data attachment）挂在玩家实体上。
 *
 * <p>两个字段就是全部规则：</p>
 * <ul>
 *   <li>{@code uses} —— 已经服用过几次，0~{@link #MAX_USES}（上限 5）。</li>
 *   <li>{@code canConsume} —— 此刻是否被允许再服一枚（吃完就变 false，要靠月灵石坛重新点亮）。</li>
 * </ul>
 *
 * <p><b>为什么要用附件而不是 {@code getPersistentData()}：</b>附件可以配置 {@code sync(...)}，
 * 于是客户端也拿得到同一份状态。物品的 {@code use()} 因此能在客户端就判定「现在不能吃」，
 * 右键不会先播一段进食动画再被服务端拒绝。持久化则用 {@link #CODEC}，并 {@code copyOnDeath}，
 * 死亡重生后进度不丢。</p>
 */
public record MoonSpiritState(int uses, boolean canConsume) {

    /** 一个玩家一辈子最多服用 5 次。 */
    public static final int MAX_USES = 5;

    /** 每服用一次提升的生命上限（2 点 = 1 颗心）。 */
    public static final double MAX_HEALTH_PER_USE = 2.0D;

    /** 从没吃过丹药的初始状态。 */
    public static final MoonSpiritState INITIAL = new MoonSpiritState(0, true);

    /** 存盘 / 读盘用的 codec（缺字段时回落到初始值，便于旧存档升级）。 */
    public static final Codec<MoonSpiritState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("uses").orElse(0).forGetter(MoonSpiritState::uses),
                    Codec.BOOL.fieldOf("can_consume").orElse(true).forGetter(MoonSpiritState::canConsume))
            .apply(instance, MoonSpiritState::new));

    /** 同步到客户端用的 stream codec。 */
    public static final StreamCodec<ByteBuf, MoonSpiritState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MoonSpiritState::uses,
            ByteBufCodecs.BOOL, MoonSpiritState::canConsume,
            MoonSpiritState::new);

    /** 是否已经吃满 5 次：吃满后连月灵石坛也无法再提供次数。 */
    public boolean atMaxUses() {
        return uses >= MAX_USES;
    }

    /** 服用一枚之后的状态：次数 +1，并且暂时不再允许服用（等石坛重新点亮）。 */
    public MoonSpiritState afterConsuming() {
        return new MoonSpiritState(Math.min(uses + 1, MAX_USES), false);
    }

    /** 月灵石坛赐福之后的状态：次数不变，重新允许服用。 */
    public MoonSpiritState afterAltarBlessing() {
        return new MoonSpiritState(uses, true);
    }

    /** 每次服用提升的生命上限，按服用次数累加得到总加成。 */
    public double bonusMaxHealth() {
        return MAX_HEALTH_PER_USE * uses;
    }
}
