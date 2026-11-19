package com.suwako.northstarplanetaddon.venus.entity;

import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 霜甲潜行者（设计草案第 5 章 #4）：30 HP / 9 伤害 / 护甲 8 / 速度 0.32。
 *
 * <p>金属霜高地的捕食者，护甲极高（8）——近战打它明显比枪械划算。
 * 沿用原版蜘蛛骨架（模型/贴图复用），去掉爬墙速度加成。</p>
 */
public class FrostStalkerEntity extends Spider {

    public FrostStalkerEntity(EntityType<? extends FrostStalkerEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.32D);
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, DamageSource source,
                                       boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, source, hitByPlayer);
        if (this.random.nextFloat() < 0.6F) {
            this.spawnAtLocation(new ItemStack(ModItems.FROST_SCALE.get(), 1));
        }
        if (this.random.nextFloat() < 0.4F) {
            this.spawnAtLocation(new ItemStack(ModItems.STATIC_SAC.get(), 1));
        }
    }
}
