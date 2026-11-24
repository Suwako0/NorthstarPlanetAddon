# 金星工业线 · 整体状况报告

> ⚠️ **历史文档（2026-11-24 起失效）**：本报告描述的金星气动 / 压力机器体系已按需求**整体移除**——
> 10 台机器方块与方块实体、气动网络、机械功与过热、`high_pressure` 配方类型（含 3 条配方）、
> 机器 GUI、`ModCapabilities`、`compat/create` 与 `compat/northstar` 两套桥接全部删除。
> 备份保留在项目根目录 `.backup/industry-2026*`/（含 `MANIFEST.txt` 与全部原始文件）。
> 下文保留为**移除前的状况记录**：若要重做工业线，§8 的优先级建议与 §7 的坑依然适用。
> 移除时顺手处理：气压计去掉了“气压（bar）”读数（`bar` 字段连同 payload/HUD 一起删除），
> 酸泵站结构里的泵体从 `pressure_tank` 改成磨制深板岩。

> 基准：当前工程代码逐文件反查 + **本次构建验证**（`gradle build` → `BUILD SUCCESSFUL in 17s`，`compileJava` UP-TO-DATE，链接 6 个 libs jar）。
> 范围：`venus` 工业线 = 气动 / 压力 / 温度机器体系 + 高压熔炼配方 + Create / Northstar 兼容层。
> **不含**：战斗与伤害管线、世界生成结构、装备武器、群系生物（各有独立文档）。
> 文中每个数字都标了出处类名，可直接跳代码核对。

---

## 0. 一句话结论

**机器闭环是通的，但"能玩"还差三口气。**

已实装且自洽：10 个方块实体、气动网络（1 bar = 1000 mB、相邻直推）、过热停机、独立的高压熔炼配方类型、10 台机器全部有配方与掉落表。

但有 **4 个真实缺口**，其中 2 个足以让玩家觉得"这线没做完"：

| 级别 | 缺口 | 后果 |
|---|---|---|
| **P0** | Create 联动在语义上没接通：`PneumaticMotorBlock` 没实现 `IRotate` | 有 Create 时马达拿不到轴转速，`getSpeed()` 恒 0 → 4 SU 应力、×2 加速都不生效 |
| **P0** | `tungsten_ingot` / `tungsten_block` 无任何获取途径（只有 `tungsten_plate` 能从 4 个箱子出） | 玩家可以靠战利品起步，但"钨"这个材料体系本身是断的 |
| P1 | `acid_proof_alloy_plate` 没有用途（0 条配方消费它） | 高压熔炉的最高价值产物是个死胡同道具 |
| P1 | 出厂即 14,157 方块/机器/每 tick 的调节器扫描 | 装了 Northstar 的整合包里，机器一多就掉 TPS |

外加两个 P2：高压熔炼配方只有 3 条（2 条是食物）、把材料从炉里换掉不重置进度（可被利用）。

---

## 1. 交付物盘点（10 台机器）

全部注册于 `registry/ModBlocks` + `registry/ModBlockEntities`；10/10 有合成配方（`data/.../recipe`）；10/10 有方块掉落表（`data/.../loot_table/blocks`）。

