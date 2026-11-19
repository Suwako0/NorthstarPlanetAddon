package com.suwako.northstarplanetaddon.mars.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 耐腐蚀：由火星钢护甲提供，免疫恶魔蛾鳞粉造成的腐蚀。
 *
 * <p>{@link MobEffect} 的构造器是 protected，所以需要这个公开子类。</p>
 */
public class CorrosionResistanceEffect extends MobEffect {

    public CorrosionResistanceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x9BB0C9);
    }
}
