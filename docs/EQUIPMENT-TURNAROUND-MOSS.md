# 装备武器 · 三视图美术需求 + MOSS-Image 直用提示词 v1.1

> **范围**：`northstar_planet_addon` 里**穿在身上与拿在手里的全部战斗物件** ——
> 火星线 2 件武器 + 熔核套（4 件甲）+ 2 件随身装备；金星线 6 件武器 + 4 套甲（16 件甲）；
> 附录 A 补月球线 1 件武器（梦眠月斧），附录 B 补随身装备的三视图规范。
> **不做什么**：本文**不产出** 16×16 图标、128×128 图集、64×32 穿戴层 —— 那些是
> [EQUIPMENT-ART-BRIEF.md](EQUIPMENT-ART-BRIEF.md) 的交付物。本文只解决一件事：
> **出一张能直接拿去做 Blockbench / Java 建模的三视图设定稿**。
> **提示词口径（v1.1 改）**：提示词里**不写长度、宽度、百分比、比例倍数** —— 那些会给模型一个
> 它其实控制不住的硬几何，最后只会换来"形状被数字压扁"的稿子。提示词只锁**身份、结构清单、配色、气质**，
> 体量关系交给模型自己给一个合理的。要精确尺寸的场合，等三视图出来了再在 Blockbench 里拉尺子。

---

## 0. 为什么"三视图"要单独出一份

原来的提示词包（`docs/AI-PROMPT-PACK.md`）给武器定的是**双视图**（45° 主视 + 侧视）、给护甲定的是
**正 / 3-4 / 背**。这两个框架解决的是"给像素美术看设计"，**解决不了建模**：

| 建模真正要的信息 | 双视图 / 3-4 视角给不了 | 三视图给得了 |
|---|---|---|
| 厚度 | 45° 里厚度被透视吃掉 | 侧视图的**纯厚度轮廓** |
| 背面结构 | 背面根本不画 | 背视图（护甲的后颈、披风内衬、背部熔核全在这） |
| 对称与否 | 因为视角斜，判断不了左右差 | 正视 + 背视并排，差 1 px 都看得见 |
| 同缩放 | 两格各自"适配画面"，比例不可比 | 三格一条基线、一个缩放，可以直接比 |

所以本文的口径是：**武器出「主视 / 侧视（或俯视）/ 背视」，护甲出「正视 / 侧视 / 背视」**，
三格等宽等高、同一缩放、共用一条隐形基线。这条版式是硬约束，不通过就重出（§6）。
**版式是硬的，物体本身是软的** —— 这是 v1.1 最核心的一条。

---

## 1. 三视图版式硬规范

### 1.1 三种版式

| 版式 | 用于 | 三格内容 | 画布 | 一句话 |
|---|---|---|---|---|
| **A1 · 冷兵器** | 融核巨剑 / 酸蚀矛 / 压力锤 / 高压钻 / 梦眠月斧 | 左 **主视**（刃面或钻头正面全宽，尖端朝上、握把朝下）· 中 **侧视**（绕长轴转 90°，只看厚度与护手/锤头的横向进深）· 右 **背视**（主视的反面） | 1536×768 | 立着放，三根同高的杆子 |
| **A2 · 器械** | 星核炮 / 雷羽弩 / 硫火喷射器 / 云钩 / 便携式冷却器 | 左 **主视**（侧面轮廓，枪口或钩头朝右）· 中 **俯视**（从正上方看横向展开：弩臂、喇叭口、抓钩开口）· 右 **后视**（从枪托/柄尾看） | 1536×768 | 横着放，左尾右头 |
| **B · 穿戴** | 五套护甲 + 护目镜 | 左 **正视**（正对镜头）· 中 **侧视**（脸朝画面左，与需求案 §4 一致）· 右 **背视** | 1536×1024 | 站在一条地面上，三具同高的人台 |

> 版式 B 的"人台"是**无面部特征的哑光灰人台**（featureless mannequin）。不要画活人、不要画脸、
> 不要画手。这一条在旧提示词包里已经写死，这里继承 —— MOSS-Image 画人脸太强，不明确禁止就会给你一张立绘。

### 1.2 硬指标（三格）

| # | 指标 | 值 |
|---|---|---|
| 1 | 格数 | **正好三格**，不多不少（第四格会被读成"变体"） |
| 2 | 同缩放 | 三格**同一缩放**；三格主体高度接近（差 ≤3%），每格占格高约八成 |
| 3 | 基线 | 共用一条**隐形水平地面基线**，三格脚底/握把尾端落在同一条线上 |
| 4 | 投影 | **正交（orthographic）**、机位水平、无透视缩短、无仰俯角 |
| 5 | 间距 | 格间留白 ≈ 单格宽度的 5%，三格不许互相压边、不许重叠 |
| 6 | 背景 | 纯色浅灰 `#6C7078`，无渐变、无地面投影、无场景 |
| 7 | 光 | 主光左上 45°，柔和环境补光；三格**光照方向完全一致**（不然三格会比色） |
| 8 | 文字 | **全画面零文字**：无标题、无编号、无字母、无箭头、无尺寸线、无色卡、无边框、无水印 |
| 9 | 设计一致 | 三格是同一件物品，不许任何一格的零件、宝石、绑带数量不同 |
| 10 | 输出 | 纯灰底 PNG，不做排版美化 —— 建模时要能直接吸色 |

> **第 8 条为什么最容易被违反**：MOSS-Image 的招牌能力恰恰是**渲染文字**，它看到引号里的词就会当成"要印上去的字"。
> 所以本文所有提示词里**不出现引号包裹的内容**（§2.4 有专门的防坑说明）。

### 1.3 三格坐标表（Layout 工作流直接抄）

MOSS-Image 的 Layout 引导支持**给每个元素一个框**，框坐标是"以画布中心为原点的绝对像素"，
顺序 `[y_min, x_min, y_max, x_max]`（`bbox_format: yxyx`，`bbox_coord_space: abs_centralized`）。
下面两张表是按画布算好的，**三个框固定不动，只换每格的描述**。

**1536×768（A1 / A2 版式）** —— 画布范围 x −768…768、y −384…384：

| 格 | 位置 | bbox |
|---|---|---|
| 左 · 主视 | x −744…−264 | `[-352, -744, 352, -264]` |
| 中 · 侧视 / 俯视 | x −240…240 | `[-352, -240, 352, 240]` |
| 右 · 背视 / 后视 | x 264…744 | `[-352, 264, 352, 744]` |

**1536×1024（B 版式）** —— 画布范围 x −768…768、y −512…512：

| 格 | 位置 | bbox |
|---|---|---|
| 左 · 正视 | x −744…−264 | `[-480, -744, 480, -264]` |
| 中 · 侧视 | x −240…240 | `[-480, -240, 480, 240]` |
| 右 · 背视 | x 264…744 | `[-480, 264, 480, 744]` |

> 换算规则（自己改画布时用）：设画布 W×H，先把左上原点的像素坐标减掉 W/2、H/2，
> 得到中心原点坐标，再按 `[y_min, x_min, y_max, x_max]` 填。宽高都必须是 **16 的倍数**。

---

## 2. 提示词骨架（复制粘贴的边界）

### 2.1 中文单段版（主用）

MOSS-Image 吃**描述最终画面**的长句（约 4K token 上限），不吃"帮我画一个"这种指令。所以骨架是
"**先定版式 → 再定主体 → 再给气质 → 最后定配色与工艺**"，四段用**真实换行**隔开：

```text
{版式段：三视图设定稿 + 三格各是什么 + 同缩放 + 同基线 + 正交 + 背景 + 零文字}
主体：{这件东西是什么 + 三个必须能被认出来的结构 + 必画的小件}
气质：{沉重还是轻快、粗野还是精致、工业还是礼仪，以及哪一头更重}
配色纪律：{主色 hex + 辅助色 + 只在什么地方发光}
材质与风格：{硬表面 + 哑光 + 三阶明度 + 左上高光 + 2D 平涂赛璐璐 + 缩到四分之一仍能数结构}
```

**为什么不给数字**（v1.1 的改动）：

1. 模型对"占全长 62%""刃宽是柄宽三倍"这类**比例倍数**的理解是"把这句话写进画面"，实际输出会走向
   两种失败：要么画成一张带标注的示意图，要么把物体硬掰成那个比例、其它结构全丢；
2. 三视图的作用是**定结构、定材质、定配色**，尺寸在 Blockbench 里拉尺子比在图上量更准；
3. 真正需要"数得清"的是**可数的结构件**（三道裂纹、五尖冠、两只压力表），不是毫米级比例 ——
   所以本文只保留"数得清"的约束，删掉了所有长度/宽度/百分比。

### 2.2 英文单段版（模型对中文结构不稳时用）

```text
A three-view design sheet of a {item}, three panels of equal width and height in one horizontal row:
left panel front view, middle panel side view, right panel back view, the same object in all three panels,
the same scale in all three, sharing one invisible ground baseline, orthographic projection, level camera,
no perspective foreshortening, flat neutral grey background #6C7078, even studio lighting with the key light
from the upper left, absolutely no text, numbers, letters, arrows, dimension lines, colour chips, watermarks or border.
Subject: {structure list}. Overall read: {heavy and top-weighted, thin and light, brutal, ceremonial}.
Colours: {main hex, secondary hex, and where the glow appears only}.
Materials: matte hard-surface industrial design, three-value cel shading, crisp edges, readable at quarter size.
```

中文段和英文段**不要混用在同一条提示词里**，模型会把语种切换当成风格切换。

### 2.3 Layout JSON 骨架（三格精确落位，推荐）

