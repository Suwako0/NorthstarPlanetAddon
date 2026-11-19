package com.suwako.northstarplanetaddon.venus.entity;

import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 菌伞蛾（设计草案第 5 章 #3）：8 HP / 无伤害 / 速度 0.3（飞行）。
 *
 * <p>真菌森林夜间的被动生物，白天本该躲在菌伞下；掉落磷粉。
 * 沿用原版蜜蜂骨架（模型/贴图复用），但不采蜜、不蜇人。</p>
 */
public class MushroomMothEntity extends Bee {

    public MushroomMothEntity(EntityType<? extends MushroomMothEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Bee.createAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.3D);
    }

    @Override
    public boolean canAttackType(net.minecraft.world.entity.EntityType<?> type) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
                                       net.minecraft.world.damagesource.DamageSource source, boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, source, hitByPlayer);
        this.spawnAtLocation(new ItemStack(ModItems.PHOSPHOR_DUST.get(), 1 + this.random.nextInt(2)));
    }
}
