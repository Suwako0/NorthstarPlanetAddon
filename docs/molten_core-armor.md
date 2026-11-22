# molten_core 盔甲（34 部件）

对着 `docs/previews/molten_core_armor-reference.jpg` 重做：五尖冠头盔 + 发光眼缝、
塔式肩甲 + 上翘尖、胸口**六边形熔核徽记**、铜腰带 + 方扣、链饰、膝甲、赤热靴底。

- 装饰图集：`textures/models/armor/molten_core_armor.png`（128×128，7 个色区）
- 穿戴层：`molten_core_layer_1.png`（头/胸/臂/靴）、`molten_core_layer_2.png`（护腿）
- 图标：`textures/item/molten_core_{helmet,chestplate,leggings,boots}.png`（16×16）
- 模型：`mars/client/model/MoltenCoreArmorModel.java`（脚本生成，勿手改）

## 图集分区（uv 0–16，1 单位 = 8 px）

| 区域 | uv | 像素 | 用途 |
|---|---|---|---|
| plate | 0,0–8,8 | (0,0)–(64,64) | 主甲面：深炭灰 + 受光边 + 铆钉 |
| trim | 8,0–12,8 | (64,0)–(96,64) | 黄铜：腰饰、方扣、领甲边 |
| gem | 12,0–16,4 | (96,0)–(128,32) | 六边形熔核徽记：铜环 + 凹槽 + 切面宝石 |
| steel | 12,4–16,8 | (96,32)–(128,64) | 亮一档的钢：护腕、膝甲、战靴 |
| glow | 0,8–6,16 | (0,64)–(48,128) | 纯发光：眼缝、靴底 |
| chain | 6,8–9,16 | (48,64)–(72,128) | 链节 |
| dark | 9,8–12,16 | (72,64)–(96,128) | 深缝 / 内衬 |

## 部件表

| 部件 | 父骨 | box | UV | 分区 |
|---|---|---|---|---|
| horn_mid_a | head | -1.2,-13,-1.2 → 1.2,-8,1.2 | 0,0 | plate |
| horn_mid_b | head | -0.7,-16.5,-0.7 → 0.7,-13,0.7 | 0,0 | plate |
| horn_side_l_a | head | -4.4,-12,-1.0 → -2.2,-8,1.0 | 0,0 | plate |
| horn_side_l_b | head | -4.9,-14.2,-0.8 → -3.2,-12,0.8 | 0,0 | plate |
| horn_side_r_a | head | 2.2,-12,-1.0 → 4.4,-8,1.0 | 0,0 | plate |
| horn_side_r_b | head | 3.2,-14.2,-0.8 → 4.9,-12,0.8 | 0,0 | plate |
| brow | head | -4.6,-8.8,-4.7 → 4.6,-7.4,-3.4 | 0,0 | plate |
| eye_slit | head | -3.2,-6.9,-4.55 → 3.2,-5.9,-4.3 | 0,64 | glow |
| pauldron_l | body | -10.6,-3.4,-3.4 → -4.2,2.8,3.4 | 0,0 | plate |
| pauldron_l_tip | body | -11.4,-6.2,-2.4 → -9.0,-3.4,2.4 | 0,0 | plate |
| pauldron_r | body | 4.2,-3.4,-3.4 → 10.6,2.8,3.4 | 0,0 | plate |
| pauldron_r_tip | body | 9.0,-6.2,-2.4 → 11.4,-3.4,2.4 | 0,0 | plate |
| core_emblem | body | -2.3,2.7,-3.02 → 2.3,6.9,-2.9 | 96,0 | gem |
| belt | body | -4.5,9.0,-2.6 → 4.5,11.0,2.6 | 0,0 | plate |
| buckle | body | -1.2,9.2,-2.85 → 1.2,10.8,-2.6 | 64,0 | trim |
| skirt | body | -5.2,10.8,-2.7 → 5.2,13.8,2.7 | 0,0 | plate |
| chest_side_l | body | -6.0,1.5,-2.6 → -4.0,9.0,2.6 | 0,0 | plate |
| chest_side_r | body | 4.0,1.5,-2.6 → 6.0,9.0,2.6 | 0,0 | plate |
| back_plate | body | -4.2,0.4,2.0 → 4.2,8.6,2.9 | 0,0 | plate |
| collar | body | -4.6,-2.6,2.1 → 4.6,0.6,3.1 | 0,0 | plate |
| chain_l_a | body | -10.0,2.4,-3.0 → -9.4,4.4,-2.4 | 48,64 | chain |
| chain_l_b | body | -10.0,4.4,-3.0 → -9.4,6.4,-2.4 | 48,64 | chain |
| chain_r_a | body | 9.4,2.4,-3.0 → 10.0,4.4,-2.4 | 48,64 | chain |
| chain_r_b | body | 9.4,4.4,-3.0 → 10.0,6.4,-2.4 | 48,64 | chain |
| cuff_r | right_arm | -3.4,2.0,-2.6 → 1.4,8.0,2.6 | 96,32 | steel |
| cuff_r_trim | right_arm | -3.5,7.4,-2.7 → 1.5,8.2,2.7 | 64,0 | trim |
| cuff_l | left_arm | -1.4,2.0,-2.6 → 3.4,8.0,2.6 | 96,32 | steel |
| cuff_l_trim | left_arm | -1.5,7.4,-2.7 → 3.5,8.2,2.7 | 64,0 | trim |
| knee_r | right_leg | -2.7,2.4,-2.9 → 2.7,4.6,-1.3 | 96,32 | steel |
| boot_r | right_leg | -2.5,9.4,-2.7 → 2.5,12.4,2.7 | 96,32 | steel |
| sole_r | right_leg | -2.2,12.3,-2.6 → 2.2,12.9,2.6 | 0,64 | glow |
| knee_l | left_leg | -2.7,2.4,-2.9 → 2.7,4.6,-1.3 | 96,32 | steel |
| boot_l | left_leg | -2.5,9.4,-2.7 → 2.5,12.4,2.7 | 96,32 | steel |
| sole_l | left_leg | -2.2,12.3,-2.6 → 2.2,12.9,2.6 | 0,64 | glow |

