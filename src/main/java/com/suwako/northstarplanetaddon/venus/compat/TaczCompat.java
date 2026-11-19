package com.suwako.northstarplanetaddon.venus.compat;

import com.suwako.northstarplanetaddon.venus.VenusModule;
import com.suwako.northstarplanetaddon.venus.config.PhosphorosConfig;
import com.suwako.northstarplanetaddon.venus.registry.ModTags;
import net.minecraft.core.registries.Registries;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * TaCZ 兼容层（设计草案 12.2 节）。
 *
 * <p>刻意<b>不写 compileOnly 依赖</b>：TaCZ 或其枪包一旦改类名/包名，
 * 硬编译依赖会让整个模组启动即崩。分类只依赖伤害类型标签，
 * 标签文件可随整合包热修，所以启动时只做一次自检并打日志。</p>
 */
public final class TaczCompat {

    public static void onServerStarted(ServerStartedEvent event) {
        if (!PhosphorosConfig.TACZ_INTEGRATION.get()) {
            return;
        }
        if (!ModList.get().isLoaded("tacz")) {
            VenusModule.LOGGER.info("[VenusModule] 未检测到 TaCZ：弹道伤害走 #minecraft:is_projectile 兜底");
            return;
        }
        int entries = event.getServer().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getTag(ModTags.DamageTypes.BALLISTIC)
                .map(set -> set.size())
                .orElse(-1);
        if (entries <= 1) {
            VenusModule.LOGGER.warn("[VenusModule] 弹道标签解出 {} 条伤害类型，"
                    + "请检查 data/phosphoros/tags/damage_type/ballistic.json 是否命中当前 TaCZ 版本", entries);
        } else {
            VenusModule.LOGGER.info("[VenusModule] 弹道标签已解出 {} 条伤害类型（TaiCZ 兼容层生效）", entries);
        }
    }

    private TaczCompat() {
    }
}
