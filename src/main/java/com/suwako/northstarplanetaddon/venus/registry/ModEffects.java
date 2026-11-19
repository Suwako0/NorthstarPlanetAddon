package com.suwako.northstarplanetaddon.venus.registry;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.effect.CorrosionMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 状态效果。 */
public final class ModEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, VenusModule.NAMESPACE);

    /**
     * 腐蚀（Corrosion）：每层 -1 护甲，满 5 层时最多 -5 护甲；同时周期性造成无视护甲的 1 点伤害。
     *
     * <p>数值来自第 6.2 节：酸蚀矛命中叠 1 层、最多 5 层、持续 8s。</p>
     */
    public static final DeferredHolder<MobEffect, MobEffect> CORROSION =
            MOB_EFFECTS.register("acid_corrosion", CorrosionMobEffect::new);

    private ModEffects() {
    }
}