## 胸口徽记的两次修正

1. 第一版是「黄铜方框 + 六边形宝石」，方形与六边形两套几何打架，且方框是全套最亮的大色块 → 很突兀。
2. 现在合并成**单一六边形徽记**：贴图里画铜环（暗→亮两阶）+ 1 px 凹槽阴影 + 三切面宝石 + 白热芯，
   几何上只有一个 `core_emblem`，没有方框；靠凹槽阴影让它「嵌进」胸甲而不是贴上去。

## 复现

```bash
python tools/gen-molten-core-armor.py            # 模型 + 图集 + 两层 + 4 图标 + 预览
python tools/gen-molten-core-armor.py --ascii    # 额外打印正面 ASCII
```

## 本次修正（脸 / 手 / 配色）

| 问题 | 处理 |
|---|---|
| 脸部被面甲完全盖住 | `layer_1` 的 `(8,9)-(16,15)` **重新挖空**，脸正视可见；只保留上下 1 px 铜边与眉带 |
| 眼缝悬在脸上 | 删掉横贯面部的 `eye_slit`，改成**两侧太阳穴的发光排气口** `vent_l/vent_r` |
| 手部多余黑方块 | 护腕 `cuff_*` 原本到 y=8（正好压在手腕/手背），缩到 y=1–6.4，只包前臂；右手窗口仍全空 |
| 颜色太单一 | 甲面从 1 档灰加到 3 档（`#3B3637` / `#504B4C` / `#262223`），加胸甲分片、肩缝、铜腰线、铜护腕箍、膝甲亮片；色数 7 → 8+，装饰图集同步提亮 |

脸与手现在都是透的，穿戴后能看见皮肤。
## 建模流程（Blockbench MCP → Java）

本轮改为在 Blockbench 里建模，再脚本转成游戏内模型：

1. docs/models/molten_core_armor.bbmodel —— 设计源文件（Generic Model，6 根人形骨骼，76 个 cube，
   含 1 个只用于预览的 skin_head）。
2. python tools/bbmodel-to-java-parts.py —— 读 bbmodel，把世界坐标（脚底 y=0、y 向上）
   换算成 HumanoidModel 空间（脖子 y=0、y 向下），按名字前缀分材质区，写出 PARTS。
3. python tools/gen-molten-core-armor.py —— 生成 Java 模型 + 图集 + 两层 + 图标 + 预览。

Blockbench 里的圆柱（熔核环）由旋转的条带拼成，而 Java 装饰模型只支持轴对齐方块，
所以转换时跳过那些条带，改成两个平面件（core_frame / core_gem）重建熔核。

部件：**65 个**（bbmodel 76 减去 12 条圆柱带和 1 个参考头）。
## 槽位归属修正（本轮）

之前的 setSlot 是**按骨骼**开关的：showBody 在“护腿”槽也为真，于是穿护腿时把肩甲、胸甲、熔核、链饰全画了出来——
这就是截图里“只穿护腿却像穿了胸甲”的原因。

现在改为**按部件所属槽位**逐件判断（slot_of()）：

| 槽位 | 包含部件 |
|---|---|
| 头盔 | 盔顶、盔侧、眉甲、颊甲、下颌、冠刺、护背 |
| 胸甲 | 胸板、领甲、背板、肩甲、肩刺、链饰、熔核框/宝石、腰带、方扣、护臂、护腕 |
| 护腿 | 大腿板、膝甲、膝尖、胫甲、腰垂 |
| 靴子 | 靴体、铜箍 |

另外删掉了两只 plate_gauntlet（手背上的黑方块），手现在一定是露出来的。
甲面整体提亮一档（#3B3637 → #4A4546），不再是纯黑。

预览脚本新增 --only chest|legs|boots|head，可单独检查每一件：
```
python tools/gen-molten-core-armor.py --only chest
```

## 为什么之前「Blockbench 里和游戏里差那么多」

差在**贴图**，不在几何。

- Blockbench 里那张 `molten_core` 贴图是 `detail_cubes` 画的：AO、倒角高光、金属划痕全烘在像素里。
- 游戏里原先用的是另外按「材质分区」画的 128 图集（每种材质一块纯色区），
  几何搬过去了，UV 和贴图没搬 —— 游戏里就成了一堆纯色方块。

## 现在的管线（唯一来源 = bbmodel）

```bash
# 1. Blockbench 里建模 + 上色（box UV），保存到 docs/models/molten_core_armor.bbmodel
# 2. 导出 bbmodel 自带的贴图 + 每个 cube 的 box-UV 偏移
python tools/bbmodel-export.py
# 3. 生成 Java / 穿戴层 / 图标 / 预览
python tools/gen-molten-core-armor.py
```

1. **必须用 box UV**（`pack_uv mode=box`），Java 的 `CubeListBuilder.texOffs(u,v)` 只支持盒式 UV。
2. 导出脚本把 bbmodel 里那张 PNG **原样**写到 `assets/.../molten_core_armor.png`，
   并把每个 cube 的 `uv_offset` 写进 PARTS；生成脚本只负责几何与 `texOffs`，**不再自己画图集**。
3. 熔核环改成两个平面方块放进 bbmodel（12 边圆柱是旋转条带，Java 装饰模型不支持旋转）。
