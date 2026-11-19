package com.suwako.northstarplanetaddon.mars.registry;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.effect.CorrosionEffect;
import com.suwako.northstarplanetaddon.mars.effect.CorrosionResistanceEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 状态效果：腐蚀（有害）与耐腐蚀（有益）。 */
public final class ModEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, MarsModule.NAMESPACE);

    /** 恶魔蛾鳞粉造成的腐蚀：持续伤害并磨损护甲。 */
    public static final DeferredHolder<MobEffect, MobEffect> CORROSION =
            MOB_EFFECTS.register("corrosion", CorrosionEffect::new);

    /** 火星钢护甲提供的耐腐蚀：免疫腐蚀效果与蛾类伤害加成。 */
    public static final DeferredHolder<MobEffect, MobEffect> CORROSION_RESISTANCE =
            MOB_EFFECTS.register("corrosion_resistance", CorrosionResistanceEffect::new);

    private ModEffects() {
    }
}