| 字段 | 填什么 |
|---|---|
| `summary.meta` | `["concept art", "turnaround sheet", "game asset"]` |
| `summary.canvas` | `"single"` |
| `summary.caption` | 总体描述：三格是什么、什么风格、什么背景 |
| `items[0..2]` | 每格一条：`label` 一律 `object`（**不要用 `character`**，会被当人物画）、`caption` 是该格的描述 |
| `bbox` | 抄 §1.3 的表 |
| `bbox_coord_space` / `bbox_format` | `abs_centralized` / `yxyx`（固定值） |

```json
{
  "summary": {
    "meta": ["concept art", "turnaround sheet", "game asset"],
    "canvas": "single",
    "caption": "A three-view design sheet on a flat neutral grey background, three panels side by side in one row with the same scale and one shared baseline, orthographic and level, no text anywhere. A colossal two-handed greatsword of black basalt split by glowing ember cracks, matte industrial hard-surface, three-value cel shading."
  },
  "items": [
    {"label": "object", "caption": "Front view: the flat of the blade faces the camera, tip up, grip down, guard spread horizontally.", "bbox": [-352, -744, 352, -264]},
    {"label": "object", "caption": "Side view: rotated ninety degrees, only the blade thickness, the guard sweep and the pommel depth are visible.", "bbox": [-352, -240, 352, 240]},
    {"label": "object", "caption": "Back view: the reverse face of the identical blade, the same three cracks seen from behind, the burnt tassel hangs on the opposite side.", "bbox": [-352, 264, 352, 744]}
  ],
  "bbox_coord_space": "abs_centralized",
  "bbox_format": "yxyx"
}
```

> **Layout 工作流有两件事必须手动确认**：① 在 ComfyUI 里把 `enable_prompt_enhancer` 关掉（开着会把框改写掉）；
> ② `character_boxes` 留空 —— 那是给"人物"用的，填了会把装备画成人。

### 2.4 三条防坑（比参数更重要）

1. **不要写引号**。`"正面"`、`"Front"` 这种带引号的词会被当成要印在图上的文字。要强调就写"正面视图"，不加引号。
2. **不要在一条提示词里既描述三格又要求一致**（如"三格完全一样"）。MOSS 会把它读成"要三份拷贝"，
   于是同一格出现三次。正确写法是**逐格写各自视角**，一致性靠 bbox + 同 seed 保证，不靠嘴说。
3. **不要留占位符**。`{主色}`、`<部位>` 这种没替换的尖括号会被当成排版符号，直接进画面。

---

## 3. MOSS-Image 参数与负向词

### 3.1 两种跑法，参数不同

| 设置 | 用哪套 | steps | cfg | sampler | shift | SLG | 尺寸 |
|---|---|---|---|---|---|---|---|
| **A. ComfyUI 模板默认**（快，先挑构图） | `MOSS-Image: Text to Image` / `Layout-Guided Text to Image` | 25 | 2.5 | `er_sde` | 5.0 | 开（22 层） | 1536×768 或 1536×1024（`ResolutionSelector`，默认 1 MP） |
| **B. 官方推荐**（定稿、细节多时） | 同上，改采样器 | **50** | **3.0**（3.0–6.0，长提示词 3.0 够） | `euler` | 5.0 | 开（22 层） | 同上；总像素落在 1–2 MP |

- **定稿换 B**：三视图里要"数清裂纹 / 看清炮膛 / 数出焊点"，25 步会糊。
- **批量挑构图**：`batch_size` 改 4，同一 seed 出 4 张挑一张，别一条提示词只跑一张。
- **不要把分辨率堆到 2 MP 以上**：模型在 4 MP 开始靠外推，三格的细件会重复而不是变细。
- **旧提示词包的 `896×1152 / steps 32 / cfg 6.5 / MJ --ar` 一律不适用**，那是 SDXL 和 MJ 的参数。

### 3.2 负向词（短，别堆）

MOSS-Image 官方只给了五个 tag（`nsfw, worst quality, bad anatomy, watermark, jpeg artifact`），并建议负面**保持简短**。
三视图真正需要额外压的只有"字"和"多出来的格子"，所以核心负面就九项，**每条都带**：

```text
nsfw, worst quality, bad anatomy, watermark, jpeg artifact, text, letters, numbers, arrows, extra views
```

卡片里的「负向追加」按需挑 **3–5 个**贴在后面，全部加起来不要超过 20 个 tag —— 三视图的崩坏主要靠
**正向写清楚**（"三格同一设计""正交""无文字"）来防，负面堆太长只会把零件细节一起压没。

> 旧提示词包里那串 40+ 项（`glossy plastic, neon glow haze, lens flare...`）是给 SDXL/MJ 的，
> MOSS-Image 对负面不敏感但**对过长负面敏感**。要禁止什么，优先写进正向
> （"哑光材质，只有左上缘一道窄高光"）而不是只写负向。

### 3.3 出图顺序（三视图的正确流水线）

1. **先出单件主视**（把三格改成"单个正面全文"），挑一张最像的 —— 这一步用 25 步足够；
2. **再用 Layout 骨架**出三格，`summary.caption` 抄第 1 步挑中那张的描述，seed **固定**；
3. 三格里哪一格崩了（最常见的：侧视图塌成正面、背视图长出新零件），**只改那一格的 caption**，其他两格一个字不动，重跑；
4. 定稿后按 3.1 的 B 套参数重跑一次 50 步，作为交付稿。

> **MOSS-Image 没有图生图输入**（模板里的 `ref_image` 只喂提示词增强器，不进扩散过程），
> 所以旧文档里"先出正面再 img2img 出侧面"的做法在这个模型上**不成立**。
> 同 seed + 同 bbox + 只改一格描述，是这个模型上最接近"保持一致"的手段。

---

## 4. 逐件需求 + 直达提示词

> 每张卡：**版式**（§1.1）→ **三格要交代什么** → **体量与气质** → **配色纪律** → **必画 / 禁画** →
> **提示词**（整段复制，已含版式段与风格尾）→ **负向追加** → **三格 caption（Layout 用）** → **验收**。
> 配色与结构来源沿用 [EQUIPMENT-ART-BRIEF.md](EQUIPMENT-ART-BRIEF.md) §4；**长度与宽度比例一律不进提示词**
> （v1.1），只在「禁画」里挡住明显跑偏的体量。

---

### 4.1 火星线 · 熔炉与战神

#### 4.1.1 `molten_core_sword` · 融核巨剑 〔EPIC · 伤害 12〕

**版式** A1（主视 / 侧视 / 背视）· 画布 1536×768

| 格 | 必须交代 |
|---|---|
| 主视（左） | 刃面全宽、分叉裂纹、刃根熔核、护手两端不对称（一端四芒星 / 一端凹槽） |
| 侧视（中） | 刃的厚度（薄板）、护手向两侧的进深、配重球与两条流苏的下垂方向 |
| 背视（右） | 同一片刃的反面：裂纹从背面看是**同几道**，血槽余烬线偏到另一侧，流苏在相反一边 |

- **体量与气质**：宽厚的双手巨剑，**刃身明显长于柄**，护手横向外扩压住重心 —— 读起来是"沉"，不是"快"
- **配色纪律**：黑曜石 + 熔壳红褐为主 · 黄铜只做箍与包边 · 余烬橙与刃口暖白**只在裂纹与血槽里**
- **必画**：可数的分叉裂纹（缝里透余烬）；血槽里一条**断续**余烬线（不许连成整条）；护手一端四芒星 + 另一端一道凹槽；握柄数道缠绳（靠护手一端更密）；配重球 + 两条**烧焦毛边**流苏
- **禁画**：整条刃口发光 · 左右对称的双面装饰 · 卡通火焰轮廓 · 发光宝珠 · 剑柄变细杆

```text
一张 Minecraft 模组武器装备的三视图设定稿，
横向三格等距并排：左格为正视图，中格为侧视图，右格为后视图，三格是同一件武器的同一设计，同一缩放，共用一条隐形水平基线，正交投影，机位水平，没有透视缩短，纯色浅灰背景，棚拍均匀打光，主光来自左上，柔和环境补光，画面里没有任何文字、编号、字母、箭头、尺寸线、色卡、边框和水印，
主体：一把宽厚的双手巨剑，刃身是一整块黑色玄武岩，几道裂纹从刃身中部向刃口分叉、缝里透出橙色余烬；刃根嵌着一颗熔核，血槽里一条断续的余烬线略微偏在一侧；护手厚重且左右不对称，一端镂空一颗四芒星、另一端只有一道凹槽；握柄缠绳、靠近护手一端更密，尾部配重球下垂两条烧焦毛边的流苏。
气质：沉重粗野，刃身明显比柄部长，护手横向外扩，重心靠前，读起来像砸下去的东西而不是快剑。
配色纪律：黑曜石与熔壳的红褐是主色，黄铜只出现在箍与包边上，余烬橙与刃口暖白只出现在裂纹和血槽里，刃的其余部分保持哑光黑、不整体发亮。
材质与风格：方块化工业硬表面设计，哑光材质，金属面至少三阶明度，高光只留在左上缘，2D 平涂三阶赛璐璐上色，边缘干净锐利，缩到四分之一大小仍能数清裂纹与流苏。
```

**负向追加**：`pure energy blade, symmetric guard, glowing sword edge, cartoon flame, magic rune, floating gem, elegant silver blade`

**三格 caption（Layout 用）**
- 主视 `Front view: the flat of the blade faces the camera, tip up, grip down, ember cracks fork toward the edge, guard spread horizontally.`
- 侧视 `Side view: rotated ninety degrees, only the blade thickness, the guard sweep depth and the two tassels hanging down are visible.`
- 背视 `Back view: the reverse face of the identical blade, the same cracks seen from behind, the ember line in the fuller sits on the opposite side.`

**验收**：① 三格裂纹根数一致；② 侧视看得到"薄"；③ 背视与主视不是镜像（流苏在反侧）；④ 刃面没有大面积发光。

