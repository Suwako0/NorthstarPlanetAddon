package com.suwako.northstarplanetaddon;

import com.suwako.northstarplanetaddon.mars.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * 无头验证用的 GameTest：确保这轮重做的装备物品与注册项都在位。
 *
 * <p>只断言服务端可见的东西（物品注册、命名空间、Boss 招式判定），贴图/模型这类纯客户端资源交给
 * {@code check-assets.mjs} 与用户跑 {@code runClient} 看。</p>
 */
@GameTestHolder(NorthstarPlanetAddon.MODID)
public final class NorthstarPlanetAddonGameTests {

    /** 模板名不带命名空间，@GameTestHolder 会自动加前缀。 */
    @GameTest(template = "dreamsleep_alter")
    public static void equipmentItemsRegistered(GameTestHelper helper) {
        Item[] weapons = {
                ModItems.MOLTEN_CORE_SWORD.get(),
                ModItems.STARCORE_CANNON.get(),
                com.suwako.northstarplanetaddon.venus.registry.ModItems.ACIDIC_SPEAR.get(),
                com.suwako.northstarplanetaddon.venus.registry.ModItems.PRESSURE_HAMMER.get(),
                com.suwako.northstarplanetaddon.venus.registry.ModItems.THUNDER_CROSSBOW.get(),
                com.suwako.northstarplanetaddon.venus.registry.ModItems.PRESSURE_DRILL.get(),
        };
        for (Item weapon : weapons) {
            helper.assertTrue(!new ItemStack(weapon).isEmpty(), "武器/器械必须存在： " + weapon);
        }

        Item[] armor = {
                ModItems.MOLTEN_CORE_HELMET.get(),
                com.suwako.northstarplanetaddon.venus.registry.ModItems.TUNGSTEN_HELMET.get(),
                com.suwako.northstarplanetaddon.venus.registry.ModItems.CLOUD_WALKER_CHESTPLATE.get(),
                com.suwako.northstarplanetaddon.venus.registry.ModItems.TUBE_HEAVY_LEGGINGS.get(),
                com.suwako.northstarplanetaddon.venus.registry.ModItems.PHOSPHOROS_BOOTS.get(),
        };
        for (Item piece : armor) {
            helper.assertTrue(!new ItemStack(piece).isEmpty(), "盔甲必须存在： " + piece);
        }

        // 三叉戟式投掷体：注册名与物品都要在，否则掷矛会掉进虚空
        var spearType = com.suwako.northstarplanetaddon.venus.registry.ModEntities.THROWN_ACID_SPEAR.get();
        helper.assertTrue(spearType != null, "投掷体实体类型必须注册");
        helper.assertTrue(
                spearType.builtInRegistryHolder().key().location().getPath().equals("acidic_spear"),
                "投掷体注册名应为 acidic_spear");

        helper.succeed();
    }

    /**
     * 赫菲斯托斯巨像的招式 / 手臂判定回归测试。
     *
     * <p>三件必须在服务端成立的事：</p>
     * <ol>
     *   <li>判定箱覆盖双拳 —— 旧版 {@code sized(6, 9)} 比肩宽（9.25 格）还窄，玩家照着拳头打会打空；</li>
     *   <li>熔岩拳的伤害落在<b>拳头</b>的位置（旧实现是拿躯干盒子往前顶 2.5 格再外扩，拳头根本不在判定里）；</li>
     *   <li>招式状态机真的推进：{@code getAction()} 进入对应动画，护甲板碎掉时插播 {@code plate_break}。</li>
     * </ol>
     */
    @GameTest(template = "hephaestus_arena", timeoutTicks = 200)
    public static void hephaestusMoveSet(GameTestHelper helper) {
        var boss = helper.spawn(
                com.suwako.northstarplanetaddon.venus.registry.ModEntities.HEPHAESTUS.get(),
                new net.minecraft.world.phys.Vec3(10.5D, 3.0D, 8.5D));
        var zombie = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.ZOMBIE,
                new net.minecraft.world.phys.Vec3(6.4D, 3.0D, 9.5D));
        // yaw 0 = 面朝 +Z；模型自身右侧是 -X，所以右拳落在 boss.x - 4.1 格
        boss.setYRot(0.0F);
        boss.setYHeadRot(0.0F);
        zombie.setPos(boss.getX() - 4.1D, boss.getY(), boss.getZ() + 1.0D);
        boss.setTarget(zombie);

        helper.assertTrue(boss.getBbWidth() >= 10.5F,
                "巨像判定箱必须覆盖双拳，实际宽度=" + boss.getBbWidth());
        double fistOffset = Math.abs(boss.fistCenter(false).x - boss.getX());
        helper.assertTrue(fistOffset > 3.0D,
                "拳心应在躯干盒子之外（旧 6 格盒子的半宽是 3），实际偏移=" + fistOffset);

        boss.debugTriggerMove(0);
        helper.assertTrue(boss.getAction() == com.suwako.northstarplanetaddon.venus.entity.boss
                        .HephaestusEntity.ACTION_PUNCH,
                "熔岩拳应立即进入 action 动画，实际=" + boss.getAction());