| 机器 | 方块 id | 功能 | 关键数值 | 交互 | GUI | 空气角色 | 物品 capability |
|---|---|---|---|---|---|---|---|
| 压缩机 | `compressor` | 产压空气 | 缓存 4000；手摇一次 +2000 mB，冷却 40t；每 tick 每邻居推 2000 | 右击摇曲柄 | — | 源（不收气） | — |
| 压力罐 | `pressure_tank` | 储气 / 串联 | 每罐 8000；每 tick 每邻居推 500；垂直列容量累加（`getColumnCapacity`） | 右击看整列气压 | — | 双向 | — |
| 高压锅 | `pressure_cooker` | 批量熔炼（原版熔炉配方） | 缓存 8000；4 入 1 出；200t 完成；20 mB/t；需 8 bar；有机械功 ×2 | 右击投料 / 空手取产物，也可开 GUI | ✅ | 消费者 | ✅ |
| 高压熔炉 | `high_pressure_furnace` | 高压熔炼（自定义配方） | 缓存 8000；4 入 1 出；默认 400t；20 mB/t；需 8 bar + 1200°C | 同上 | ✅ | 消费者 | ✅ |
| 散热器 | `radiator` | 给周围机器降温 | 冷却剂 6400t 上限：雪球 100 / 冰 400 / 浮冰 800 / 蓝冰 1600；每台 −40°C；自不过热 | 右击投冷却剂 | — | 不参与 | — |
| 冷凝塔 | `condenser` | 气 + 热 → 冰 | 缓存 4000；2 bar + 300°C；50 mB/t；1200t 出 1 冰 | 右击收冰 / 看进度 | — | 消费者 | ❌ |
| 云梯 | `cloud_lift` | 垂直传送 | 100 mB / 次；±32 格；60t 冷却（潜行向下） | 右击（手持物也行） | — | 消费者 | — |
| 气动马达 | `pneumatic_motor` | 空气 → 32 RPM 机械功 | 20 mB/t；缓存 8000；轴向三态（`AXIS`） | 右击看状态 | — | 消费者 | — |
| 酸泵 | `acid_pump` | 抽硫酸 | 缓存 8000 mB 酸；200 mB/t；扫描半径 3（±1 高）；1 桶 = 1000 mB | 右击装桶 | — | 不参与 | — |
| 菌床 | `fungus_bed` | 产菌丝纤维 | 9600t（8 分钟）一茬，2–4 个；不收不生长 | 右击收取 | — | 不参与 | ❌（注释说"漏斗可抽"，实际未注册） |

**判定**：机器**种类**已经够一条线（压缩 → 储气 → 消费 → 降温 → 产出），缺的是**产出用途**与**能源上限**（见 §5、§6）。

---

## 2. 气动网络模型（`machine/PneumaticStorage` + `AbstractMachineBlockEntity`）

- **换算**：1 bar = 1000 mB（`PneumaticStorage.MILLIBUCKET_PER_BAR`）。
- **传输**：不做管道、不占 capability。每 tick 六向遍历，只认相邻的 `AbstractMachineBlockEntity`，按 `acceptsAir()` / `suppliesAir()` 决定能否推/拉；每邻居每 tick 有上限（压缩机 2000、压力罐 500、锅 400、炉 800、云梯 800）。
- **为什么这么做**（代码注释）：避免和 Create 的流体/应力 capability 打架。

**这个取舍的后果（要提前知道）**：

1. 远距离/转角传输没有任何介质，只能靠**压力罐串联**铺过去（每罐 +8000，每 tick 只推 500 → 10 罐串起来理论 5000 mB/t，实际受接收端 `receive` 上限约束）。
2. 罐子之间是**自动均压**的（都往邻居推），所以"储气列"实际上是共享缓冲池，玩家不能做"分区管网"。
3. 气动是**纯整数 mB**、无压力损失、无爆管——手感上更像"电池"而不是"气压"。

---

## 3. 机械功与加速（`machine/WorkConsumer`）

| 来源 | 输出 | 消耗 |
|---|---|---|
| 气动马达（无 Create） | 32 RPM | 20 mB/t |
| 气动马达（有 Create，`KineticPneumaticMotorEntity`） | Create 轴转速 | 声明 4 SU/RPM 应力 |

**实际实现是"有动力就 ×2"，没有分级**，因为：

- `WorkConsumer.acceptWork(int rpm)` 返回的倍率（`return rpm >= 16 ? 2.0F : 1.0F`）**调用方全部忽略**（`PneumaticMotorBlock.Entity`、`KineticPneumaticMotorEntity.pushWork` 都不读返回值）；
- 真正的加速靠机器自己记 `workTicks = 20`，每 tick 若 `workTicks > 0` 就多推 1 点进度并自减；
- 而马达是**每 tick 都调** `acceptWork`，所以 `workTicks` 永远被刷回 20 → 只要马达在转，就是恒定 ×2；
- `findNeighbor` 只返回**第一个**消费者 → 一台马达只能带一台机器，无法并联分发；
- `PressureCookerBlockEntity.applyWorkBoost()` / `HighPressureFurnaceBlockEntity.applyWorkBoost()` **定义了但从未被调用**（死代码，与内联的 `workTicks` 逻辑重复）。

