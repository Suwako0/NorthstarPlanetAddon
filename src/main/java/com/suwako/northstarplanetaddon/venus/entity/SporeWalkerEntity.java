package com.suwako.northstarplanetaddon.venus.entity;

import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 孢子行者（设计草案第 5 章 #2）：20 HP / 6 伤害 / 护甲 2 / 速度 0.28。
 *
 * <p>真菌森林的精怪，成群出现（3–5 只）。沿用原版僵尸骨架（模型/贴图复用），
 * 但<b>不在日光下燃烧</b>——真菌森林本来就有光斑。</p>
 */
public class SporeWalkerEntity extends Zombie {

    public SporeWalkerEntity(EntityType<? extends SporeWalkerEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.28D);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, DamageSource source,
                                       boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, source, hitByPlayer);
        if (this.random.nextFloat() < 0.75F) {
            this.spawnAtLocation(new ItemStack(ModItems.SPORE_SYRUP.get(), 1));
        }
        this.spawnAtLocation(new ItemStack(ModItems.FUNGUS_FIBER.get(), 2 + this.random.nextInt(2)));
    }
}
