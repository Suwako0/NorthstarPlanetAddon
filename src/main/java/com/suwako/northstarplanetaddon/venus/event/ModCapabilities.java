package com.suwako.northstarplanetaddon.venus.event;

import com.suwako.northstarplanetaddon.venus.registry.ModBlockEntities;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * capability 暴露：让漏斗 / 管道 / 机械手能操作高压锅的槽位。
 *
 * <p>气动本身走相邻方块直连（见 {@code AbstractMachineBlockEntity}），
 * 不占用任何 capability，避免与 Create 的流体/应力 capability 冲突。</p>
 */
public final class ModCapabilities {

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.PRESSURE_COOKER.get(),
                (cooker, side) -> cooker.getInventory());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntities.HIGH_PRESSURE_FURNACE.get(),
                (furnace, side) -> furnace.getInventory());
    }

    private ModCapabilities() {
    }
}
