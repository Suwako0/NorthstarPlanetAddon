package com.suwako.northstarplanetaddon.mars.registry;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 本模组创造模式物品栏。 */
public final class ModTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MarsModule.NAMESPACE);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            CREATIVE_TABS.register("mars", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.northstar_planet_addon.mars"))
                    .icon(() -> new ItemStack(ModItems.DARK_RED_SCALE_DUST.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.RED_IRONSTONE.get());
                        output.accept(ModItems.MARS_ICE.get());
                        output.accept(ModItems.DRY_ICE.get());
                        output.accept(ModItems.STAR_CORE.get());
                        output.accept(ModItems.ARES_ALTAR_CASING.get());
                        output.accept(ModItems.DEMON_MOTH_WING_MEMBRANE.get());
                        output.accept(ModItems.DARK_RED_SCALE_DUST.get());
                        output.accept(ModItems.ANTI_CORROSION_COATING.get());
                        output.accept(ModItems.SANDSTORM_GOGGLES.get());
                        output.accept(ModItems.PORTABLE_COOLER.get());
                        output.accept(ModItems.COOLANT_TANK.get());
                        output.accept(ModItems.WATER_COOLANT_TANK.get());
                        output.accept(ModItems.LIQUID_OXYGEN_COOLANT_TANK.get());
                        output.accept(ModItems.MARS_STRAWBERRY.get());
                        output.accept(ModItems.MARS_STRAWBERRY_SEEDS.get());
                        output.accept(ModItems.STAR_CORE_SHARD.get());
                        output.accept(ModItems.STAR_CORE_HEART.get());
                        output.accept(ModItems.MOLTEN_CORE_SWORD.get());
                        output.accept(ModItems.STARCORE_CANNON.get());
                        output.accept(ModItems.MOLTEN_CORE_HELMET.get());
                        output.accept(ModItems.MOLTEN_CORE_CHESTPLATE.get());
                        output.accept(ModItems.MOLTEN_CORE_LEGGINGS.get());
                        output.accept(ModItems.MOLTEN_CORE_BOOTS.get());
                        output.accept(ModItems.DEMON_MOTH_SPAWN_EGG.get());
                        output.accept(ModItems.ARES_SPAWN_EGG.get());
                    })
                    .build());

    private ModTabs() {
    }
}
