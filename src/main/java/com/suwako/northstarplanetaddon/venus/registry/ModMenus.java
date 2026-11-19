package com.suwako.northstarplanetaddon.venus.registry;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.menu.MachineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 容器菜单类型。 */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, VenusModule.NAMESPACE);

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE =
            MENU_TYPES.register("machine", () -> IMenuTypeExtension.create((id, inventory, data) ->
                    MachineMenu.client(id, inventory)));

    private ModMenus() {
    }
}