---

#### 4.1.2 `starcore_cannon` · 星核炮 〔EPIC · 伤害 20〕

**版式** A2（主视 / 俯视 / 后视）· 画布 1536×768

| 格 | 必须交代 |
|---|---|
| 主视（左） | 炮身前粗后细、炮口可见同心膛线（最内圈最亮）、中段宽铜箍、侧面星核观察窗、尾部压力罐 + 两根软管、炮口下方横向格栅 |
| 俯视（中） | 炮口的圆、观察窗的凸出、两根软管往哪一侧绕、握把偏在哪一边 |
| 后视（右） | 枪托端面、压力罐的圆、扳机护圈的内轮廓 |

- **体量与气质**：手持重炮 —— **前段明显粗过后段**，一眼读出"炮"而不是"法杖"；粗野、重、不灵巧
- **配色纪律**：火星钢为主 · 黄铜箍与边框为辅 · 星核橙与白热**只在观察窗内部与炮膛最内圈**
- **必画**：可见的炮膛同心环（由外向内逐级变亮）；圆形观察窗（黄铜框 + 玻璃 + 内部星核）；尾部压力罐与两根**带肋纹**软管；炮口下方横向格栅；扳机护圈 + 握把缠绳
- **禁画**：法杖细杆 · 科幻能量纹 · 整根发光 · 榴弹发射器式的短粗口

```text
一张 Minecraft 模组武器装备的三视图设定稿，
横向三格等距并排：左格为主视图（侧面轮廓，炮口朝右），中格为俯视图，右格为后视图，三格是同一件器械的同一设计，同一缩放，共用一条隐形水平基线，正交投影，机位水平，没有透视缩短，纯色浅灰背景，棚拍均匀打光，主光来自左上，柔和环境补光，画面里没有任何文字、编号、字母、箭头、尺寸线、色卡、边框和水印，
主体：一门手持重型迫击炮，炮身前粗后细，炮口内壁露出同心膛线、由外向内逐级变亮；中段一道宽黄铜箍；炮身侧面一颗圆形观察窗，黄铜边框里是玻璃与内部一颗星核；尾部斜挂一只压力罐，两根带肋纹的软管绕回炮身；炮口下方一排横向格栅散热窗；下方扳机护圈与缠了绳的握把。
气质：重型、粗壮、端着沉的工程器械，前段体量明显大于后段，读起来像炮而不是法杖。
配色纪律：火星钢是主色，黄铜只做箍、边框与接头，星核橙与白热只出现在观察窗内部与炮膛最内圈，枪身其余部分哑光不发光。
材质与风格：方块化工业硬表面设计，哑光材质，金属面至少三阶明度，高光只留在左上缘，2D 平涂三阶赛璐璐上色，边缘干净锐利，缩到四分之一大小仍能读出炮口与炮尾。
```

**负向追加**：`staff, wand, thin rod, sci-fi laser rifle, energy cannon, glowing whole barrel, rocket launcher, toy proportions`

**三格 caption（Layout 用）**
- 主视 `Side silhouette of a heavy handheld mortar cannon, muzzle to the right, concentric rifling rings inside the bore, brass band mid-barrel, round star-core window on the side, rear pressure tank with two ribbed hoses.`
- 俯视 `Top view: the round muzzle, the window bulge on one flank, the two hoses looping along one side only.`
- 后视 `Rear view: the stock end face, the round pressure tank, the inner outline of the trigger guard.`

**验收**：① 一眼分清炮口/炮尾；② 膛线层次可数；③ 俯视图能看出软管只走一侧；④ 无科幻能量线。

---

#### 4.1.3 熔核套 `molten_core_{helmet,chestplate,leggings,boots}` 〔EPIC · 护甲 5/12/9/5 · 韧性 4.5〕

**版式** B（正视 / 侧视 / 背视）· 画布 1536×1024 · **一套四件画在同一张三视图上**（人台穿全套）

| 格 | 必须交代 |
|---|---|
| 正视（左） | 五尖冠、两条眼缝、胸前圆形熔核开口、双膝护片、靴底前掌赤热 |
| 侧视（中） | 肩背双翼鳍的厚度、后颈护板、腰封厚度、靴踝箍 |
| 背视（右） | 背部熔核开口的背面、两条锁链的走向、肩鳍的背面轮廓、腰带扣在后腰的结构 |

- **体量与气质**：**高瘦剪影**（肩线高、腰线收），从熔岩里捞出来的板甲 —— 厚重、灼热、不修边幅
- **配色纪律**：甲面黑褐 + 熔壳红褐为主 · 硫金只做包边与腰扣 · 余烬橙只在眼缝、熔核开口与靴底，宝石极小
- **必画**：五尖冠（中尖最高、两侧递减）；两条**只透微光**的眼缝；胸前圆形熔核开口 + 两条锁链（不是飘带）；方形硫金腰带扣正对中线；双膝护片；靴底前掌一块赤热
- **禁画**：布质披风（会遮背部熔核）· 对称光翼 · 可爱化短腿大头 · 整块甲面发光

```text
一张 Minecraft 模组护甲的三视图设定稿，画在三具无面部特征的哑光灰人台上，
横向三格等距并排：左格为正视图，中格为侧视图（脸朝画面左），右格为背视图，三格是同一套护甲的同一设计，同一缩放，共用一条隐形水平基线，脚底落在同一条线上，正交投影，机位水平，没有透视缩短，纯色浅灰背景，棚拍均匀打光，主光来自左上，柔和环境补光，画面里没有任何文字、编号、字母、箭头、尺寸线、色卡、边框和水印，
主体：一套从熔岩里捞出来的重型板甲，头盔是五尖冠、中间最高两侧递减，面甲上两条窄眼缝只透一点暗光；胸甲中心一个圆形熔核开口，两侧垂下两条真实锁链而不是飘带，肩背有向外张开的双翼状鳍板；腰封一条方形硫金腰带扣正对中线，膝部有方形护片，靴底前掌一块赤热橙红；甲片边缘是硫金包边，表面有铸造痕与崩口。
气质：高瘦而厚重，肩线抬高、腰线收紧，整套压在身上而不是贴在上面；表面粗野、有使用痕迹。
配色纪律：甲面黑褐与熔壳红褐是主色，硫金只出现在包边与腰带扣，余烬橙只在眼缝、胸前熔核开口与靴底，宝石极小、不抢视觉，亮部不整片发光。
材质与风格：方块化工业硬表面设计，哑光材质，金属面至少三阶明度，高光只留在左上缘，2D 平涂三阶赛璐璐上色，边缘干净锐利，缩到四分之一大小仍能数出五个冠尖与两条锁链。
```

**负向追加**：`cape, cloth cloak, glowing wings, cute chibi proportions, round helmet, symmetric halo, floating armor pieces, visible face, thin legs`

**三格 caption（Layout 用）**
- 正视 `Front view of a full armor set on a featureless matte grey mannequin: five-spike crown helmet with two narrow eyeslits, round molten core opening in the chest, twin fin pauldrons, square brass belt buckle on the centre line, knee guards, hot orange boot soles.`
- 侧视 `Side view, facing left: the thickness of the back-swept fin pauldrons, the rear neck guard, the waist thickness and the ankle rings.`
- 背视 `Back view: the reverse of the molten core opening, the two real chains running down the back, the fin outline from behind, the belt buckle closing at the lower back.`

**验收**：① 五个冠尖可数；② 背视图里锁链是"链"不是飘带；③ 三格靴底都赤热；④ 与其它四套并排时能读出"最重、最热"。

---

### 4.2 金星线 · 六把武器

#### 4.2.1 `acidic_spear` · 酸蚀矛 〔UNCOMMON · 伤害 9 / 攻速 1.4〕

**版式** A1 · 画布 1536×768

| 格 | 必须交代 |
|---|---|
| 主视（左） | 叶形枪头 + 两条开槽（槽内两段不连续酸液）、枪头根部的蚀孔、两道铜箍、握柄缠绳与一处油污、尾端配重球 + 分叉酸绿流苏 |
| 侧视（中） | 枪头的薄、开槽的通透、箍的进深 |
| 背视（右） | 枪头反面（开槽从背面是两道透光缝）、蚀孔集中在根部与箍下 |

- **体量与气质**：**长杆为主、枪头只占很小一截**的工业长枪；冷、细、有积液与蚀痕，不发光不圣洁
- **配色纪律**：钨钢由暗到亮四阶为主 · 黄铜箍为辅 · 酸绿**只在枪头开槽与流苏**
- **必画**：槽内**两段**不连续酸液（不是一条直线）；根部的蚀孔；间距不等的两道箍；握柄油污；分叉流苏
- **禁画**：整根发绿光 · 圣枪式镜面反光 · 光滑对称的装饰箍

```text
一张 Minecraft 模组武器装备的三视图设定稿，
横向三格等距并排：左格为正视图，中格为侧视图，右格为后视图，三格是同一件武器的同一设计，同一缩放，共用一条隐形水平基线，正交投影，机位水平，没有透视缩短，纯色浅灰背景，棚拍均匀打光，主光来自左上，柔和环境补光，画面里没有任何文字、编号、字母、箭头、尺寸线、色卡、边框和水印，
主体：一支钨钢长枪，枪头是叶形，中间两条细开槽里各有一段不连续的酸液，枪头根部沿受力方向有几个蚀孔；杆身中段两道黄铜箍、间距一宽一窄；握柄区缠绳，绳间有一处油污；尾端一颗配重球，垂一条分叉的酸绿色短流苏。
气质：又长又细的工业长杆，枪头只是杆顶的一小截；冷、克制、有积液与蚀痕，没有圣器感。
配色纪律：钨钢由暗到亮是主色，黄铜只做箍，酸绿只出现在枪头开槽与流苏上，枪杆不发光。
材质与风格：方块化工业硬表面设计，哑光材质，金属面至少三阶明度，高光只留在左上缘，2D 平涂三阶赛璐璐上色，边缘干净锐利，缩到四分之一大小仍能看出一宽一窄两道箍。
```

