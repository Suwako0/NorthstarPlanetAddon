package com.suwako.northstarplanetaddon.venus.entity;

import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 黄铜执事（设计草案第 5 章 #8，精英）：60 HP / 14 伤害（压力锤）/ 护甲 10 / 速度 0.2。
 *
 * <p>云港遗迹的守卫（固定 1–2 只）。沿用原版卫道士骨架，
 * 出生时手持本模组的压力锤——正好复用「压力锤震荡波 → Boss 过载」那条代码路径。</p>
 */
public class BrassDeaconEntity extends Vindicator {

    public BrassDeaconEntity(EntityType<? extends BrassDeaconEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Vindicator.createAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
    }

    /** 出生时装备压力锤（{@link net.minecraft.world.entity.Mob#finalizeSpawn} 之后由生成事件设置）。 */
    public void equipPressureHammer() {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.PRESSURE_HAMMER.get()));
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, DamageSource source,
                                       boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, source, hitByPlayer);
        this.spawnAtLocation(new ItemStack(ModItems.CLOUD_MAP_FRAGMENT.get(), 1));
        this.spawnAtLocation(new ItemStack(ModItems.PRE_CIVILIZATION_ALLOY.get(), 2));
        if (this.random.nextFloat() < 0.5F) {
            this.spawnAtLocation(new ItemStack(ModItems.AEROSTAT_CORE.get(), 1));
        }
    }
}
