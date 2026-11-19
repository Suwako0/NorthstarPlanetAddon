package com.suwako.northstarplanetaddon.venus.entity;

import com.suwako.northstarplanetaddon.venus.registry.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 熔管蠕虫（设计草案第 5 章 #7）：40 HP / 12 伤害 / 护甲 6 / 速度 0.25（钻地）。
 *
 * <p>熔管长廊的地底伏击者。沿用原版蠹虫骨架（模型/贴图复用）；
 * 「钻地」表现为受击后短暂隐身 20 tick 再从另一侧出现。</p>
 */
public class LavaWormEntity extends Silverfish {

    public LavaWormEntity(EntityType<? extends LavaWormEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Silverfish.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.random.nextFloat() < 0.35F) {
            // 钻地：短暂隐身 + 向后位移
            this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.INVISIBILITY, 20, 0));
            this.setPos(this.getX(), this.getY() - 1.0D, this.getZ());
        }
        return hurt;
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, DamageSource source,
                                       boolean hitByPlayer) {
        super.dropCustomDeathLoot(level, source, hitByPlayer);
        if (this.random.nextFloat() < 0.7F) {
            this.spawnAtLocation(new ItemStack(ModItems.LAVA_GLAND.get(), 1));
        }
        if (this.random.nextFloat() < 0.2F) {
            this.spawnAtLocation(new ItemStack(ModItems.CORE_SHARD.get(), 1));
        }
    }
}