**负向追加**：`glowing whole shaft, holy lance, mirrored silver spear, magic runes, halo, symmetric ornaments`

**三格 caption（Layout 用）**
- 主视 `Front view of a tungsten spear, leaf-shaped head with two grooves each holding one separate green acid segment, etch pits at the head root, two brass bands at unequal spacing, cord wraps on the grip, forked acid-green tassel at the butt.`
- 侧视 `Side view: the blade is thin, the grooves read as through-slots, the bands show their depth.`
- 背视 `Back view: the reverse of the head where the two grooves appear as two light slots, etch pits concentrated under the root and below the bands.`

**验收**：① 槽里是两段酸液；② 两道箍间距明显不等；③ 侧视看得到开槽通透；④ 枪杆不发光。

---

#### 4.2.2 `pressure_hammer` · 压力锤 〔RARE · 伤害 13 / 攻速 0.75〕

**版式** A1 · 画布 1536×768

| 格 | 必须交代 |
|---|---|
| 主视（左） | 锤头两侧各一个圆形压力表（**指针角度不同**）、锤面六角凹坑与铆钉、加强肋与泄压阀、柄部缠绳 + 尾端粗箍 + 防脱绳、锤头一侧旧裂纹与焊补 |
| 侧视（中） | 锤头横向的进深、两个表分别在哪个面、锤面朝下还是朝前 |
| 背视（右） | 另一侧压力表、焊补痕的另一半、加强肋背面 |

- **体量与气质**：**锤头大、柄短粗**，头重柄轻；工业、被砸过、有旧伤与焊补
- **配色纪律**：钨灰为主 · 钢饰与抛光棱为辅 · 仪表青只做表盘 · 黄铜只做泄压帽
- **必画**：两只指针角度不同的压力表；锤面六角凹坑 + 铆钉；三角加强肋 + 小泄压帽；柄部缠绳 + 尾端粗箍 + 防脱绳；一处旧裂纹与焊补（不抢主视觉）
- **禁画**：幻想巨锤（超大锤面/尖刺）· 发光锤面 · 木柄

```text
一张 Minecraft 模组武器装备的三视图设定稿，
横向三格等距并排：左格为正视图，中格为侧视图，右格为后视图，三格是同一件武器的同一设计，同一缩放，共用一条隐形水平基线，正交投影，机位水平，没有透视缩短，纯色浅灰背景，棚拍均匀打光，主光来自左上，柔和环境补光，画面里没有任何文字、编号、字母、箭头、尺寸线、色卡、边框和水印，
主体：一柄用高压容器做锤头的战锤，锤头两侧各嵌一只圆形压力表、两只表针指向不同角度，锤面正中一个六角凹坑、四周一圈铆钉；锤头与短粗锤柄的连接处有三角加强肋和一只小黄铜泄压帽；锤头一侧有一道旧裂纹与焊补痕迹；锤柄缠绳，尾端一道粗箍并挂一根防脱绳。
气质：头重柄短，锤头的体量压过手柄；工业、被砸过，读起来是工具而不是礼器。
配色纪律：钨灰是主色，钢饰与抛光棱提亮，仪表青只出现在表盘，黄铜只做泄压帽，锤面不发光。
材质与风格：方块化工业硬表面设计，哑光材质，金属面至少三阶明度，高光只留在左上缘，2D 平涂三阶赛璐璐上色，边缘干净锐利，缩到四分之一大小仍能看出两只表与短粗柄。
```

**负向追加**：`fantasy giant hammer, spiked maul, glowing hammer face, wooden handle, magic rune, steam burst`

**三格 caption（Layout 用）**
- 主视 `Front view of a war hammer whose head is a pressure vessel: one round gauge on each flank with needles at different angles, hexagonal recess and rivets on the striking face, triangular gussets and a small brass relief cap at the neck, cord wraps and a wrist strap.`
- 侧视 `Side view: the depth of the head, one gauge on the near face, the striking face pointing down-forward.`
- 背视 `Back view: the gauge on the far flank, the weld patch continuing, the gussets seen from behind.`

**验收**：① 两只表指针角度不同；② 锤头体积明显重于柄；③ 焊补不抢主视觉。

---

#### 4.2.3 `thunder_crossbow` · 雷羽弩 〔RARE · 伤害 9 / 蓄力 1.2s / 连锁 3〕

**版式** A2（主视 / 俯视 / 后视）· 画布 1536×768

| 格 | 必须交代 |
|---|---|
| 主视（左） | 两段式弩臂 + 臂尖雷羽状薄刃、弦上断续电弧、弦槽与上膛弩矢（箭头 + 双尾羽）、侧面充能窗、瞄具 + 扳机护圈 |
| 俯视（中） | 弩臂展开宽度、托肩的偏侧、瞄具在中轴 |
| 后视（右） | 托肩端面、充能窗在侧面的位置、弦的走线 |

- **体量与气质**：**横向展开的弩臂是主体量**，弩身偏短；精密、带电、但不发整片蓝光
- **配色纪律**：钢四阶为主 · 雷光与电弧白**只在线与窗** · 铜件只做小件
- **必画**：两段式臂、臂尖雷羽状薄刃（不是羽毛笔）；弦上**不连续**电弧；可见的弩矢（箭头 + 双尾羽）；充能窗（有刻度）；瞄具 + 扳机护圈 + 缠绳
- **禁画**：整把发蓝光 · 卡通闪电贴纸 · 羽毛笔式羽毛 · 枪托式长枪身

```text
一张 Minecraft 模组武器装备的三视图设定稿，
横向三格等距并排：左格为主视图（侧面轮廓，弩口朝右），中格为俯视图，右格为后视图，三格是同一件器械的同一设计，同一缩放，共用一条隐形水平基线，正交投影，机位水平，没有透视缩短，纯色浅灰背景，棚拍均匀打光，主光来自左上，柔和环境补光，画面里没有任何文字、编号、字母、箭头、尺寸线、色卡、边框和水印，
主体：一把以雷羽作弓臂的弩，弩臂分两段、臂尖各装几片薄刃状的雷羽；弦上浮着断续的电弧，没有连成一条；弩身中段有弦槽和一支上膛的弩矢，能看到箭头与双尾羽；侧面一扇带刻度的充能窗，顶部一段瞄具，下方扳机护圈，托肩缠绳。
气质：横向展开的弩臂是最大体量，弩身相对短；精密、带电、机械感强，但没有整片蓝光。
配色纪律：钢的四个明度阶是主色，雷光与电弧白只出现在弦与充能窗附近，铜件只做小配件，弩身本身不发光。
材质与风格：方块化工业硬表面设计，哑光材质，金属面至少三阶明度，高光只留在左上缘，2D 平涂三阶赛璐璐上色，边缘干净锐利，缩到四分之一大小仍能看出一段式臂尖与充能窗。
```

**负向追加**：`glowing whole bow, cartoon lightning bolt sticker, feather quill, long rifle body, magic bow, energy arrow`

**三格 caption（Layout 用）**
- 主视 `Side view of a crossbow with thunder-feather limbs, muzzle to the right, two-segment limbs tipped with blade-like feathers, separate short arcs on the string, a loaded bolt with arrowhead and two fletchings in the groove, a ticked charge window on the flank, sight and trigger guard.`
- 俯视 `Top view: the full span of the two limbs, the stock offset to one side, the sight on the centre line.`
- 后视 `Rear view: the stock end face, the charge window on the flank, the string running straight across.`

**验收**：① 弩臂能看出两段；② 电弧是不连续的几段；③ 充能窗有刻度。

---

#### 4.2.4 `sulfur_flamethrower` · 硫火喷射器 〔RARE · 6 伤/秒 · 点燃 8s〕

**版式** A2 · 画布 1536×768

| 格 | 必须交代 |
|---|---|
| 主视（左） | 罐在左、枪管在右、喇叭口分段收口、罐体液位带、两根软管、握把 + 扳机护圈、喷口前**点状**火苗 |
| 俯视（中） | 罐的圆、软管绕过哪一侧、喷口口径 |
| 后视（右） | 罐底阀、握把端面、软管接口 |

- **体量与气质**：**罐与枪管接近对半的短器械**，横着端；结构必须看得清，火只能是点状
- **配色纪律**：钢灰为主 · 硫黄罐为辅 · 火橙少量 · 硫焰芯绿**只做火苗内核**
- **必画**：罐体液位带；两根软管；喇叭口分段收口；喷口前**点状不连续**火苗；握把 + 扳机护圈
- **禁画**：整片火焰糊住枪身 · 罐体爆炸感 · 科幻能量罐

```text
一张 Minecraft 模组武器装备的三视图设定稿，
横向三格等距并排：左格为主视图（侧面轮廓，喷口朝右），中格为俯视图，右格为后视图，三格是同一件器械的同一设计，同一缩放，共用一条隐形水平基线，正交投影，机位水平，没有透视缩短，纯色浅灰背景，棚拍均匀打光，主光来自左上，柔和环境补光，画面里没有任何文字、编号、字母、箭头、尺寸线、色卡、边框和水印，
主体：一支手持硫火喷射器，左半是一只硫黄色燃料罐、罐身一道横向液位带，罐与短枪管之间用两根软管相连；枪管前端是分段收口的喇叭口，喷口外只有几团点状不连续的火苗；下方手枪式握把与扳机护圈，握把缠一段革带。
气质：罐与枪管体量接近的短粗器械，横着端在手里；结构必须看得清，火焰轻薄、不能遮住枪身。
配色纪律：钢灰是主色，硫黄罐是第二块颜色，火焰橙只出现在喷口外，硫焰芯的绿只做火苗内核一小点，革带压暗。
材质与风格：方块化工业硬表面设计，哑光材质，金属面至少三阶明度，高光只留在左上缘，2D 平涂三阶赛璐璐上色，边缘干净锐利，缩到四分之一大小仍能分出罐、枪管、火苗三段。
```

