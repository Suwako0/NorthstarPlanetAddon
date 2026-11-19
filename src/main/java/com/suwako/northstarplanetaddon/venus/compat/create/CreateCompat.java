package com.suwako.northstarplanetaddon.venus.compat.create;

import net.neoforged.fml.ModList;

/**
 * Create 软依赖适配（设计草案 12.2 节）。
 *
 * <p><b>为什么是反射而不是编译依赖</b>：本机 Gradle 缓存里没有 Create 的 jar
 * （`maven.modrinth` 组不存在），加 `compileOnly` 会直接构建失败；同时保持
 * 「零第三方编译依赖」也让本模组在没有 Create 的整合包里绝不崩。</p>
 *
 * <p>因此这里只做三件事：探测、记录、为将来的真实实现留出明确的挂载点。
 * 气动马达的机械功走 {@code com.suwako.northstarplanetaddon.venus.machine.WorkConsumer}，
 * 等有 Create jar 后把 {@link #registerStressConsumer()} 补成真实调用即可。</p>
 */
public final class CreateCompat {

    private static final String CREATE_ID = "create";
    private static boolean attempted;
    private static boolean active;

    /** 是否有可用的 Create 集成（当前恒为 false，直到 {@link #registerStressConsumer()} 落实）。 */
    public static boolean isActive() {
        return active;
    }

    public static boolean isCreateLoaded() {
        return ModList.get().isLoaded(CREATE_ID);
    }

    /**
     * 尝试把机械功消费者注册进 Create 的应力体系。
     *
     * <p>实现要点（待补）：Create 6 里应通过
     * {@code com.simibubi.create.api.stress.BlockStressValues} 的注册入口登记
     * 本模组机器的应力影响值，并让气动马达实现 KineticBlockEntity 的转速接口。
     * 由于本机没有 Create 的 jar，任何直接引用都无法编译，这里只保留探测与日志。</p>
     */
    public static void registerStressConsumer() {
        if (attempted) {
            return;
        }
        attempted = true;
        if (!isCreateLoaded()) {
            com.suwako.northstarplanetaddon.venus.VenusModule.LOGGER.info("[VenusModule] Create not present; mechanical work stays on the internal WorkConsumer path.");
            return;
        }
        com.suwako.northstarplanetaddon.venus.VenusModule.LOGGER.info(
                "[VenusModule] Create detected. Stress integration is pending: build with a Create compileOnly dependency to enable it.");
    }

    private CreateCompat() {
    }
}