---

## 4. 温度与过热（`machine/MachineEnvironment` + `AbstractMachineBlockEntity`）

**基础温度**：金星地表（`northstar:venus`）430°C / 云海维度 30°C / 其他 20°C。
**热源**（半径 2，最多统计 8 个）：熔岩 600、岩浆块 300、火或灵魂火 300、营火 200。
**冷源**（半径 ±4 / ±2，最多 8 个）：水、冰、浮冰、蓝冰、雪块各 −40；有冷却剂的散热器再各 −40。
**过热**：环境温度 > 400°C 且机器在工作 → 累计 1200t（60s）停机 → 冒黑烟 + 钟声；温度回落后 200t 恢复。全部可配置（`PhosphorosConfig.machines`）。
**旁路**：Northstar 温度调节器 16×6×16 范围内直接重置过热计时（`MachineEnvBridge.isCooledByRegulator`）。

**两个要修的点**：

1. **P0 · 性能**：`tickOverheat` 里**每 tick** 都调 `isCooledByRegulator`，而它一旦命中 Northstar 就会扫 `33×13×33 = 14,157` 个方块（`NorthstarMachineBridge.HORIZONTAL=16 / VERTICAL=6`）。没装 Northstar 时反射直接返回 false（无成本），装了就是每台机器每 tick 1.4 万次 `getBlockState` + 一次反射调用。**应改为 20 或 40 tick 一次并缓存**。
2. **P2 · 逻辑**：`AcidPumpBlock.Entity.isWorking() = immersed`，而酸泵在金星地表 430°C 的酸里工作 → 60 秒后过热停机 10 秒，循环往复。酸泵不吃气压也不产热，这条过热对它没有游戏意义。

---

## 5. 配方与产物流

### 5.1 高压熔炼（`machine/HighPressureRecipe`，类型 `northstar_planet_addon:high_pressure`）

无序多材料：`ingredients` 列表里每个 ingredient 匹配一件**不重复**的原料即可开工；`bar` / `temperature` / `time` 可逐条覆盖（默认 8 / 1200 / 400）。

**全库只有 3 条**：

| 产物 | 材料 | bar | °C | time | 用途 |
|---|---|---|---|---|---|
| `acid_proof_alloy_plate` 耐酸合金板 | 2 钨板 + 1 硫晶 + 1 金锭 | 8 | 1200 | 200 | **无任何配方消费**（仅作为箱子战利品） |
| `pressure_stew` 压力炖 | 熟牛肉 + 烤巨型真菌 + 云藻胶 + 酸囊 | 8 | 300 | 200 | 食物，也是下一条的原料 |
| `phosphoros_feast` 启明星宴 | 压力炖 + 酸菜 + 霜莓冰沙 + 雷蜜饮 | 8 | 1200 | 400 | 食物（终局料理） |

> 3 条里 2 条是食物，唯一的"工业产物"没有下游。这是这条线**最像"没做完"**的地方。

### 5.2 材料链条现状

```
结构箱子（11 张表）
  ├── tungsten_plate ──→ compressor / pressure_tank / pressure_cooker / high_pressure_furnace
  │                      cloud_lift / condenser / pneumatic_motor / barometer / 工具武器
  ├── pre_civilization_alloy ──→ 上述机器配方里的"文明遗物"件
  └── acid_proof_alloy_plate（死胡同）

tungsten_block ─(合成)→ 9 tungsten_ingot ─(合成)→ 4 张成 1 tungsten_plate
      ↑                                                    （现有唯一来源是箱子直接给板）
  无来源（无矿、无战利品、无掉落）
```

**结论**：`tungsten_ingot` 与 `tungsten_block` **只能靠创造模式或 `/give`**；主获取途径是"结构箱子直接给板"。所以工业线能启动，但"挖矿→冶炼→制板"的中段是空的。