**负向追加**：`flame covering the whole gun, fire cloud, explosion, sci-fi energy canister, magic fire, smoke everywhere`

**三格 caption（Layout 用）**
- 主视 `Side view of a handheld sulfur flamethrower, nozzle to the right: sulfur-yellow fuel tank on the left with a horizontal level band, two hoses to a short barrel, a flared nozzle with a few separate dots of flame outside it, pistol grip and trigger guard.`
- 俯视 `Top view: the round tank, the two hoses running along one flank, the diameter of the flared nozzle.`
- 后视 `Rear view: the tank bottom valve, the grip end face, the hose sockets.`

**验收**：① 罐/枪管/火三段分明；② 火苗是点状；③ 结构不被火遮住。

---

#### 4.2.5 `pressure_drill` · 高压钻 〔RARE · 挖掘 10 · 3×3〕

**版式** A1 · 画布 1536×768

| 格 | 必须交代 |
|---|---|
| 主视（左） | 螺旋排屑槽钻头 + 收锥钻尖 + 钻头环、侧面圆形压力表与朝后的气喉接口、机体箍 + 横条散热窗、握把缠绳 + 扳机 + 托底、一圈螺栓其中一颗松动 |
| 侧视（中） | 钻头螺旋的进深、气喉从哪一侧出、机体厚度 |
| 背视（右） | 气喉接口正面、机体背面散热窗、握把背弧 |

- **体量与气质**：**钻头长、机体粗、后段短握把**的气动工具；重、吵、油污感的工程器械
- **配色纪律**：钢四阶为主 · 锈色做沟槽与缝隙 · 黄铜只做箍 · 仪表青只有表盘一点
- **必画**：可数的螺旋排屑槽 + 收锥钻尖；压力表 + 气喉接口；机体箍与横条散热窗；握把缠绳 + 扳机；一颗明显松动的螺栓
- **禁画**：钻头过短（读成锤子）· 整体发光 · 科幻等离子钻

```text
一张 Minecraft 模组武器装备的三视图设定稿，
横向三格等距并排：左格为正视图，中格为侧视图，右格为后视图，三格是同一件武器的同一设计，同一缩放，共用一条隐形水平基线，正交投影，机位水平，没有透视缩短，纯色浅灰背景，棚拍均匀打光，主光来自左上，柔和环境补光，画面里没有任何文字、编号、字母、箭头、尺寸线、色卡、边框和水印，
主体：一把气动钻机，前段是带螺旋排屑槽的钻头、钻尖收锥、根部几道钻头环；中段机体最粗，侧面一只圆形压力表与一个朝后的气喉接口，机体上有几道箍与横条散热窗；后段手枪握把缠绳，带扳机与托底；机体上的一圈螺栓里有一颗明显松动。
气质：钻头很长、机体很粗、后段握把短，读起来是重型气动工具；有油污与锈迹，不是精密仪器。
配色纪律：钢的四个明度阶是主色，锈色只进排屑槽与缝隙，黄铜只做箍，仪表青只有表盘一点亮色，全身不发光。
材质与风格：方块化工业硬表面设计，哑光材质，金属面至少三阶明度，高光只留在左上缘，2D 平涂三阶赛璐璐上色，边缘干净锐利，缩到四分之一大小仍能数出螺旋段数。
```

**负向追加**：`short drill bit, hammer head, glowing plasma drill, laser cutter, magic drill, oversized gem`

**三格 caption（Layout 用）**
- 主视 `Front view of a pneumatic drill: a helical bit with a tapered tip and a few rings, the widest body in the middle with one round gauge and a rear air-hose port, hoops and louver vents, a pistol grip with cord wraps, one bolt visibly loose.`
- 侧视 `Side view: the depth of the helix, the hose port leaving from one flank, the body thickness.`
- 背视 `Back view: the hose port face-on, the rear louvers, the curve of the grip.`

**验收**：① 螺旋段数可数；② 表与气喉在视觉上连成一条气路；③ 钻头明显比机体长。

---

#### 4.2.6 `cloud_hook` · 云钩 〔UNCOMMON · 伤害 2 / 24 格〕

**版式** A2 · 画布 1536×768

| 格 | 必须交代 |
|---|---|
| 主视（左） | J 形钩头、钩尖内弯带倒刺、钩背蒙半透明云帆膜、两段钢柄 + 一道铜箍、柄尾一圈绳 |
| 俯视（中） | 钩口的宽度与开口方向、膜的薄 |
| 后视（右） | 钩背膜的外表面、柄尾绳圈 |

- **体量与气质**：**钩头小、柄长**的抓钩；轻、薄、能挂住东西，不是假肢
- **配色纪律**：钢为主 · 云帆膜做背板 · 云白只出现在钩体与膜缘高光 · 铜只做一道箍
- **必画**：内弯倒刺；钩背半透明膜（膜缘比膜亮一阶）；两段柄 + 一道箍；柄尾绳圈
- **禁画**：海盗铁钩（假肢感）· 绳梯式长绳 · 发光钩体

```text
一张 Minecraft 模组武器装备的三视图设定稿，
横向三格等距并排：左格为主视图（侧面轮廓，钩头朝右上），中格为俯视图，右格为后视图，三格是同一件器械的同一设计，同一缩放，共用一条隐形水平基线，正交投影，机位水平，没有透视缩短，纯色浅灰背景，棚拍均匀打光，主光来自左上，柔和环境补光，画面里没有任何文字、编号、字母、箭头、尺寸线、色卡、边框和水印，
主体：一支用云帆膜与轻钢做成的抓钩，钩头是 J 形、钩尖向内弯并带一枚短倒刺，钩背蒙着一层半透明云帆膜、膜缘比钩体亮一阶；钩柄分两段、中间一道铜箍，柄尾挂一圈缠好的绳。
气质：钩头小而柄长，整体轻、薄，读起来是能挂住东西的抓钩，不是假肢。
配色纪律：钢是主色，云帆膜的深蓝是背板，云白只出现在钩体与膜缘的高光上，铜只做一道箍，钩体不发光。
材质与风格：方块化工业硬表面设计，哑光材质，金属面至少三阶明度，高光只留在左上缘，2D 平涂三阶赛璐璐上色，边缘干净锐利，缩到四分之一大小仍能读出钩与柄。
```

**负向追加**：`pirate hook hand, prosthetic hook, glowing hook, rope ladder, thin wire hook, anchor`

**三格 caption（Layout 用）**
- 主视 `Side view of a grappling hook made of light steel and translucent cloud sail membrane, J-shaped head at the upper right, barb on the inner tip, membrane on the hook back, two-segment shaft with one brass band, rope coil at the butt.`
- 俯视 `Top view: the width of the hook opening and its direction, the membrane reading as a thin sheet.`
- 后视 `Rear view: the outer face of the membrane on the hook back, the rope coil at the butt.`

**验收**：① 一眼读成钩 + 柄；② 倒刺可见；③ 膜的透光边可辨。

---

### 4.3 金星线 · 三套常规护甲

#### 4.3.1 钨钢压力服 `tungsten` 〔T2 · 护甲 4/8/6/3 · 韧性 2.0 · 耐压 100 atm〕

**版式** B · 画布 1536×1024

| 格 | 必须交代 |
|---|---|
| 正视（左） | 偏方头盔 + 前面一枚压力表、每条甲缝的双线包边、肩甲加强肋 + 六角铆钉（间距不等）、胸口铭牌划痕 |
| 侧视（中） | 肩甲外扩的厚度、后颈波纹管、胸甲前后厚度、膝肘铰接缝 |
| 背视（右） | 后颈波纹管的走向、背部甲缝双线包边、肩甲背面的肋条 |

- **体量与气质**：**宽肩、方头**的硬壳潜水服 —— 比其它套更"宽"，不是更"高"；封闭、工程师味、无装饰
- **配色纪律**：甲面灰为主 · 包边亮灰提亮 · 内衬压暗 · 黄铜宝石只做一点
- **必画**：每块甲片**双线包边**（外亮内暗，压力容器焊缝语言）；肩甲加强肋 + 间距不等的六角铆钉；后颈波纹管；胸口铭牌划痕；膝/肘铰接缝
- **禁画**：发光纹 · 布料飘带 · 毛领（原模型 `fur_collar` 改成**加厚密封领圈**，同 UV 改画法）
- **剪影对照**：与熔核套并排时，"宽"必须是本套的第一印象（熔核套是"高"）

```text
一张 Minecraft 模组护甲的三视图设定稿，画在三具无面部特征的哑光灰人台上，
横向三格等距并排：左格为正视图，中格为侧视图（脸朝画面左），右格为背视图，三格是同一套护甲的同一设计，同一缩放，共用一条隐形水平基线，脚底落在同一条线上，正交投影，机位水平，没有透视缩短，纯色浅灰背景，棚拍均匀打光，主光来自左上，柔和环境补光，画面里没有任何文字、编号、字母、箭头、尺寸线、色卡、边框和水印，
主体：一套能顶住极高气压的硬壳压力服，头盔偏方形、正面嵌一枚小圆形压力表，后颈接着几节波纹管；每块甲片边缘都是双线包边、外面一档亮里面一档暗，像压力容器的焊缝；肩甲明显外扩并带加强肋与间距不等的六角铆钉；胸口一块小铭牌上有几道划痕；肘与膝是断开的铰接缝；领口是一圈加厚的密封领圈而不是毛领。
气质：宽肩方头、封闭、无装饰的工程师装备，第一印象是宽而不是高，读起来像能密封的容器。
配色纪律：甲面灰是主色，包边用亮灰提亮，内衬压暗，黄铜色只做一颗小小的宝石点缀，全身不发光。
材质与风格：方块化工业硬表面设计，哑光材质，金属面至少三阶明度，高光只留在左上缘，2D 平涂三阶赛璐璐上色，边缘干净锐利，缩到四分之一大小仍能看出双线包边与波纹管。
```

