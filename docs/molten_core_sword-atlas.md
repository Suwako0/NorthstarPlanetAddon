# molten_core_sword · 熔核巨剑（3D 双手大剑 + 256×512 图集）

参考图：`docs/previews/molten_core_sword-reference.png`（黑曜石宽刃 + 铜色刃口 + 熔岩裂纹 + 三角熔核护手 + 缠绳握把 + 流苏）

- **唯一来源**：`docs/models/molten_core_sword.bbmodel`（几何 + 贴图都在里面，改形制只改它）
- 模型：`models/item/molten_core_sword.json` —— 48 个 element，`texture_size [256,512]`、`gui_light front`、8 组 display
- 贴图：`textures/item/molten_core_sword.png` —— 256×512，box UV，**8 px = 1 模型单位**（bbmodel 建在 8 倍尺度上，导出时 ÷8 并烘掉 `inflate`）
- 尺寸：长 **33.75** 单位（2.1 格，双手大剑档），护手尖距宽 9.5
- 握把：中心对齐模型原点 `(8,8,8)`，握柄长 6 单位（`grip_core` y 4.4–10.4），手有明确落点

## 重新生成

```bash
python tools/bbmodel-to-item-model.py --name molten_core_sword --scale 8
.\gradlew.bat build
```

（脚本从 bbmodel 取几何 + 每面 UV + 内嵌贴图，写模型 JSON 与 PNG；`inflate` 会烘进 `from/to`，
因为 Java 物品模型没有 inflate 字段。display 姿态写在脚本的 `DISPLAY` 常量里。）

## 部件 → 材质分区

| 前缀 | 部件 | 材质 / 颜色 |
|---|---|---|
| `blade_1..4` / `tip_1..3` | 四段刃身 + 三段收尖 | 黑曜石 `#16150F`（低噪点 stone，仅有轻微磨痕） |
| `bevel_a_*` / `bevel_b_*` | 左右两侧铜色刃口（各 4 段，比刃身厚 0.1 突出） | 铜 `#8E5A38`（gold，低 sheen） |
| `facet_a` / `facet_b` | 刃根两片斜切面 | 暗石 `#1E1C18` |
| `core_front_*` / `core_back_*` | 护手三角熔核（正反两面） | 自发光 `#FF8A2A` + `paint_faces` 亮芯 |
| `guard_*` | 护手横梁 / 套环 / 双翼 / 尖刺 / 面板 / 铆钉 | 暗铁 `#2B2624` |
| `ferrule_*` / `pommel_ring` | 铜箍、配重环 | 铜 `#C8962F` |
| `grip_core` / `wrap_1..5` | 握柄芯 + 5 道缠绳（外扩 0.15） | 皮革 `#40301C` |
| `tassel_*` | 流苏：绳、结、穗身、双股 | 皮革 `#40301C`（更暗） |
| `pommel_cap` | 配重帽 | 暗铁 |

熔岩裂纹画在 `blade_*` / `tip_*` 的 **south（正面）与 north（背面）** 两张面上：
主线 `#FFD08A` 1 px 亮芯 + `#FF7A1E` 2 px 外焰 + `#8A2E08` 4 px 焦边，各段带 1–2 条分叉，
段与段之间用同一 x 位置接续，整条刃身读起来是一整道裂纹。

## 与旧版（脚本生成）的差别

旧版由 `tools/gen-molten-core-sword.py` 直接写 JSON + 画图集，几何和贴图各写一套；
现在几何与贴图都来自同一个 `.bbmodel`，旧脚本仅保留作历史记录（不再参与构建）。

## 游戏内验收

- 创造模式取出 `northstar_planet_addon:molten_core_sword`：图标、手持、掉落物三者同一模型
- 第三人称/第一人称持握：握把在手中、刃身朝上，不出手背、不穿身体
- 客户端模型与贴图**不热重载**，改完必须重启客户端

## 决策记录：剑不做骨骼动画、也不做玩家挥砍动作

1. **物品自身骨骼动画（GeckoLib 动画物品）已撤销**。原因：NeoForge 只在模型 `isCustomRenderer()` 为真时才启用 `GeoItemRenderer`，即物品模型必须挂 `parent: builtin/entity`；接管后实测剑身整块发黑（`GeoItemRenderer` 用实体光照渲染，`#16150F` 这种近黑基色直接变纯黑剪影），且长刃绕中段旋转会被读成「软剑」。结论：收益不抵风险，剑保持**普通 Java 3D 物品模型**。
2. **玩家挥砍动作（类 Epic Fight）不做**。原因：玩家手臂动画由玩家实体模型驱动，物品无法改变；要自研必须接管 `RenderPlayerEvent` 重画玩家模型，会与同样接管玩家渲染的模组（First Person Model / Not Enough Animations / 其它战斗模组）冲突。作者明确要求优先保证**与其他模组的兼容性**。
3. 骨架与三段动画（`idle` / `swing` / `slam`，bones = `grip → blade / core / tassel`）仍保存在 `docs/models/molten_core_sword.bbmodel` 里，**不参与构建**。若将来换用别的动画系统，可直接复用；导出脚本 `tools/bbmodel-to-geckolib-item.py` 保留。
4. 现在参与构建的只有：`tools/bbmodel-to-item-model.py`（bbmodel → 静态物品模型 + 贴图）与 `tools/bbmodel-to-geckolib.py`（巨像动画，与剑无关）。