        helper.runAfterDelay(12L, () -> {
            helper.assertTrue(zombie.getHealth() < zombie.getMaxHealth(),
                    "熔岩拳的伤害必须落在拳头判定箱里，僵尸血量=" + zombie.getHealth());
            boss.damagePlates(1000.0F);
            helper.assertTrue(boss.getAction() == com.suwako.northstarplanetaddon.venus.entity.boss
                            .HephaestusEntity.ACTION_PLATE_BREAK,
                    "护甲板被打碎时应插播 plate_break，实际=" + boss.getAction());
            // 独立破甲：碎掉的板必须写进同步位掩码（客户端据此隐藏 plate_n 骨）
            helper.assertTrue(boss.getPlateMask() != 0,
                    "护甲板碎掉后 DATA_PLATE_MASK 必须置位，实际=" + boss.getPlateMask());
            helper.succeed();
        });
    }

    /**
     * 巨像必须「直驱逼近」：11 格宽的身体在 {@code PathFinder} 里几乎处处无路，
     * 旧实现用 {@code getNavigation().moveTo()} 的结果是原地不动 —— 于是既不走路也不出拳，
     * 玩家看到的就是「Boss 没有动作」。
     */
    @GameTest(template = "hephaestus_arena", timeoutTicks = 200)
    public static void hephaestusWalksTowardTarget(GameTestHelper helper) {
        var boss = helper.spawn(
                com.suwako.northstarplanetaddon.venus.registry.ModEntities.HEPHAESTUS.get(),
                new net.minecraft.world.phys.Vec3(10.5D, 3.0D, 6.5D));
        var bait = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.ZOMBIE,
                new net.minecraft.world.phys.Vec3(10.5D, 3.0D, 17.5D));
        boss.setYRot(0.0F);
        boss.setYHeadRot(0.0F);
        boss.setTarget(bait);

        boss.debugTickAi();
        helper.assertTrue(boss.debugForwardInput() > 0.05F,
                "目标在远处时巨像必须给出前进输入（直驱），实际 zza=" + boss.debugForwardInput());
        // 隔离检查：绕开所有移动管线直接 move，判断是「环境卡住」还是「代码没驱动」
        double z0 = boss.getZ();
        boss.move(net.minecraft.world.entity.MoverType.SELF,
                new net.minecraft.world.phys.Vec3(0.0D, 0.0D, 0.5D));
        helper.assertTrue(boss.getZ() > z0 + 0.05D,
                "直接 move 必须能推动巨像（否则是 Arena 把它卡住了）z: " + z0 + " -> " + boss.getZ()
                        + " | " + boss.debugMoveInfo());
        double before = boss.distanceTo(bait);
        helper.runAfterDelay(25L, () -> {
            double after = boss.distanceTo(bait);
            helper.assertTrue(boss.debugTravelDriveTicks() > 0,
                    "直驱 travel 一次都没生效：travelTicks=" + boss.debugTravelDriveTicks());
            helper.assertTrue(after < before - 0.5D,
                    "巨像必须真的靠近目标，前后距离 " + String.format(java.util.Locale.ROOT, "%.2f -> %.2f", before, after)
                            + " | " + boss.debugMoveInfo());
            helper.succeed();
        });
    }

    /**
     * 用 GeckoLib 自己的 Gson 适配器解析动画文件。
     *
     * <p>{@code GeoModel#getAnimation} 是从静态缓存 {@code GeckoLibCache.getBakedAnimations()} 里取的，
     * 缓存由客户端的资源重载监听器填充；只要解析抛异常或键对不上，控制器就永远拿到 {@code null}，
     * 表现就是「模型有、但一动不动」。这个测试把「生成的问题」和「代码的问题」分开：
     * 解析通过 → 资源没问题，问题在客户端缓存/接线；解析失败 → 生成器写错了格式。</p>
     */
    @GameTest(template = "hephaestus_arena", timeoutTicks = 100)
    public static void hephaestusAnimationsParse(GameTestHelper helper) {
        String animPath = "/assets/northstar_planet_addon/animations/entity/hephaestus.animation.json";
        try (var in = NorthstarPlanetAddonGameTests.class.getResourceAsStream(animPath)) {
            helper.assertTrue(in != null, "动画文件不在 classpath 上：" + animPath);
            // 与 GeckoLib FileLoader#loadAnimationsFile 完全一致：先取 root["animations"]，再交给适配器
            var root = com.google.gson.JsonParser.parseReader(
                    new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            helper.assertTrue(root.has("animations"), "动画文件缺少 animations 根键");
            var gson = new com.google.gson.GsonBuilder()
                    .registerTypeAdapter(software.bernie.geckolib.loading.object.BakedAnimations.class,
                            new software.bernie.geckolib.loading.json.typeadapter.BakedAnimationsAdapter())
                    .create();
            var baked = gson.fromJson(root.getAsJsonObject("animations"),
                    software.bernie.geckolib.loading.object.BakedAnimations.class);
            helper.assertTrue(baked != null && baked.animations() != null,
                    "GeckoLib 解析动画返回 null");
            helper.assertTrue(baked.animations().size() >= 12,
                    "GeckoLib 解析出的动画段数=" + baked.animations().size() + " keys=" + baked.animations().keySet());
            for (String name : new String[]{"idle", "idle_p2", "idle_p3", "walk", "lava_punch", "backhand",
                    "slag_throw", "quake_stomp", "steam_vent", "overload_burst", "plate_break", "death"}) {
                var animation = baked.getAnimation(name);
                helper.assertTrue(animation != null, "GeckoLib 找不到动画名：" + name);
                helper.assertTrue(animation.boneAnimations() != null && animation.boneAnimations().length > 0,
                        "动画没有骨骼通道：" + name);
            }
            helper.succeed();
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable t) {
            helper.assertTrue(false, "GeckoLib 解析动画失败：" + t);
        }
    }

    /*
     * 注：原本想在这里用 helper.makeMockServerPlayerInLevel() 验证「巨像会主动锁定附近的玩家」，
     * 但这套整合里的 Create 会在添加假玩家时发送 create:sync_edge_group 自定义包，无连接直接抛异常，
     * 与测试目标无关。锁定逻辑（getEntitiesOfClass(Player.class, 48 格) + setTarget）改由人工在游戏内确认。
     */

    private NorthstarPlanetAddonGameTests() {
    }
}
