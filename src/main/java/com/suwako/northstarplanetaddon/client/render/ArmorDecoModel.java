package com.suwako.northstarplanetaddon.client.render;

import net.minecraft.world.item.ArmorItem;

/**
 * 3D 盔甲装饰模型的统一入口。
 *
 * <p>由 {@code armor3d.mjs} 生成的 {@code XxxArmorModel} 只负责画装饰件（冠、角、肩甲、披风、
 * 腕箍、腰甲、胫甲），基础甲面仍然由原版 {@code <set>_layer_1/2.png} 那张 64x32 皮肤式
 * 图集负责，所以脸和手不会被 3D 甲片盖住。</p>
 *
 * <p>渲染哪个槽位由 {@link #setSlot(ArmorItem.Type)} 决定：头盔不该把披风和护腿一起画出来。</p>
 */
public interface ArmorDecoModel {

    /** 渲染前调用，决定本次只画哪些部位对应的装饰件。 */
    void setSlot(ArmorItem.Type type);
}
