package com.suwako.northstarplanetaddon.venus.combat;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * 本模组的伤害类型（数据包注册，代码只持 {@link ResourceKey}）。
 *
 * <p>酸蚀伤害<b>刻意不设置直接实体</b>（direct entity 为 null，只带 causing entity），
 * 这样 {@link DamageClassifier} 会按设计草案 2.3 节的顺序先排除「直接实体是生物」的近战分支，
 * 再命中 {@code #northstar_planet_addon:acid} 标签，从而正确归入 {@link DamageKind#ACID}。</p>
 *
 * <p>对应的数据文件：{@code data/phosphoros/damage_type/acid.json} 与
 * {@code data/phosphoros/tags/damage_type/acid.json}。</p>
 */
public final class ModDamageSources {

    public static final ResourceKey<DamageType> ACID =
            ResourceKey.create(Registries.DAMAGE_TYPE, VenusModule.id("acid"));

    @Nullable
    public static DamageSource acid(Level level, @Nullable Entity attacker) {
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(ACID);
        return new DamageSource(holder, null, attacker);
    }

    private ModDamageSources() {
    }
}
