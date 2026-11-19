package com.suwako.northstarplanetaddon.mars.event;

import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import com.suwako.northstarplanetaddon.mars.registry.ModEffects;
import com.suwako.northstarplanetaddon.mars.registry.ModItems;
import com.suwako.northstarplanetaddon.mars.registry.ModTags;
import com.suwako.northstarplanetaddon.mars.world.Sandstorm;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** 玩家相关玩法：火星钢护甲耐腐蚀、火星钢镐双倍挖掘速度、沙尘暴磨损装备。 */
@EventBusSubscriber(modid = NorthstarPlanetAddon.MODID)
public final class PlayerEvents {

    /** 记为「穿了火星钢护甲」所需的件数（头盔位让给护目镜，所以是 3 件）。 */
    private static final int REQUIRED_ARMOR_PIECES = 3;

    /** 沙尘暴磨损装备的间隔（tick）：每 10 秒给每个已装备部位掉 1 点耐久。 */
    private static final int SANDSTORM_WEAR_INTERVAL = 200;

    /** 集齐 3 件以上火星钢护甲 → 持续获得「耐腐蚀」，免疫恶魔蛾鳞粉腐蚀。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }

        if (player.tickCount % 40 == 0) {
            int pieces = 0;
            for (ItemStack stack : player.getArmorSlots()) {
                if (stack.is(ModTags.Items.CORROSION_RESISTANT_ARMOR)) {
                    pieces++;
                }
            }
            if (pieces >= REQUIRED_ARMOR_PIECES) {
                player.addEffect(new MobEffectInstance(ModEffects.CORROSION_RESISTANCE, 100, 0, false, false));
            }
        }

        // 火星沙尘暴：缓慢磨损装备
        if (player.tickCount % SANDSTORM_WEAR_INTERVAL == 0 && Sandstorm.isActive(player.level())) {
            wearEquipmentBySandstorm(player);
        }
    }

    /**
     * 沙尘暴缓慢降低装备耐久度。
     *
     * <p>规则：每 {@link #SANDSTORM_WEAR_INTERVAL} tick，给每个**已装备**的护甲与手持物品各掉 1 点耐久。
     * 暴露在天空下才算（室内 / 密封基地不受影响）；创造与旁观模式不受影响；
     * 沙尘暴护目镜本身豁免（它就是为了沙尘暴而造的）。</p>
     *
     * <p>v1.11：穿齐**熔核护甲四件套**时完全免疫沙尘暴磨损（《Boss属性重新设计》里
     * 「全套提供 … 免疫沙尘暴磨损」）。</p>
     */
    private static void wearEquipmentBySandstorm(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        if (ArmorEvents.wearsFullSet(player)) {
            return;
        }
        if (!player.level().canSeeSky(player.blockPosition())) {
            return;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            EquipmentSlot.Type type = slot.getType();
            if (type != EquipmentSlot.Type.HUMANOID_ARMOR && type != EquipmentSlot.Type.HAND) {
                continue;
            }
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                continue;
            }
            if (slot == EquipmentSlot.HEAD && stack.is(ModItems.SANDSTORM_GOGGLES.get())) {
                continue;
            }
            stack.hurtAndBreak(1, player, slot);
        }
    }

    /** 火星钢镐挖掘赤铁岩 / 火星石 / 火山岩时速度翻倍，其余方块维持铁镐水平。 */
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getState().is(ModTags.Blocks.MARTIAN_HARD_ROCK)) {
            return;
        }
        if (!event.getEntity().getMainHandItem().is(ModTags.Items.MARTIAN_STEEL_PICKAXES)) {
            return;
        }
        event.setNewSpeed(event.getOriginalSpeed() * 2.0F);
    }

    private PlayerEvents() {
    }
}