**负向追加**：`fur collar, cloth cloak, glowing trim, sci-fi neon lines, astronaut bubble helmet, cape, visible face`

**三格 caption（Layout 用）**
- 正视 `Front view of a hard-shell pressure suit on a featureless mannequin: squared helmet with a small round gauge on the front, double-line edging on every plate, broad pauldrons with ribs and uneven hex rivets, small nameplate with scratches on the chest.`
- 侧视 `Side view, facing left: the depth of the widened pauldrons, the bellows at the nape, the chest and back thickness, the open hinge seam at the knee.`
- 背视 `Back view: the bellows running down the nape, the double-line edging on the back plates, the ribbing on the back of the pauldrons.`

**验收**：① 双线包边处处成立；② 波纹管可数；③ 与熔核套并排能立刻分出宽/高。

---

#### 4.3.2 云行者 `cloud_walker` 〔T2 · 轻甲 · 护甲 3/6/5/2 · 韧性 1.0 · 滑翔〕

**版式** B · 画布 1536×1024

| 格 | 必须交代 |
|---|---|
| 正视（左） | 肩后展开的翼膜、膜骨、膜面透光边、头巾两条长度不等的尾带、胸甲上板下布、袍摆锯齿下缘 |
| 侧视（中） | 翼膜的薄与后掠角、头巾尾带的走向、袍摆前后长度差 |
| 背视（右） | 翼膜外表面与膜骨根部、头巾结、袍摆一侧略短 |

- **体量与气质**：**展翼**的轻甲 —— 比其它套更"薄"、更"布"；有风感、轻，绝不像重甲
- **配色纪律**：甲面蓝灰 + 布料深蓝为主 · 包边云白提亮 · 强调青**只在膜缘与宝石**
- **必画**：翼膜的膜骨与透光边；两条不等长头巾尾带；锯齿下缘（一侧略短）；金属件细包边（轻量化，不画厚焊缝）
- **禁画**：厚板甲 · 重金属铆钉阵列 · 天使羽毛 · 斗篷

```text
一张 Minecraft 模组护甲的三视图设定稿，画在三具无面部特征的哑光灰人台上，
横向三格等距并排：左格为正视图，中格为侧视图（脸朝画面左），右格为背视图，三格是同一套护甲的同一设计，同一缩放，共用一条隐形水平基线，脚底落在同一条线上，正交投影，机位水平，没有透视缩短，纯色浅灰背景，棚拍均匀打光，主光来自左上，柔和环境补光，画面里没有任何文字、编号、字母、箭头、尺寸线、色卡、边框和水印，
主体：一套用云帆膜和轻钢骨架拼成的滑翔轻甲，背后一对薄膜翼从肩后向后展开、每片膜上有放射状膜骨、膜的外缘是一条比膜本身亮一档的透光边；头上是头巾，脑后垂下两条长度不等的尾带；胸甲上部是轻钢板、下部是布，布的部分连到带锯齿下缘的袍摆，一侧比另一侧短一截；所有金属件只有细包边，没有厚焊缝。
气质：横向展开的翼膜是最大特征，整套薄、轻、有风感，读起来像布与骨架而不是板甲。
配色纪律：甲面蓝灰与布料深蓝是主色，包边用云白提亮，强调青只出现在膜缘与宝石上，其余部分不发光。
材质与风格：方块化工业硬表面设计，哑光材质，金属面至少三阶明度，高光只留在左上缘，2D 平涂三阶赛璐璐上色，边缘干净锐利，缩到四分之一大小仍能读出翼膜与锯齿袍摆。
```

**负向追加**：`heavy plate armor, rivet array, angel feather wings, cape, thick welding seam, glowing whole body, knight armor`

**三格 caption（Layout 用）**
- 正视 `Front view of a glider light armor on a featureless mannequin: translucent membrane wings spreading back from the shoulders with radiating ribs and one bright rim edge, hood with two tail bands of unequal length, chest plate above cloth below, sawtooth robe hem.`
- 侧视 `Side view, facing left: the membrane reads as a thin sheet with a swept-back angle, the hood tails trail behind, the hem is shorter at the front.`
- 背视 `Back view: the outer face of the membrane, the roots of the ribs, the hood knot, one side of the hem slightly shorter.`

**验收**：① 与钨钢并排能立刻分出轻/重；② 膜缘透光边在缩略图上可辨；③ 两条头巾尾带不等长。

---

#### 4.3.3 熔管重甲 `tube_heavy` 〔T4 · 护甲 5/9/7/4 · 韧性 3.0 · 击退抗性 0.2 · 耐压 120 atm〕

**版式** B · 画布 1536×1024

| 格 | 必须交代 |
|---|---|
| 正视（左） | 面甲两条眼缝（**缝里是黑洞，不发光**）、胸前焦黑烧伤区、肩前后各几根短刺 + 加强座、竖向排酸沟槽若干处连续 |
| 侧视（中） | 肩刺前后分布、短披风的下垂与熔损毛边、沟槽深度 |
| 背视（右） | 披风背面的烧熔缺口、肩刺背面、背部沟槽走向 |

- **体量与气质**：**带刺、带短披风**的重板甲 —— 比熔核套更脏更凶；粗野、破损、有熔痕
- **配色纪律**：甲面热变色铁 + 暗部为主 · 硫金只做包边 · 披风暗红 · 强调橙**只做缝与孔**，宝石极小
- **必画**：竖向排酸沟槽（槽底比甲面暗两档、多处连续）；肩刺**根部有加强座**、长短不等；胸前不规则烧伤区；披风下缘熔损缺口；两条黑洞式眼缝
- **禁画**：漂亮完整的披风 · 等长对称的刺 · 眼缝透红光 · 干净无伤的甲面
- **剪影对照**：与熔核套并排时，它是"最脏"的那件

```text
一张 Minecraft 模组护甲的三视图设定稿，画在三具无面部特征的哑光灰人台上，
横向三格等距并排：左格为正视图，中格为侧视图（脸朝画面左），右格为背视图，三格是同一套护甲的同一设计，同一缩放，共用一条隐形水平基线，脚底落在同一条线上，正交投影，机位水平，没有透视缩短，纯色浅灰背景，棚拍均匀打光，主光来自左上，柔和环境补光，画面里没有任何文字、编号、字母、箭头、尺寸线、色卡、边框和水印，
主体：一套在熔岩管道里干活的重板甲，最脏最凶的一套；面甲是平的、上面两条窄眼缝，缝里是纯黑没有光；肩甲前后各插几根短刺、每根都有自己的加强座，刺的长短明显不同；胸甲上有一大块不规则的焦黑烧伤痕迹；全身甲面有竖向的排酸沟槽，槽底比甲面暗两档、多处连续下来；背后一件短披风，下缘有几处被烧熔的缺口。
气质：粗野、破损、带刺，比同族其它套更脏更凶；体积压身，读起来是在极端环境里干过活的甲。
配色纪律：热变色铁与暗部是主色，硫金只做包边，披风是暗红，强调橙只出现在缝隙与损坏处，宝石极小，眼缝完全不发光。
材质与风格：方块化工业硬表面设计，哑光材质，金属面至少三阶明度，高光只留在左上缘，2D 平涂三阶赛璐璐上色，边缘干净锐利，缩到四分之一大小仍能数出肩刺与披风缺口。
```

**负向追加**：`clean new cape, symmetric equal spikes, glowing eyeslits, polished armor, knight armor, angel wings, pristine metal`

**三格 caption（Layout 用）**
- 正视 `Front view of a brutal tunnel plate armor on a featureless mannequin: flat faceplate with two black eyeslits, uneven short spikes with reinforced sockets on each pauldron, a large irregular scorched patch on the chest, vertical acid drainage grooves running down the plates.`
- 侧视 `Side view, facing left: the front-to-back spread of the shoulder spikes, the short torn cape hanging with melted notches, the channel depth.`
- 背视 `Back view: melted notches along the cape hem, the backs of the shoulder spikes, the drainage channels continuing down the back plates.`

**验收**：① 排酸沟槽可读；② 披风有熔损缺口；③ 比熔核套更脏；④ 眼缝是黑洞。

---

### 4.4 金星线 · 终局护甲

#### 4.4.1 启明星战甲 `phosphoros` 〔T5 · 护甲 5/10/8/4 · 韧性 4.0 · 耐压 130 atm · ★基准样板〕

**版式** B · 画布 1536×1024

| 格 | 必须交代 |
|---|---|
| 正视（左） | 双光环（外环断几处 / 内盘刻八芒）、冠的两只角不等高、菱形胸宝 + 四爪金托、护腕刻痕数量不同、腰垂小宝石 |
| 侧视（中） | 光环的厚度与离头距离、披肩前缘、腰垂与膝宝的层次、披肩内衬的黎明玫瑰色只露一道窄边 |
| 背视（右） | 光环背面、披肩后的长度、腰垂背片、膝宝背面 |