### 5.3 机器掉落与战利品

11 张箱子表（`cloud_harbor`、`acid_pump_station`、`observation_tower`、`crashed_airship`、`fungal_nursery`、`lava_tube_city`、`phosphoros_altar` + 4 张 `_calibrator` / `_core` / `_boss` 变体）覆盖 8 座结构；机器方块 10/10 有自掉落表。

---

## 6. 兼容层（`compat/`，三层隔离）

| 层 | 类 | 状态 |
|---|---|---|
| Create 反射 | `compat/create/CreateReflect` | ✅ 按 `ModList.isLoaded("create")` 解析一次，失败只 warn |
| Create 桥 | `compat/create/CreateMachineBridge` | ✅ 隔离类，仅命中后被类加载 |
| Create BE | `compat/create/KineticPneumaticMotorEntity extends KineticBlockEntity` | ⚠️ **只声明了应力**（`calculateStressApplied() = 4.0`），没有 `IRotate`、没有 visual/renderer、没有 goggle tooltip |
| 条件注册 | `registry/ModBlockEntities.PNEUMATIC_MOTOR` | ✅ 有 Create → 换成 kinetic BE；否则 `PneumaticMotorBlock.Entity` |
| 主包桥 | `compat/MachineTickerBridge` | ✅ 无第三方 import |
| Northstar 反射 | `compat/northstar/NorthstarReflect` + `NorthstarMachineBridge` | ✅ 调节器检测 + 环境温度（供气压计） |
| TaCZ | `compat/TaczCompat` | ✅ 只打印 `#northstar_planet_addon:ballistic` 标签条数，不 import 任何 TaCZ 类 |

**Create 联动的确切缺口**（`KineticPneumaticMotorEntity`）：

1. 方块 `PneumaticMotorBlock` 继承 `AbstractMachineBlock`（普通 `Block`），**没有实现 `IRotate`**。按 Create 的传播契约（本项目自己的笔记 `docs/CREATE-KINETIC-TRANSMISSION.md` §2.1 也写了："动力方块必须实现 `IRotate`，否则不参与传播"），`hasShaftTowards` / `getRotationAxis` 缺失 → 相邻传动轴不会给它转速 → `getSpeed()` 恒 0 → `pushWork()` 第一行 `if (rpm <= 0) return;` 直接退出。
   **最小修法**：让 `PneumaticMotorBlock` 实现 `IRotate`（或让 Create 分支改用 `RotatedPillarKineticBlock`），`getRotationAxis` 返回 `AXIS`，`hasShaftTowards` 返回 `face.getAxis() == AXIS`。
2. `calculateStressApplied()` 硬编码 4.0，未走 `BlockStressValues.IMPACTS`；也没有 `getStressConfigKey()` 覆写，玩家无法用数据包调。
3. 没有 `KineticBlockEntityVisual` / `KineticBlockEntityRenderer`，客户端就是静止方块（Create 的轴/齿轮会转，它不转）。
4. 没有 Ponder 场景、没有 goggle 信息（`IHaveGoggleInformation` 未实现）；整套 `venus` 里 `ponder|jei|Jade|TOP|goggle` 关键词**零命中**。

---

## 7. 代码质量与文档漂移

| 项 | 现状 | 影响 |
|---|---|---|
| `ModBlocks` 类注释 | 仍写"压力机器属于 M3 里程碑，扩展点见 compat/create" | 与事实相反（机器已全落地） |
| `AbstractMachineBlock` 类注释 | "M3 阶段不必先做 Menu/Screen" | 但 `MachineMenu`/`MachineScreen` 已有 |
| `applyWorkBoost()` ×2 | 死代码，从未调用 | 误导读代码的人以为加速有第二处入口 |
| `FungusBedBlock` 注释 | "物品槽暴露为 `IItemHandler`，漏斗可抽走" | `ModCapabilities` 只注册了锅和炉 → **漏斗抽不走菌床产物** |
| `CondenserBlock` 产物 | 同样有 `ItemStackHandler` 但未注册 capability | 只能手收 |
| `HighPressureFurnaceBlockEntity` | 每 tick 重新 `findRecipe()`；`progress` 在换配方时**不重置** | 玩家把原料换成别的配方，旧进度会被带到新配方上（可利用/可卡） |
| 调试命令 | 只有 `/northstar_planet_addon debug boss\|dps\|classify` | 机器侧无任何调试入口（给气、看温度、强制过热都没命令） |

