# 装备重做记录（模型 + 贴图）

本次把工程里全部「装备类」资产（武器 / 工具 / 五套盔甲）重做了一遍：
7 件武器与器械换成带真实长度和装饰件的 3D 模型（`weapon3d.mjs` + `tools/gen-tool3d.mjs` 生成，≥35 element），
其余工具图标重画成 16px 手绘图标，盔甲五套除了 4 件图标 + `layer_1`/`layer_2` 穿戴层，
还各配了 128×128 的 `_armor.png` 与 `HumanoidModel` 子类的 3D 穿戴模型（装饰层单独一次绘制）。

配色一律走 `northstar-art-style` 的实测调色板：金属只用冷灰阶，
强调色（蓝 / 琥珀 / 硫黄 / 余烬）面积控制在 10% 以内。

---

## 1. 武器与器械：3D 模型 + 64x64 图集

| 物品 | kind | 几何长度 | element | 图集 | 配色（暗→亮） |
|---|---|---|---|---|---|
| `molten_core_sword` 熔核巨剑 | sword（greatsword 预设） | 38 u ≈ 2.38 格 | 42 | `textures/item/molten_core_sword.png` 64x64 | 黑曜石 `#150d0c`→`#2a1512`→`#5e2a17`，余烬刃心 `#ff7a3c`，刃口 `#ffd9a0` |
| `starcore_cannon` 星核炮 | staff（sword 长度） | 31.4 u ≈ 1.96 格 | 37 | `textures/item/starcore_cannon.png` | 火星钢杆 `#373e3a`，铜箍 `#c9a227`，星核 `#ffb03a` + 白热芯 `#ffe9b0` |
| `acidic_spear` 酸蚀长枪 | spear | 56 u ≈ 3.5 格 | 51 | `textures/item/acidic_spear.png` | 钨钢枪头 `#3f464b`→`#eaf3f6`，酸液刃口 `#7dff4a`，绿宝石 `#56c02a` |
| `pressure_hammer` 压力战锤 | hammer | 34.8 u ≈ 2.17 格 | 37 | `textures/item/pressure_hammer.png` | 钨锤头 `#2e2e2e`→`#717171`，压力表芯 `#48c8c0` + `#bfffff` |
| `moon_deer_axe` 梦眠月斧 | axe | 32.2 u ≈ 2.01 格 | 36 | `textures/item/moon_deer_axe.png` | 月银 `#2a2740`→`#f2f5ff`，月蓝宝石 `#8f8bff` |
| `thunder_crossbow` 雷霆弩 | **crossbow**（自建形制） | 34 u ≈ 2.13 格 | 44 | `textures/item/thunder_crossbow.png` | 钢 `#2b3038`→`#c2ccd4`，钢饰件 `#aeb9c2`，雷光宝石 `#8fd8ff` + `#eaf8ff` |
| `pressure_drill` 高压钻 | **drill**（自建形制） | 34 u ≈ 2.13 格 | 42 | `textures/item/pressure_drill.png` | 钢 `#3a4046`→`#d5dde2`，黄铜箍 `#b08a3a`，压力表 `#48c8c0` + `#d4fffb` |

- 前五把走官方生成器（spec 在 `tools/equip-specs/<name>.json`）：
  `node <skills>/weapon-armor-art/scripts/weapon3d.mjs tools/equip-specs/<name>.json --out .`
- 弩 / 钻不在 `weapon3d.mjs` 的 11 种 kind 里（硬套会做出「带弩臂的剑」），
  由 `tools/gen-tool3d.mjs` 产出同规格模型：
  `node tools/gen-tool3d.mjs tools/equip-specs/<name>.json --out .`
  - 弩：托肩 + 三段弩身 + 弩臂座 + 双段弩臂×2 + 臂尖/臂箍 + 两段弓弦 + 三段弩矢 + 箭头 + 双尾羽 + 扳机护圈 + 手枪握把 + 三圈缠绳 + 瞄具 + 双宝石 + 四铆钉 + 两道箍 + 三件背带。
  - 钻：托底 + 手枪握把 + 三圈缠绳 + 扳机 + 三段枪体 + 三道箍 + 侧面压力表 + 气喉接口 + 卡盘 + 四螺栓 + 四段钻头 + 钻尖 + 两道钻头环 + 四段螺旋排屑槽 + 三片散热窗 + 四铆钉。
  - 生成器内置 `MIN_PARTS = 35` 硬校验，并把 `display.gui` 一起算好。
- 七个模型都是 `elements` 3D 模型（无 parent），`texture_size:[64,64]`，`gui_light:front`，
  display 用第三方 `big_handheld` 数值（1.75 缩放），另有地面 / 展示台 / 头盔位与 `gui` 变换。