- **体量与气质**：**带披风、带头冠光环**的礼仪重甲 —— 金为主、结构为骨、光只做点缀；终局感来自"整"与"贵"
- **配色纪律**：金四阶占绝大多数 · 黎明玫瑰做内衬与布 · 天青**只在光环与宝石** · 宝石红极小
- **必画**：外环断开几处（对应八芒星的缺口）；内盘刻八芒；冠两角不等高；菱形胸宝 + 四爪 + 细金托；两护腕刻痕数不同；腰垂与膝宝各一颗更小的宝石；披肩内衬只在侧面露一道窄边
- **禁画**：全身发光 · 三色平摊（需求案 R2 红线）· 青紫混用 · 变成天使或教会骑士

```text
一张 Minecraft 模组护甲的三视图设定稿，画在三具无面部特征的哑光灰人台上，
横向三格等距并排：左格为正视图，中格为侧视图（脸朝画面左），右格为背视图，三格是同一套护甲的同一设计，同一缩放，共用一条隐形水平基线，脚底落在同一条线上，正交投影，机位水平，没有透视缩短，纯色浅灰背景，棚拍均匀打光，主光来自左上，柔和环境补光，画面里没有任何文字、编号、字母、箭头、尺寸线、色卡、边框和水印，
主体：一套前文明遗落的礼仪重甲，金色为主；头部后面浮着双层光环，外环在几个位置断开，内环是一块刻着八道放射线的圆盘；头盔顶上一对尖角、左右不等高；胸甲中心一颗菱形宝石、四只金爪扣住、四周一圈细金托；两件护腕上的刻痕数量不同；腰下垂着甲片、每片一颗小宝石，膝部也各镶一颗更小的宝石；披肩宽厚，内衬是黎明玫瑰色的布，只在侧面露出一道窄边。
气质：完整、贵重、有仪式感，金色压倒性地占主导；体积稳重但不臃肿，读起来是终局那套。
配色纪律：金的几个明度阶占绝大多数，黎明玫瑰只做内衬与布面，天青只出现在光环与宝石上，宝石红极小，甲面本身不发光。
材质与风格：方块化工业硬表面设计，哑光材质，金属面至少三阶明度，高光只留在左上缘，2D 平涂三阶赛璐璐上色，边缘干净锐利，缩到四分之一大小仍能数出光环的断开处与两只不等高的角。
```

**负向追加**：`full body glow, three equal colours, purple accent, angel wings, church knight, magic rune circle, floating gems, neon cyan armor, halo made of light`

**三格 caption（Layout 用）**
- 正视 `Front view of a ceremonial gold aegis on a featureless mannequin: double halo behind the head, outer ring broken in places and an inner disc engraved with eight rays, two horns of unequal height, diamond chest gemstone held by four gold claws, bracers with different numbers of engraved lines.`
- 侧视 `Side view, facing left: the halo thickness and its distance from the head, the front edge of the shoulder mantle, the layered tassets and knee gems, a thin dawn-rose lining visible at the mantle edge.`
- 背视 `Back view: the reverse of the halo, the long mantle down the back, the rear tasset plates, the small gems behind the knees.`

**验收**：① 金压倒性地占主导；② 天青只出现在光环/宝石；③ 光环断开处可数；④ 与熔管重甲并排能读出"终局"。

---

### 4.5 附录 A · 月球线武器（需求案未覆盖，本文补）

#### `moon_deer_axe` · 梦眠月斧 〔2200 耐久 · 速度 9.5 · +4.5 · 附魔性 22〕

> 需求案 v1.0 明确把月球线排除在外，所以这件**本文第一次给设计**。设定依据全部来自代码与月球线资源：
> 斧由**梦眠月鹿的鹿角**锻成（`MoonDeerAxeItem` 注释）、持有者常驻抗性、命中时迸射 `END_ROD` 月华粒子。

**版式** A1 · 画布 1536×768

| 格 | 必须交代 |
|---|---|
| 主视（左） | 月牙形斧刃 + 内弧的月华细线、刃根被**分叉鹿角**包住、角尖无刃上翘、月华木柄、月银箍、柄首鹿角角基 |
| 侧视（中） | 斧刃的薄、鹿角包刃的包覆深度、柄的直径 |
| 背视（右） | 斧刃背面（月华细线从背面是断续的）、鹿角包背面的分叉走向、柄首角基的断面 |

- **体量与气质**：单手斧 —— **刃大柄短**，木质与骨质压过金属；冷、静、月下器物的气质（与火星巨剑的"热"对撞）
- **配色纪律**：月华木 + 鹿角骨为主 · 月银做箍 · 月华青**只在斧刃内弧那道线** · 梦眠紫极小
- **必画**：月牙刃 + 内弧月华线（断续，不许连成整条）；**分叉**鹿角包刃（鹿角是结构件，不是装饰贴片）；木纹顺柄方向；月银箍；柄首角基断面（可见髓腔）
- **禁画**：发光整刃 · 弯月魔法符号 · 羽毛/丝带 · 银白圣斧 · 鹿头雕塑

```text
一张 Minecraft 模组武器装备的三视图设定稿，
横向三格等距并排：左格为正视图，中格为侧视图，右格为后视图，三格是同一件武器的同一设计，同一缩放，共用一条隐形水平基线，正交投影，机位水平，没有透视缩短，纯色浅灰背景，棚拍均匀打光，主光来自左上，柔和环境补光，画面里没有任何文字、编号、字母、箭头、尺寸线、色卡、边框和水印，
主体：一把用梦眠月鹿的鹿角锻成的单手斧，斧刃是月牙形，内弧上有一条断续的淡青月华细线；刃根被一段分叉的鹿角紧紧包住，鹿角的分叉从刃背向后上方翘出一截、没有开刃；斧柄是月华木、木纹顺着柄方向；中段两道月银箍，柄首收在一段鹿角角基上、断面能看到细小的髓腔。
气质：刃大柄短的单手斧，木与骨压过金属；冷、静、像月亮那边的器物，与火焰系武器形成对照。
配色纪律：月华木与鹿角骨是主色，月银只做箍，月华青只出现在斧刃内弧那道细线上，梦眠紫只做很小的暗部，整刃不发光。
材质与风格：方块化工业硬表面设计，哑光材质，金属面至少三阶明度，高光只留在左上缘，2D 平涂三阶赛璐璐上色，边缘干净锐利，缩到四分之一大小仍能读出月牙刃与分叉鹿角。
```

**负向追加**：`glowing whole blade, crescent moon symbol, magic rune, feather, ribbon, holy silver axe, deer head sculpture, neon cyan`

**三格 caption（Layout 用）**
- 主视 `Front view of a hand axe forged from a moon deer antler: crescent blade with a broken pale-cyan glow line along the inner curve, a forked antler clamping the blade root and sweeping up without an edge, moon-wood haft with grain, two silver bands, antler base at the butt with a visible marrow cavity.`
- 侧视 `Side view: the thinness of the blade, how deep the antler clamps the root, the diameter of the haft.`
- 背视 `Back view: the reverse of the blade where the glow line reads as broken dashes, the fork of the antler from behind, the cross-section of the butt.`

**验收**：① 月牙刃 + 分叉鹿角两个识别点都在；② 月华线是断续的；③ 斧柄有木纹；④ 与火星巨剑并排时能读出"冷/静"对"热/躁"。

> **待登记**：本卡的月华木 / 鹿角骨 / 月华青 / 梦眠紫四色尚未进需求案 §7A，正式开工前补一行登记。

---

### 4.6 附录 B · 随身装备两件（只需两张小图）

这两件不是甲也不是武器，三视图按"器械"口径出，格数同样三格，尺寸可以降到 1024×512（仍是 16 的倍数，0.5 MP 在模型可外推范围内）。

#### B1. `sandstorm_goggles` · 沙尘暴护目镜 〔UNCOMMON · 占头盔位 · 耐久 220〕

- **三格**：正视（正面朝观众）/ 侧视 / 俯视（从上往下看两只镜片的张开角度）
- **体量与气质**：**横向铺开的飞行眼镜**，窄而宽；野外作业护具，不是盔甲
- **必画**：两片琥珀镜片各一道斜高光；右镜片一道划痕（不对称）；鼻梁连接件；两侧硫化革绑带末端金属扣
- **禁画**：整圈发光镜片 · 科幻 HUD 刻度 · 潜水镜造型

```text
一张 Minecraft 模组装备的三视图设定稿，横向三格等距并排：左格为正视，中格为侧视，右格为俯视，三格是同一件护目镜的同一设计，同一缩放，正交投影，没有透视缩短，纯色浅灰背景，棚拍均匀打光，主光来自左上，画面里没有任何文字、编号、字母、箭头、尺寸线、色卡、边框和水印，
主体：一副用于沙尘环境的飞行护目镜，黄铜镜框里两片琥珀色镜片，一片镜片上有一道细划痕；中间一个窄鼻梁连接件；两侧是硫化革绑带，末端各一个金属扣。
气质：横向铺开、窄而宽的一副眼镜，读起来是野外作业护具而不是盔甲配件。
配色纪律：黄铜框与硫化革是主色，琥珀镜片是画面唯一的亮点但不整圈发光，高光只在镜片左上角一点。
材质与风格：方块化工业硬表面设计，哑光材质，2D 平涂三阶赛璐璐上色，边缘干净锐利，缩到四分之一大小仍是护目镜。
```

**负向追加**：`glowing lenses, sci-fi HUD, diving mask, sunglasses, helmet, glowing rim, hologram`

**三格 caption（Layout 用，1024×512 时 bbox 按 §1.3 的换算规则自己缩）**
- 正视 `Front view: two amber lenses in brass frames, a narrow nose bridge between them, leather straps running to both sides with metal buckles at the ends, one thin scratch on one lens.`
- 侧视 `Side view: the depth of the brass frame, the strap leaving the frame horizontally, the buckle.`
- 俯视 `Top view: the splay angle of the two lenses and the gap at the nose bridge.`