---

## 8. 优先级建议（按"投入产出比"排序）

| # | 动作 | 工作量 | 收益 |
|---|---|---|---|
| 1 | `PneumaticMotorBlock implements IRotate`（`getRotationAxis`=AXIS、`hasShaftTowards`=同轴面） | 小 | 让 Create 联动真的成立，这是整条线对外宣传的卖点 |
| 2 | 钨链补源：给金星/火星加钨矿，或让某台机器/结构稳定产 `tungsten_ingot` | 小 | 补上"挖→炼→板"的中段 |
| 3 | 给 `acid_proof_alloy_plate` 找下游：盾、护甲片、酸泵升级、云港修复件（结构里已有"投入耐酸合金板换酸泵"的玩法占位） | 中 | 让高压熔炉有存在理由 |
| 4 | 调节器扫描改 20t 一次 + 缓存结果 | 小 | 消除 P0 性能雷 |
| 5 | 补 3–5 条高压熔炼配方（钨板压制、硫晶提纯、液氧、合金、结构修复件） | 小 | 让配方类型不止"做饭" |
| 6 | 菌床/冷凝塔注册 `Capabilities.ItemHandler` | 极小 | 兑现注释里的承诺，让自动化接得上 |
| 7 | 高压熔炉：换料即重置 `progress`；或用"锁定一次配方到完成" | 小 | 堵掉卡进度的利用 |
| 8 | `KineticBlockEntityRenderer` + `BlockStressValues.IMPACTS` 注册 | 中 | Create 玩家的手感与可调性 |

---

## 9. 文件地图（要动手时按这张表找）

| 关注点 | 文件 |
|---|---|
| 气动储量 / 换算 | `venus/machine/PneumaticStorage.java` |
| 机器基类（推拉气、过热、存盘） | `venus/blockentity/AbstractMachineBlockEntity.java` |
| 方块基类（ticker、掉落内容物） | `venus/block/AbstractMachineBlock.java` |
| 温度 / 热源 / 冷源 | `venus/machine/MachineEnvironment.java` |
| 机械功接口 | `venus/machine/WorkConsumer.java` |
| 高压熔炼配方类型 | `venus/machine/HighPressureRecipe.java`、`ModRecipes.java` |
| 注册 | `venus/registry/ModBlocks.java`、`ModBlockEntities.java`、`ModCapabilities.java` |
| 数值配置 | `venus/config/PhosphorosConfig.java`（`machines` / `balance` 两节） |
| Create 联动 | `venus/compat/create/{CreateReflect,CreateMachineBridge,KineticPneumaticMotorEntity}.java` |
| Northstar 联动 | `venus/compat/northstar/{NorthstarReflect,NorthstarMachineBridge}.java` |
| 配方数据 | `data/northstar_planet_addon/recipe/*.json`（87 个，其中 3 个 `high_pressure`） |
| UI | `venus/menu/{MachineMenu,MachineContainer}.java`、`venus/client/screen/MachineScreen.java` |

---

## 10. 本次核实过的边界（避免误读）

- 构建状态：**通过**（增量 17s）。本文没有在真实服务器跑过机器行为，数值全部来自代码常量；"能不能玩得顺"仍需实机。
- Create 联动"不成立"的判断依据是 Create 的传播契约（`IRotate` 必需）+ 本项目自己的研究笔记，**没有在装有 Create 的客户端实测**。若要下定论，装 Create 6 放一根轴接马达看 `getSpeed()` 是否非 0 即可。
- 战利品/配方/掉落表的统计来自 `data/` 下 JSON 文件实点，不是 README 转述。