- `weapon3d.mjs` 不写 `gui` 变换，3D 模型在物品栏会溢出格子，因此前五把用
  `tools/patch-gui-display.mjs` 按包围盒补了 `display.gui`（缩放到 15/16 格并居中）。
- 每个部件的 UV 区域对照表见 `docs/<name>-atlas.md`（blade 0,0 16x48 / guard 16,0 16x8 /
  wrap 16,8 16x16 / pommel 16,24 16x8 / gem 16,32 8x8 / tassel 24,32 8x16 / accent 32,0 32x16 /
  flat 32,16 16x16）——七张图集共用同一套区域，画法可以互抄。

## 2. 普通工具：16px 图标重画

| 物品 | 造型 | 主要颜色 |
|---|---|---|
| `sulfur_flamethrower` 硫火喷射手 | 硫罐 + 枪管 + 枪口火焰 | 钢 `#8a9199`，火 `#ff9a3c` |
| `cloud_hook` 云钩 | 云白 J 形钩 + 长柄 | `#cfd8e6` / `#8a9199` |
| `fungus_shears` 菌剪 | X 形双刃 + 硫黄轴心 | `#8a9199`，轴 `#e8d97a` |
| `sandstorm_goggles` 沙暴护目镜 | 双镜片 + 黄铜框 + 皮质绑带 | 框 `#b08a3a`，镜 `#ffb648`，带 `#30230f` |

> `pressure_drill` 与 `thunder_crossbow` 已升级为 3D 模型（见第 1 节），
> 它们原来的 16px 图标被同名的 64x64 图集覆盖。

## 3. 五套盔甲

每套 = 4 张 16x16 图标（`textures/item/<set>_{helmet,chestplate,leggings,boots}.png`）
+ 2 张 64x32 穿戴层（`textures/models/armor/<set>_layer_{1,2}.png`）。
`layer_1` 是贴身甲面（脸和手留空，玩家皮肤可见），`layer_2` 只画外层装饰件：
头冠 / 眉饰 / 肩甲 / 腰带 / 腕箍 / 膝板 / 靴口。

| 套装 | 甲面（暗→亮） | 强调色 | 身份特征 |
|---|---|---|---|
| `tungsten` 钨钢压力 | `#2e2e2e` / `#3f3f3f` / `#5c5c5c` | `#ffb648` 琥珀 | 眉灯 + 胸前压力条 |
| `cloud_walker` 云行者 | `#364050` / `#55627a` / `#94a2ba` | `#4a46a8` 月蓝宝石 | 蓝饰边，最亮的一套 |
| `tube_heavy` 熔管重甲 | `#2a211c` / `#3d2d23` / `#5f4630` | `#ff6a1e` 岩余烬 | 更宽的盔体 + 顶冠 |
| `phosphoros` 启明星 | `#2b2617` / `#3f3823` / `#5e5533` | `#e8d97a` 硫黄辉光 | 胸前光盘 `#fff6c0` |
| `molten_core` 熔核 | `#2a1512` / `#4a1f16` / `#7a2f1a` | `#ff7a3c` 熔岩 | 额头 / 胸口熔核亮点 |

模板（便于以后再换配色）：
`tools/equip-specs/templates/armor_layer1.png`、`armor_layer2.png`。
换色方法：`load` 模板 → `replace` 四个甲面色 + 强调色 → `save`。

## 4. 3D 穿戴模型 + 128×128 `_armor.png`

五套盔甲各有一个 `HumanoidModel<LivingEntity>` 子类，画出 layer 贴图做不出来的立体件
（17 个部件 / 其中 8 个纯装饰，实测 12 个真实盔甲模型平均 9.5 部件 / 3.3 装饰，本套明显高于平均线）：

| 套装 | 模型类 | 图集 | UV 对照表 |
|---|---|---|---|
| `tungsten` | `venus/client/model/TungstenArmorModel.java` | `textures/models/armor/tungsten_armor.png` | `docs/tungsten-armor.md` |
| `cloud_walker` | `venus/client/model/CloudWalkerArmorModel.java` | `cloud_walker_armor.png` | `docs/cloud_walker-armor.md` |
| `tube_heavy` | `venus/client/model/TubeHeavyArmorModel.java` | `tube_heavy_armor.png` | `docs/tube_heavy-armor.md` |
| `phosphoros` | `venus/client/model/PhosphorosArmorModel.java` | `phosphoros_armor.png` | `docs/phosphoros-armor.md` |
| `molten_core` | `mars/client/model/MoltenCoreArmorModel.java` | `molten_core_armor.png` | `docs/molten_core-armor.md` |