#### B2. `portable_cooler` · 便携式冷却器 〔工具 · 冷却枪 · 不可堆叠〕

- **三格**：主视（侧面轮廓，喷头朝右）/ 俯视 / 后视
- **体量与气质**：**短枪管 + 侧挂小罐**的手持工具；冷、干净、工程感，工具感大于武器感
- **必画**：侧面圆形压力表（指针指向一角）；半透明液位窗（内有液面线，与压力表**不在同一水平线**）；枪口三孔喷头；握把防滑纹
- **禁画**：火焰色 · 整枪发光 · 科幻能量枪

```text
一张 Minecraft 模组装备的三视图设定稿，
横向三格等距并排：左格为主视图（侧面轮廓，喷头朝右），中格为俯视图，右格为后视图，三格是同一件器械的同一设计，同一缩放，共用一条隐形水平基线，正交投影，机位水平，没有透视缩短，纯色浅灰背景，棚拍均匀打光，主光来自左上，柔和环境补光，画面里没有任何文字、编号、字母、箭头、尺寸线、色卡、边框和水印，
主体：一支手持高压冷却枪，枪身中段并排两只小罐、前段一根短枪管、枪口一个多孔喷头；侧面一只圆形压力表，指针指向一角；枪身下方一扇窄长的半透明液位窗，窗内有液面线，位置与压力表错开；握把上有几道防滑纹。
气质：短枪管配侧挂小罐的手持工具，冷、干净、工程感强，比武器更像维修设备。
配色纪律：钢是主色，抛光件提亮，压力表青只有一点，冷雾白只做液位与喷口附近的冷气，铜阀压暗，画面里没有任何暖色。
材质与风格：方块化工业硬表面设计，哑光材质，金属面至少三阶明度，高光只留在左上缘，2D 平涂三阶赛璐璐上色，边缘干净锐利，缩到四分之一大小仍能看出表与液位窗。
```

**负向追加**：`fire, flame, warm orange, sci-fi blaster, energy weapon, glowing whole gun, water splash, frost everywhere`

**三格 caption（Layout 用）**
- 主视 `Side view of a handheld high-pressure cooling gun, nozzle to the right: two small tanks mounted alongside the mid body, one round gauge with the needle at an angle, a narrow translucent level window set lower than the gauge, grip grooves.`
- 俯视 `Top view: the twin tanks read as two circles, the short barrel on the centre line, the nozzle holes.`
- 后视 `Rear view: the tank end caps, the grip end face, the hose sockets.`

---

## 5. 一致性流水线（同一套甲 / 同一族武器怎么不跑偏）

### 5.1 三条硬办法

| 办法 | 做法 | 解决什么 |
|---|---|---|
| **基准件先行** | 每族先只出一件：火星族 = 融核巨剑，护甲族 = **启明星套**（需求案已定为样板） | 后面所有同族件都以它当"母版描述" |
| **seed 锁定** | 母版定稿后把 seed 记进交付回复；同族后续件**沿用同一 seed**，只改主体段 | 光照、笔触、背景灰度不漂 |
| **只改一格** | 三格里崩了一格时，**只改那一格的 caption / 那一句**，其余一个字不动 | 避免"改一处全盘重画" |

> 提示词里没有数字之后，**同族之间的一致性反而更好控**：模型只被"身份 + 气质"约束，不会因为换了一个
> 百分比就把体量关系整体改掉。

### 5.2 同一套护甲的四件怎么拆

- **一张三视图穿全套**（本文 §4 的写法）是母版；
- 四件单品的 16×16 图标与 64×32 穿戴层**不靠三视图直接降采样**，按需求案 §3 的像素规范重画；
- 单件需要单独一张设定稿时，把三格的内容换成"单件 + 45° + 背面"（这里允许偏离三视图版式，因为单件没有厚度问题）。

### 5.3 出图与留档

| 项 | 规定 |
|---|---|
| 存放 | `docs/previews/turnaround/<注册名>_3view_v1.png`（三格一张原图，不裁不拼） |
| 单格 | 需要单格时另存 `<注册名>_{front,side,back}_v1.png` |
| 记录 | 每张图在交付回复里写：seed、steps、cfg、sampler、画布尺寸、用的哪个模板（T2I / Layout） |
| 版本 | 设计改动就 `v2`，**不覆盖 v1**（建模对不上时要回头比） |

---

## 6. 验收清单

### 6.1 版式（不过不许进建模）

- [ ] 正好三格，等宽等高，格间留白一致
- [ ] 三格同一缩放，主体高度接近（差 ≤3%）
- [ ] 共用一条隐形基线（脚底 / 握把尾端在同一条线上）
- [ ] 正交投影，无透视缩短、无机位倾斜
- [ ] 纯色浅灰背景，无地面投影、无场景物件
- [ ] 三格光照方向一致（主光左上）
- [ ] **全图零文字**：无编号、无字母、无箭头、无尺寸线、无色卡、无边框、无水印
- [ ] 三格是同一件东西：零件数、宝石数、绑带数一致

### 6.2 结构（对着卡片逐条打勾）

- [ ] 卡片"必画"的每一项都能在三格里找到（缺一件就重出那一格）
- [ ] 该不对称的地方确实不对称（护手两端 / 两只表 / 护腕刻痕 / 冠角高低 / 头巾尾带）
- [ ] **背视不是主视的镜像复制**（有真实差异：反面裂纹、背面结构、流苏在反侧）
- [ ] 侧视/俯视能读出厚度（不是把正面压扁）
- [ ] 发光只出现在卡片指定的位置，没有大面积色块

### 6.3 可用性（建模视角）

- [ ] 能读出**体块关系**：哪一头重、哪一段粗、厚度感在哪 —— 够 Blockbench 起手掐体块
- [ ] 能判断对称：正/背两格并排可见左右差异
- [ ] 能吸色：色块是实的，没有渐变噪声（否则吸出来是脏灰）
- [ ] 与同族其它件并排时，**剪影不重复**（高 / 宽 / 展翼 / 带刺 / 带披风五条至少两条不同）

> **验收不再要求"能直接量出 62% 这类数字"**（v1.1）：三视图定结构不定尺寸，真尺寸在 Blockbench 里拉。
> 只要"哪头重、哪段长、哪里薄"这三件事读得出来，它就够用了。

### 6.4 复现

- [ ] seed、steps、cfg、sampler、画布尺寸、模板名都记了
- [ ] 用了 Layout 的，JSON 原文留档（含三个 bbox）
- [ ] `enable_prompt_enhancer` 的状态记了（开着的话实际送的文本会变）

---

## 7. 与现有文档的分工

| 文档 | 管什么 | 本文怎么用它 |
|---|---|---|
| [EQUIPMENT-ART-BRIEF.md](EQUIPMENT-ART-BRIEF.md) | **设计意图与数值**：命题、剪影、必画 / 禁画、配色、尺寸、UV 布局、验收 | 本文的结构清单与配色从它来；**它的比值数字本文不用**（v1.1），只在建模阶段当参考 |
| [AI-PROMPT-PACK.md](AI-PROMPT-PACK.md) | **给 MJ / SDXL 的提示词**（双视图、3-4 视角、`--ar`、`896×1152`） | 画风锚点沿用；**参数与取景框架不适用**于 MOSS-Image（§3.1、§3.3） |
| [APHRODITE-ART-BRIEF.md](APHRODITE-ART-BRIEF.md) / [BOSS-ART-BRIEF.md](BOSS-ART-BRIEF.md) | **Boss 的三视图**版式规范（灰底 `#6C7078`、正交、同基线、无文字） | 本文 §1.2 的版式硬指标是它的收紧版；Boss 归 Boss，装备归本文 |
| [MODELING-CONVENTION.md](MODELING-CONVENTION.md) | Blockbench 建模的坐标 / 单位 / 导出约定 | 三视图出完就进它 |

### 7.1 建议出图顺序（对齐需求案的批次）

| 批 | 内容 | 张数 | 说明 |
|---|---|---|---|
| T1 | 启明星套（基准件）+ 融核巨剑（火星族基准） | 2 | 先定画风与光照，后面全靠它们当母版 |
| T2 | 熔核套 + 星核炮 | 2 | 与 T1 同 seed 出，验证"同族不跑偏" |
| T3 | 酸蚀矛 / 压力锤 / 雷羽弩 / 高压钻 | 4 | 四把 RARE，先用模板 25 步挑，再 50 步定 |
| T4 | 硫火喷射器 / 云钩 | 2 | 小件，可与 T3 并行 |
| T5 | 钨钢 / 云行者 / 熔管重甲 | 3 | 三套甲的剪影必须一眼分开（§4.3 各卡的"剪影对照"） |
| T6 | 梦眠月斧 + 护目镜 + 冷却器 | 3 | 附录 A / B，配色需回填需求案 §7A |

> 每批出完先过 §6 自检，再进 Blockbench。**不要**把三视图直接当贴图用 —— 它是量结构与配色的图纸，贴图仍走需求案的像素规范。

---

*本文 v1.1 · 2026-11-24 · 覆盖火星 2 武器 + 熔核套、金星 6 武器 + 4 套甲、月球梦眠月斧、随身装备 2 件。
**v1.1 改动**：全部提示词删掉长度/宽度/百分比/比例倍数（原"比例"段改为"气质"段），卡片里"量化比例"改为
"体量与气质"；保留版式硬指标、可数结构件与配色纪律 —— 前者管画面，后两者管身份。*
*MOSS-Image 侧的参数与格式依据：官方 [USERGUIDE](https://github.com/OpenMOSS/MOSS-Image/blob/main/USERGUIDE.md)
与 [ComfyUI 官方文档](https://github.com/comfy-org/docs/blob/main/zh/tutorials/image/moss-image/moss-image.mdx)。*
