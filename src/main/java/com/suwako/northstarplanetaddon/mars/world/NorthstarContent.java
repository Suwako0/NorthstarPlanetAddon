package com.suwako.northstarplanetaddon.mars.world;

import com.suwako.northstarplanetaddon.mars.MarsModule;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按**注册名**软取 Northstar 的方块。
 *
 * <p>本模组不直接引用 Northstar 的类，全部走 ID 查询：缺少 Northstar 时返回
 * {@link Blocks#AIR}，调用方据此静默跳过（不崩服、不生成错误方块）。</p>
 *
 * <p>首次使用时解析并缓存；世界生成一定发生在注册表冻结之后，因此缓存是安全的。</p>
 */
public final class NorthstarContent {

    private static final Map<String, Block> CACHE = new ConcurrentHashMap<>();

    public static Block marsStone() {
        return block("northstar", "mars_stone");
    }

    public static Block marsDeepStone() {
        return block("northstar", "mars_deep_stone");
    }

    public static Block block(String namespace, String path) {
        String key = namespace + ":" + path;
        return CACHE.computeIfAbsent(key, ignored -> {
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
            if (block == null) {
                MarsModule.LOGGER.warn("Block {} is missing; Mars surface generation will skip it", key);
                return Blocks.AIR;
            }
            return block;
        });
    }

    private NorthstarContent() {
    }
}