部件清单（每套相同，挂在对应骨头上，跟着转头和摆臂动）：
头盔 = 冠 3 件 + 双角；胸甲 = 护胸 + 核心宝石 + 双肩甲 + 披风 + 腰带；
护臂 ×2（`right_arm`/`left_arm`）；腰甲 ×2 + 胫甲 ×2（`right_leg`/`left_leg`）。

**为什么甲面不画在 3D 模型里**：`renderToBuffer` 只渲染这些装饰件，基础的头/身/臂/腿
仍然由 `_layer_1/2.png` 负责，所以戴头盔能看见脸、穿胸甲能看见手——这是
「HumanoidModel 子类把整套身体重画一遍」最容易踩的坑。

**渲染链路**（`client/render/ArmorDecoLayer.java` + `client/ArmorDecoClient.java`）：

1. `EntityRenderersEvent.RegisterLayerDefinitions` 注册五个模型层
   `northstar_planet_addon:<set>_armor`；
2. `EntityRenderersEvent.AddLayers` 里 `bakeLayer` 出模型实例，把每套 4 件物品全部登记到
   `ArmorDecoLayer`（同一个模型实例），再给 `default` / `slim` 两个玩家渲染器挂上该层；
3. 每帧渲染时按 `HEAD/CHEST/LEGS/FEET` 查物品 → `setSlot(HELMET/CHESTPLATE/LEGGINGS/BOOTS)`
   → `copyPropertiesTo` 复制玩家骨骼姿态 → 用 `RenderType.armorCutoutNoCull(<set>_armor.png)`
   画装饰件。原版 `HumanoidArmorLayer` 那次绘制不受影响，甲面照旧。

图集配色（只改甲片色 + 强调色，装饰件色阶沿用生成器的角色划分）：

| 套装 | 甲片 / 腰甲 | 冠 | 冠中脊 | 角 | 宝石 | 披风 | 腰带 |
|---|---|---|---|---|---|---|---|
| tungsten | `#3f3f3f` / `#2e2e2e` | `#5c5c5c` | `#717171` | `#9aa0a6` | `#ffb648` | `#2a2d31` | `#1a1a1a` |
| cloud_walker | `#55627a` / `#3f4a5e` | `#6e7d98` | `#94a2ba` | `#cfd8e6` | `#4a46a8` | `#333a4d` | `#23262f` |
| tube_heavy | `#3d2d23` / `#2a211c` | `#57402f` | `#6b4f38` | `#8a5a34` | `#ff6a1e` | `#241a15` | `#17110e` |
| phosphoros | `#3f3823` / `#2b2617` | `#574d2c` | `#6f6438` | `#8f8548` | `#e8d97a` | `#2a2416` | `#1a160e` |
| molten_core | `#4a1f16` / `#2a1512` | `#6a2a18` | `#8a3418` | `#c25a22` | `#ff7a3c` | `#33100c` | `#1c0f0c` |

**重新生成 / 改形制的流程**（两步，别手改生成的 Java）：

```bash
node <skills>/weapon-armor-art/scripts/armor3d.mjs tools/equip-specs/armor3d-<set>.json --out .
node tools/patch-armor-model.mjs src/main/java/**/<Set>ArmorModel.java
```

`patch-armor-model.mjs` 补两件生成器没做的事：`implements ArmorDecoModel`（渲染层要靠统一
类型调 `setSlot`），以及给 `addBox(...)` / `PartPose.offset(...)` 的 double 字面量补 `F`——
1.21.1 的这些形参是 `float`，生成器写的是 `-2.2`，不补会 12 处/模型编译报错。

## 5. 校验结果

- **`gradlew build` 通过**：`BUILD SUCCESSFUL`。本机 Gradle 缓存里只有
  `neoforge:21.1.249`（工程 pin 的 21.1.248 拉不到，`maven.neoforged.net` 不可达），
  所以验证命令是 `gradlew build "-Pneo_version=21.1.249" --offline`
  ——只是命令行覆盖属性，`gradle.properties` 没动。
- `check-assets.mjs`：85 注册物品 / 92 贴图 / 144 模型，**errors 0，warnings 0**。
- `model.mjs lint --expect item`：七个 3D 装备模型全部 `pass: true`
  （42 / 37 / 51 / 37 / 36 / 44 / 42 elements，均 ≥ 35 下限）。
- 尺寸复核：7 张装备图集 64x64；5 张盔甲模型图集 128x128；10 张穿戴层 64x32；20 张图标 16x16。
