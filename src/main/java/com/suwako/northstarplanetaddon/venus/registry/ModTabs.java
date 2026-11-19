package com.suwako.northstarplanetaddon.venus.registry;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 创造模式物品栏。 */
public final class ModTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, VenusModule.NAMESPACE);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN =
            CREATIVE_MODE_TABS.register("venus", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.northstar_planet_addon.venus"))
                    .icon(() -> new ItemStack(ModItems.PHOSPHOROS_CORE.get()))
                    .displayItems((params, output) -> {
                        // 按注册顺序输出，配方里出现的顺序即玩家看到的顺序
                        ModItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.get()));
                    })
                    .build());

    private ModTabs() {
    }
}
