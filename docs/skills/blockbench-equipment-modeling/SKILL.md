---
name: blockbench-equipment-modeling
description: 用 Blockbench MCP 制作 Minecraft 装备 3D 模型（护甲、武器、工具、饰品、装备件）的流水线：box UV、整数网格、外扩必须在打包前、共面与对称、贴图与几何同源、导出到 Java/JSON、display 持握姿态、装备动画、校验与 GameTest 验收；含护甲分层（脸/手/断面窗口）与武器握把对齐的专项规则。
whenToUse: 只用于装备类 3D 模型——护甲装饰件与穿戴层、武器/工具、饰品、装备件；或出现"装备在游戏里与 Blockbench 不一样、手上黑方块、穿模闪烁、盔甲臃肿、左右不对称、UV 拉伸、持握位置不对"时。生物/Boss、方块、机械结构不要套用本技能。
---

# 装备建模流水线（护甲 / 武器 / 工具 / 饰品）

一句话：**几何与贴图的唯一来源是 `.bbmodel`，游戏内资源由脚本从它导出**。任何"手写坐标 + 另画一套贴图"的做法，最终都会得到两个互不相干的模型——装备尤其致命，因为它同时受模型、贴图、槽位、持握姿态四套东西约束。

## 0. 适用范围

| 属于本技能 | 不属于 |
|---|---|
| 护甲（穿戴层 + 3D 装饰件） | 生物 / Boss / 载具 |
| 武器、工具、法杖、盾 | 方块、机械、多方块结构 |
| 饰品、装备件、背包外挂件 | 纯 2D 图标 / GUI（走贴图技能） |

生物与方块各有自己的骨架与体积规则，套用装备规则会得到"比例正确但结构错误"的模型。

## 1. 十条铁律（每条都对应一次返工）

1. **几何只来自 Blockbench**。不手写 `from/to`、不手算 UV。改形制 = 改 `.bbmodel`，再重跑导出脚本。
2. **贴图存进 bbmodel**（`detail_cubes` / `paint_faces`）。导出时把同一张 PNG 写进 `assets/`；另画一套"游戏用图集"必然导致 Blockbench 与游戏内两副面孔。
3. **box UV 强制**（`pack_uv {mode:"box"}`）。Java 的 `elements` 和 `CubeListBuilder.texOffs` 只认盒式 UV；逐面 UV 无法转换。
4. **整数网格**：坐标与尺寸对齐 1 单位，每条边 ≥1。小于 1 的薄片会被打包成 0 像素宽的面 → 游戏里是黑面（`check_model` 报 `zero_uv`）。
5. **外扩（clearance）必须在 `pack_uv` 之前烘进几何**。打包后再改尺寸，UV 岛与盒子错位，面会采到透明像素变黑。
6. **共面即闪**。相邻件重叠 1 单位、或错开 ≥0.2；`coplanar_overlap` 必须清零，`fix_z_fighting` 后把 `inflate` **烘进导出坐标**（Java 装备模型没有 inflate 字段）。
7. **对称只做一侧**，另一侧由导出脚本按骨骼中线镜像。左右各自取整会差 0.1~1.3 单位，肉眼可见。
8. **别用会倾印全部 cube 的工具**。一次塞几十个立方体的调用，光返回值就能吃掉整段上下文；批量建模/改动用 `execute_script`，只返回计数与异常。
9. **验收顺序固定**：`check_model` → 导出 → 装备校验脚本 → `gradlew build` → `runGameTestServer`。客户端模型与贴图**不热重载**，必须重启客户端。
10. **精细度下限**：至少三层形体（剪影 → 分件 → 小件：尖角/链/铆钉/边条），2~3 个明度色调 + 1 个强调色。部件数下限见 `references/qa-checklist.md`。

## 2. 标准流程

```
0. 需求/参考图 → 定类型（穿戴 / 手持）、尺寸、骨架、材质分区、导出格式
1. new_project（free / bedrock / geckolib，按导出目标）
2. 建骨架：穿戴用原版人形六骨；手持用单骨或双骨（柄 + 刃/头）
3. 形体：大块定剪影 → 中块分件 → 小件细节
4. pack_uv(box) → detail_cubes 按名字前缀上色
5. check_model → fix_z_fighting → 重新 pack_uv → 重新上色
6. save_project → docs/models/<name>.bbmodel
7. 脚本导出：贴图 + 每个 cube 的 uv_offset + 尺寸 → Java/JSON
8. 校验脚本 → build → GameTest → 截图（Blockbench 与游戏内各一张）
```

第 5 步不能省：改过几何就必须重打包并重新上色，否则 UV 岛与贴图对不上。

## 3. 装备特有的两件"动作"事

**A. 持握 / 穿戴姿态（display）** —— 比建模更容易出错：

- 物品模型的手在 `(8,8,8)`；**握把中心必须对齐到模型原点**，否则手会握在刃身中段或悬空。
- 长武器（剑/长柄/枪）必须用 `big_handheld` 一类的 display 预设；短刃用 `handheld`。
- 穿戴件没有 display，靠**骨骼绑定**：部件挂在 `head/body/arm/leg` 骨上，跟随转头、摆臂、潜行、骑乘。
- 姿态要么用原版预设，要么显式给 `rotation / translation / scale`，不要"看着差不多"。

**B. 装备动画** —— 装备也可能动，但只做这几类：

- 面甲 / 护目镜开合（单轴旋转 + 定格）
- 武器旋转、悬浮、拖尾（循环）
- 护甲部件摆动（裙甲、披风、链饰，跟随走路的次级运动）

要求：循环动作首尾同姿态；曲线用 `catmullrom`，打击/开合瞬间用 `linear`；导出前 `validate_animations`。手持姿态与动画的细节见 `references/display-and-animation.md`。

## 4. 故障对照表

| 现象 | 真正原因 | 处理 |
|---|---|---|
| 手/脚上有黑方块 | 分层贴图只挖了侧面，**没挖"断面"**（盒子底面那格） | `references/armor.md` 的 64×32 区域表 |
| 装备在游戏里看不见 | 被原版护甲层（皮肤 +1.0）包住 | 该件再外扩；或整套改纯 3D |
| 拖视角时闪 | 两件共面 | `fix_z_fighting` + 导出烘 inflate，或相邻件重叠 1 |
| 比 Blockbench 胖 | 外扩做了两遍（顶层 `deformation` + 自己的 clearance） | 只留一处 |
| 左右不一样 | 左右各自取整 | 导出只取一侧做镜像 |
| 只穿护腿却显示整套 | 槽位可见性按**骨骼**而不是按**部件**判断 | 按部件所属槽位开关 |
| 握把位置怪 / 手悬空 | 握把中心没对齐模型原点 | 见 `references/weapon.md` |
| 贴图拉伸/糊 | 尺寸不是整数像素 | 全部对齐整数、最小边 1 |
| 面是纯黑 | UV 采到透明像素（打包后改尺寸 / 薄件零面积） | 重打包 + 重新上色 |
| 改了没生效 | 客户端模型/贴图不热重载 | 重启客户端 |

## 5. 参考

- `references/armor.md` —— 穿戴件：原版层几何、脸/手/断面窗口、分层与纯 3D 的取舍、槽位。
- `references/weapon.md` —— 手持件：握把对齐、display 预设、2D 图标与 3D 模型的分工。
- `references/display-and-animation.md` —— display 变换与装备动画。
- `references/qa-checklist.md` —— 交付前逐项清单与命令。
