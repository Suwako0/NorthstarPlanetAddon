# AI 绘图指令包 · 特殊建筑（金星遗迹 + 三座祭坛）

> 配套：[美术需求案](STRUCTURE-ART-BRIEF.md)。本文件是**喂给 AI 绘图软件的指令**，不是需求说明。
> 要**直接生成方块建筑**（进游戏）请用 [STRUCTURE-BUILDER-PROMPTS.md](STRUCTURE-BUILDER-PROMPTS.md)（mc-ai-builder 用的施工式提示词）。
> 每条 prompt 已内联统一风格锚点与环境句，**复制即用，不需要再拼别的句子**。
> 机器可读版本：`docs/ai-prompts/structure-prompts.json`；纯文本合集：`docs/ai-prompts/structure-prompts.txt`。
> 尺寸/材质/比例全部来自代码反查与方块贴图实测（见需求案 §0、§1.3）。

---

## 0. 怎么用

1. 找到结构的小节，**只复制 text 代码块里的那一段**，粘进软件。
2. **先看你的软件**：Midjourney / DALL·E 3 / SDXL / Flux 用「长版」；SD 1.5、NovelAI、老版 WebUI 用「短版」（长版会被 CLIP 在 77 token 处截断，后半段的尺寸与颜色全丢）。
3. 「负面词」在 Midjourney 里写成 `--no <内容>`；SD / ComfyUI 粘进 Negative 框；DALL·E 3 忽略即可。
4. 「参数」给的是建议值，按你用的软件取用。
5. 用国产中文绘图软件（即梦 / 可图 / 文心 / 通义 / Seedream）时用「中文版」那一段，它和长版是同一份内容。
6. 要换视角、换天气，用 §3 的**视图追加句**接在长版末尾，不必重写。
7. 要剪影测试：把出图统一压黑，8 座横排对照需求案 §1.2 的剪影指纹。

### 0.1 现实提醒（和装备包同一条）

AI 画不出可直接导入的 Minecraft 结构；这些建筑是 `RuinBuilder` 按坐标摆的方块，**不是 NBT**。
所以本文件的定位是「**把建筑意图压成 AI 能听懂的话**」，产出的图用来：

- 定**剪影与比例**（远景认得出是哪座）；
- 定**材质与配色**（哪个面用什么方块）；
- 给手搭 / `RuinBuilder` 追加参数（几格高、窗间距、柱数量）。

### 0.2 全部 11 条索引

| # | 结构 | 平台 | 类型 | 体量（格） | 画幅 | 所在节 |
|---|---|---|---|---|---|---|
| 1 | 云港遗迹 `cloud_harbor` | 金星 | 结构 | 21×15×21 | 16:9 | §1 |
| 2 | 酸泵站 `acid_pump_station` | 金星 | 结构 | 13×9×13 | 4:3 | §1 |
| 3 | 高压观测塔 `observation_tower` | 金星 | 结构 | 11×32×11 | 3:4 | §1 |
| 4 | 真菌苗圃 `fungal_nursery` | 金星 | 结构 | 11×5×11 | 4:3 | §1 |
| 5 | 坠毁飞艇 `crashed_airship` | 金星 | 结构 | 19×6×5 | 16:9 | §1 |
| 6 | 熔管城市 `lava_tube_city` | 金星 | 结构 | 41×15×41 | 16:9 | §1 |
| 7 | 启明星祭坛 `phosphoros_altar` | 金星 | 结构 | 9×6×9 | 1:1 | §1 |
| 8 | 梦眠祭坛 `dreamsleep_altar` | 月球 | 结构 | 7×3×7 | 1:1 | §2 |
| 9 | 阿瑞斯祭坛 `ares_altar` | 火星 | 方块 | 1×1×1 | 1:1 | §2 |
| A1 | 金星云海氛围 | 金星 | 氛围 | — | 16:9 | §2 |
| A2 | 月林夜间氛围 | 月球 | 氛围 | — | 16:9 | §2 |

---

## 1. 金星遗迹（7 座）

**共用的三条**（已内联进每条 prompt，改这里等于改整组）：
硫晶是**琥珀金**不是荧光绿；铜绿只从底部和接缝往上走；所有形体都是 1 格立方体。

### 01 · 云港遗迹 · `cloud_harbor`

**id** `cloud_harbor` ｜ **平台** 金星 ｜ **类型** 结构 ｜ **体量** 21×15×21 格 ｜ **画幅** 16:9

```text
stylized Minecraft voxel architecture concept art, blocky one-metre cube construction, isometric three-quarter view from above at 35 degrees, painted 2D illustration with 3-value cel shading, matte materials, crisp per-block surface detail, warm dusk key light from the upper left with soft ambient fill, the structure standing on a pale blue-grey cloud sea with no ground plane, empty overcast sky, no characters, no creatures, no text, no watermark, no logo, no UI, a 21 by 21 block square harbor platform with its four corners chamfered, floating above the cloud sea, a 6 block tall polished andesite parapet around the full edge with one square window slot every 5 blocks along its upper two thirds, four squat 2 by 2 corner pylons 9 blocks tall each capped by a single glowing amber sulfur crystal, a chunky 5 by 5 central watchtower 12 blocks tall with a 3 wide 2 tall doorway on the near face and a flat cloud-stone roof carrying one amber sulfur crystal as its beacon, a 1 block wide cut-copper inlay line running from the doorway straight to the platform edge, copper grate vents set low in the parapet, a pair of hanging lanterns on chains at each corner, one chest visible inside the doorway, every form is a 1-block cube grid, clean voxel edges, strong readable silhouette at a glance, whole structure fully inside the frame with generous margin, production concept art, consistent material language. Colors: cloud stone #586070 #788090 #8898a0, polished andesite grey, smooth stone floor, cut copper #c16d4f, sulfur crystal #a88018 to #f8e8a8, warm lantern #f0d78a, tinted glass dark grey.
```

**负面词**
```text
characters, players, creatures, mobs, text, watermark, signature, logo, ui elements, multiple unrelated buildings, duplicated structure, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, photorealistic stone, organic curved walls, smooth sculpted geometry, gothic cathedral, fantasy castle, sci-fi glass curtain wall, neon glow, lens flare, oversaturated, tilted horizon, vines covering walls, trees hiding the building, night scene, ground plane at the bottom, round dome, flagpole, banners, windows filling the whole wall, corner pylons taller than the central tower
```

**参数**：MJ `--ar 16:9 --style raw --quality 2` ｜ SDXL `1344x768, steps 32, cfg 6.5, sampler DPM++ 2M Karras` ｜ Flux `16:9, guidance 3.5, steps 30`

**短版**
```text
concept art, Minecraft voxel architecture, isometric above, blocky cubes, matte, dusk light, no text, no characters, 21x21 stone harbor platform on clouds, 6 block parapet with window slots, four corner pylons with amber crystals, 12 block central watchtower with one beacon, copper inlay, #586070 #c16d4f #a88018
```

**中文版**
```text
我的世界方块风建筑设计概念图，全部由 1 格立方体构成，四分之三俯视 35 度视角，三渲二平涂插画，哑光材质，左上暖色黄昏主光 + 柔和环境光，整座建筑悬在淡蓝灰云海之上，无地面、无天空细节，画面中没有任何人物、生物、文字、水印、UI，一座 21×21 格的方形港口平台，四角各切掉一格，平台上是 6 格高的磨制安山岩矮墙，墙身上三分之一每 5 格开一个方窗，四角各一座 2×2、9 格高的矮塔，塔顶各一颗发光的琥珀色硫晶，中央一座 5×5、12 格高的灯塔，正面有 3 格宽 2 格高的门洞，云岩平屋顶上立一颗硫晶信标，一条 1 格宽的切制铜镶线从门洞直通平台边缘，墙脚有铜格栅通风口，四角挂锁链灯笼，门内可见一只箱子，每处细节都必须是整齐的方块，边缘干净，剪影一眼可辨，整栋建筑完整入画并留边距，生产级概念设定图，统一的材质语言。配色：云岩 #586070 #788090 #8898a0、磨制安山岩灰、平滑石地面、切制铜 #c16d4f、硫晶 #a88018 到 #f8e8a8、暖灯笼 #f0d78a、深灰染色玻璃。负面：人物、生物、文字、水印、UI、多栋建筑、圆形穹顶、旗杆、旗帜、满墙开窗、角塔比中央灯塔高。
```

**调色板**：`#586070` · `#788090` · `#8898a0` · `#c16d4f` · `#a88018` · `#f8e8a8` · `#f0d78a`

**要点**：四角晶塔等距、中央塔高 ≈ 围墙两倍、地面一条铜线；四角必须能看出被切角。

---

### 02 · 酸泵站 · `acid_pump_station`

**id** `acid_pump_station` ｜ **平台** 金星 ｜ **类型** 结构 ｜ **体量** 13×9×13 格 ｜ **画幅** 4:3

```text
stylized Minecraft voxel architecture concept art, blocky one-metre cube construction, isometric three-quarter view from above at 35 degrees, painted 2D illustration with 3-value cel shading, matte materials, crisp per-block surface detail, warm dusk key light from the upper left with soft ambient fill, the structure standing on a shallow sulfuric acid flat on a high cloud plateau, thin acid mist at the base, no characters, no creatures, no text, no watermark, no logo, no UI, a 13 by 13 block square pump-station platform with chamfered corners, a low 4 block tall polished andesite parapet with one narrow window slot every 5 blocks, a stubby 5 by 5 control tower only 5 blocks tall at the centre capped with one amber sulfur crystal, two upright cylindrical pressure vessels on opposite sides of the open yard, the taller one exactly 2 blocks high and the other a single block, each a dark steel drum with two banded seams and one vertical glass sight window, a short pipe stub and a valve handwheel connecting one vessel toward the tower base, copper grate vents in the parapet foot, a chain and lantern at each corner, acid-green seepage stains only along the outer platform edge never on the metal, three chests scattered in the yard, every form is a 1-block cube grid, clean voxel edges, strong readable silhouette at a glance, whole structure fully inside the frame with generous margin, production concept art, consistent material language. Colors: smooth stone, polished andesite grey, pressure steel #282828 #484848 #889090, sulfur crystal #a88018 #f8e8a8, sulfuric acid #88e048 only in the surrounding liquid, moss #6a7a2f, lantern #f0d78a.
```

**负面词**
```text
characters, players, creatures, mobs, text, watermark, signature, logo, ui elements, multiple unrelated buildings, duplicated structure, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, photorealistic metal, organic curved walls, smooth sculpted geometry, gothic cathedral, fantasy castle, sci-fi glass curtain wall, neon glow, lens flare, oversaturated, tilted horizon, towering chimneys, steampunk pipe spaghetti, castle battlements, glowing green crystal, acid-green glow on the building itself
```

**参数**：MJ `--ar 4:3 --style raw --quality 2` ｜ SDXL `1152x896, steps 32, cfg 6.5, sampler DPM++ 2M Karras` ｜ Flux `4:3, guidance 3.5, steps 30`

**短版**
```text
concept art, Minecraft voxel architecture, isometric above, blocky, matte, dusk light, no text, no characters, 13x13 stone pump station in an acid flat, low 4 block parapet, small 5 block tower with amber crystal, two squat steel pressure vessels with sight windows, #484848 #a88018, acid only on the ground
```

**中文版**
```text
我的世界方块风建筑设计概念图，全部由 1 格立方体构成，四分之三俯视 35 度视角，三渲二平涂插画，哑光材质，左上暖色黄昏主光 + 柔和环境光，建筑立在高原上一片浅浅的硫酸滩上，脚下有稀薄酸雾，画面中没有人物、生物、文字、水印、UI，一座 13×13 格的方形酸泵站平台，四角切角，四周是 4 格高的磨制安山岩矮护堤，每 5 格开一个窄窗，中央一座 5×5、只有 5 格高的矮控制塔，塔顶一颗琥珀色硫晶，院子两侧各立一台深色钢制立式压力罐、一台 2 格高一台 1 格高（一高一低），罐身两道箍环加一道竖向玻璃液位窗，其中一台用短管与手轮阀连向塔基，护堤脚部有铜格栅通风口，四角各挂一只锁链灯笼，酸绿的渗漏痕只出现在平台外缘、绝不上到金属件，院子里散放三只箱子，每处细节必须是整齐的方块，边缘干净，剪影一眼可辨，整栋建筑完整入画并留边距，生产级概念设定图，统一的材质语言。配色：平滑石、磨制安山岩灰、压力罐钢 #282828 #484848 #889090、硫晶 #a88018 #f8e8a8、硫酸 #88e048 只存在于周围液体、苔藓 #6a7a2f、灯笼 #f0d78a。负面：人物、生物、文字、水印、UI、摩天烟囱、蒸汽朋克管道缠绕、城堡垛口、发绿光的晶体、建筑本体发绿光。
```

**调色板**：`#282828` · `#484848` · `#889090` · `#a88018` · `#88e048` · `#6a7a2f` · `#f0d78a`

**要点**：压力罐是"两个"而且有竖窗；围墙明显比人矮；酸绿只在液体里，建筑本体不发光。

---

### 03 · 高压观测塔 · `observation_tower`

**id** `observation_tower` ｜ **平台** 金星 ｜ **类型** 结构 ｜ **体量** 9×32×9 格 ｜ **画幅** 3:4

```text
stylized Minecraft voxel architecture concept art, blocky one-metre cube construction, three-quarter view from a low angle of 20 degrees so the full height fits, painted 2D illustration with 3-value cel shading, matte materials, crisp per-block surface detail, cold overcast key light from the upper left with soft ambient fill, the tower rising out of a cloud sea with no ground plane, low clouds drifting across its middle, no characters, no creatures, no text, no watermark, no logo, no UI, a very slender observation tower standing alone: an 11 by 11 block smooth stone plinth, a 7 by 7 chiseled stone brick shaft exactly 30 blocks tall rising perfectly straight with no taper, cut-copper reinforcement bands wrapping all four faces every 4 blocks so about seven bands can be counted from bottom to top, a 9 by 9 flat smooth stone observation deck cantilevered one block beyond the shaft at the very top, one amber sulfur crystal beacon standing at the deck centre, four chests arranged two by two at the base, chains and hanging lanterns at the plinth corners, a cold sea-lantern glow at the base doorway, every form is a 1-block cube grid, clean voxel edges, strong readable silhouette at a glance, whole structure fully inside the frame with generous margin, production concept art, consistent material language. Colors: chiseled stone brick grey, smooth stone, cut copper #c16d4f, sulfur crystal #a88018 to #f8e8a8, sea lantern #bfe8dc, warm lantern #f0d78a.
```

**负面词**
```text
characters, players, creatures, mobs, text, watermark, signature, logo, ui elements, multiple unrelated buildings, duplicated structure, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, photorealistic stone, organic curved walls, smooth sculpted geometry, gothic cathedral, fantasy castle, sci-fi glass curtain wall, neon glow, lens flare, oversaturated, tilted horizon, tapered spire, conical roof, helipad markings, glass greenhouse on top, external staircase, radio dish, antenna array
```

**参数**：MJ `--ar 3:4 --style raw --quality 2` ｜ SDXL `896x1152, steps 32, cfg 6.5, sampler DPM++ 2M Karras` ｜ Flux `3:4, guidance 3.5, steps 30`

**短版**
```text
concept art, Minecraft voxel architecture, low angle, blocky, matte, cold light, no text, no characters, slender stone tower 7 wide 30 tall with no taper, copper bands every 4 blocks, 9x9 deck wider than the shaft on top with one amber crystal, four chests at the base, #c16d4f #a88018
```

**中文版**
```text
我的世界方块风建筑设计概念图，全部由 1 格立方体构成，四分之三低角度仰视 20 度，让整座塔的高度完整入画，三渲二平涂插画，哑光材质，左上冷调阴天主光 + 柔和环境光，塔从云海中升起、没有地面，中段有薄云飘过，画面中没有人物、生物、文字、水印、UI，一座极细长的观测塔独占画面：11×11 格平滑石基座，7×7 格凿制石砖塔身整 30 格高、笔直无收分，四个立面每 4 格一道切制铜加固箍，从底到顶可数出约七道，塔顶一块 9×9 格平滑石观测平台、向外各挑出 1 格、明显比塔身宽，平台正中一颗琥珀色硫晶信标，基座摆四只箱子呈 2×2，基座四角挂锁链灯笼，底层门洞透出一点冷青海晶灯的光，每处细节必须是整齐的方块，边缘干净，剪影一眼可辨，整座塔完整入画并留边距，生产级概念设定图，统一的材质语言。配色：凿制石砖灰、平滑石、切制铜 #c16d4f、硫晶 #a88018 到 #f8e8a8、海晶灯 #bfe8dc、暖灯笼 #f0d78a。负面：人物、生物、文字、水印、UI、塔身收分成尖锥、圆顶、直升机坪标记、顶部玻璃花房、外挂楼梯、雷达天线。
```

**调色板**：`#8a8a8a` · `#a0a0a0` · `#c16d4f` · `#a88018` · `#f8e8a8` · `#bfe8dc`

**要点**：细长比 7:30 必须成立（塔高 ≥ 画面 70%）；铜箍可数 ≈7 道；顶盘比塔身宽且只有一颗信标。

---

### 04 · 真菌苗圃 · `fungal_nursery`

**id** `fungal_nursery` ｜ **平台** 金星 ｜ **类型** 结构 ｜ **体量** 11×5×11 格 ｜ **画幅** 4:3

```text
stylized Minecraft voxel architecture concept art, blocky one-metre cube construction, isometric view from above at 45 degrees, painted 2D illustration with 3-value cel shading, matte materials, crisp per-block surface detail, warm dusk key light from the upper left with soft ambient fill, the structure resting on a fungal forest floor with humidity haze, huge mushroom shapes dissolved in the background, no characters, no creatures, no text, no watermark, no logo, no UI, an 11 by 11 block square nursery platform with chamfered corners only 2 blocks thick, a 3 by 3 grid of 2 by 2 moss beds with the centre position empty so exactly eight beds ring the plate, each bed carrying a giant mushroom 3 blocks tall made of one stem block and one cap block, the caps alternating warm brown #8a6a4a and deep red #b02a20 in an unmistakable checker arrangement, a low cut-copper kerb edging every bed, one amber sulfur crystal standing where the ninth bed would be, a chain and lantern at each corner, two chests on the plain stone margin, this is the flattest and lowest building of the whole set, every form is a 1-block cube grid, clean voxel edges, strong readable silhouette at a glance, whole structure fully inside the frame with generous margin, production concept art, consistent material language. Colors: smooth stone, moss bed #6a7a2f, mushroom brown cap #8a6a4a, mushroom red cap #b02a20, mushroom stem pale cream, cut copper #c16d4f, sulfur crystal #a88018, lantern #f0d78a.
```

**负面词**
```text
characters, players, creatures, mobs, text, watermark, signature, logo, ui elements, multiple unrelated buildings, duplicated structure, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, photorealistic mushrooms, organic curved walls, smooth sculpted geometry, gothic cathedral, fantasy castle, sci-fi glass curtain wall, neon glow, lens flare, oversaturated, tilted horizon, real farm rows, dense crops hiding the beds, glossy fungus caps, fluorescent green mushrooms, tall buildings
```

**参数**：MJ `--ar 4:3 --style raw --quality 2` ｜ SDXL `1152x896, steps 32, cfg 6.5, sampler DPM++ 2M Karras` ｜ Flux `4:3, guidance 3.5, steps 30`

**短版**
```text
concept art, Minecraft voxel architecture, isometric above, blocky, matte, warm light, no text, no characters, flat 11x11 stone nursery with eight 2x2 moss beds in a 3x3 grid minus the centre, each with a 3 block giant mushroom, caps alternating brown #8a6a4a and red #b02a20, lanterns
```

**中文版**
```text
我的世界方块风建筑设计概念图，全部由 1 格立方体构成，四分之三俯视 45 度视角，三渲二平涂插画，哑光材质，左上暖色黄昏主光 + 柔和环境光，建筑坐在真菌森林地面、背景是虚化的巨型蘑菇林和潮湿雾，画面中没有人物、生物、文字、水印、UI，一座 11×11 格的方形苗圃平台，四角切角、只有 2 格厚，台面上 3×3 排列 2×2 的苔藓苗床、正中一格空着，因此正好八块苗床围成一圈，每块苗床上一株 3 格高的巨型蘑菇，由一格茎和一格伞帽组成，伞帽以暖棕 #8a6a4a 与深红 #b02a20 明确地交替成棋盘，每块苗床用一圈低矮的切制铜镶边，本该是第九块床的位置立一颗琥珀色硫晶，四角各挂一只锁链灯笼，石台边放两只箱子，这是全套里最矮最扁的一座，每处细节必须是整齐的方块，边缘干净，剪影一眼可辨，整栋建筑完整入画并留边距，生产级概念设定图，统一的材质语言。配色：平滑石、苔藓 #6a7a2f、棕伞帽 #8a6a4a、红伞帽 #b02a20、蘑菇茎浅米色、切制铜 #c16d4f、硫晶 #a88018、灯笼 #f0d78a。负面：人物、生物、文字、水印、UI、写实蘑菇、真实农田垄沟、作物把苗床盖住、油亮菌盖、荧光绿蘑菇、高层建筑。
```

**调色板**：`#6a7a2f` · `#8a6a4a` · `#b02a20` · `#c16d4f` · `#a88018` · `#f0d78a`

**要点**：八床可数（3×3 缺心）；红棕交替；是八座里最矮的一座。

---

### 05 · 坠毁飞艇 · `crashed_airship`

**id** `crashed_airship` ｜ **平台** 金星 ｜ **类型** 结构 ｜ **体量** 19×6×5 格 ｜ **画幅** 16:9

```text
stylized Minecraft voxel architecture concept art, blocky one-metre cube construction, three-quarter view from the front at 45 degrees and slightly above, painted 2D illustration with 3-value cel shading, matte materials, crisp per-block surface detail, cold storm rim light with a warm break in the clouds behind, no characters, no creatures, no text, no watermark, no logo, no UI, a wrecked airship lying across a storm cloud sea: a 15 block long, 5 block wide, 4 block tall capsule hull built of weathered cut copper, both ends tapering to a single block so the plan reads as a capsule, dark tungsten ribs exposed and countable every 3 blocks across the hull girth, a 2 by 2 black breach torn through the middle of the near side showing a dark empty interior, a broken tail section dragging behind and sinking two blocks into the cloud with its underside ripped open and jagged copper plates hanging loose, a few snapped cable stubs and torn pale canvas scraps still lashed to the hull top, absolutely no intact gas envelope only a bundle of torn rigging lines, the front third of the hull swallowed by cloud, three chests visible through the breach, every form is a 1-block cube grid, clean voxel edges, strong readable silhouette at a glance, whole structure fully inside the frame with generous margin, production concept art, consistent material language. Colors: weathered cut copper #6a9a88, tungsten #303830 #485850 #889890, torn canvas pale grey, breach pure black #14100e, cloud sea #8898a0.
```

**负面词**
```text
characters, players, creatures, mobs, text, watermark, signature, logo, ui elements, multiple unrelated buildings, duplicated structure, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, photorealistic metal, organic curved walls, smooth sculpted geometry, gothic cathedral, fantasy castle, sci-fi spaceship, neon glow, lens flare, oversaturated, tilted horizon, intact airship, round gas envelope balloon, zeppelin, propeller close up, clean cut break, fantasy sailing ship
```

**参数**：MJ `--ar 16:9 --style raw --quality 2` ｜ SDXL `1344x768, steps 32, cfg 6.5, sampler DPM++ 2M Karras` ｜ Flux `16:9, guidance 3.5, steps 30`

**短版**
```text
concept art, Minecraft voxel architecture, three-quarter view, blocky, matte, storm light, no text, no characters, 15 block capsule airship wreck of oxidized copper, dark ribs every 3 blocks, black 2x2 breach, broken tail dragging into the cloud with jagged plates, no balloon, #6a9a88 #303830
```

**中文版**
```text
我的世界方块风建筑/载具残骸概念图，全部由 1 格立方体构成，四分之三前侧视角、略微俯视，三渲二平涂插画，哑光材质，冷调雷暴边缘光 + 云层后一缕暖光，画面中没有人物、生物、文字、水印、UI，一艘坠毁的飞艇横躺在雷暴云海上：15 格长、5 格宽、4 格高的胶囊形船体，用氧化的风化切制铜建造，两端各收窄到一格，所以俯视是胶囊形，深色钨钢肋骨每隔 3 格横跨船身、可数出多根并外露，中段近侧船壁被撕开一个 2×2 的黑洞、能看到空荡发暗的内舱，尾部一段船体拖在后面、向云里下沉两格，其底面被撕开、参差的铜板向外翻卷，船顶还绑着几段断掉的缆索和破烂的浅灰帆布，绝不要完整的气囊、只留一束被撕裂的吊索，船头三分之一被云吞没，从破口能看到三只箱子，每处细节必须是整齐的方块，边缘干净，剪影一眼可辨，整艘残骸完整入画并留边距，生产级概念设定图，统一的材质语言。配色：风化切制铜 #6a9a88、钨钢 #303830 #485850 #889890、破帆布浅灰、破口纯黑 #14100e、云海 #8898a0。负面：人物、生物、文字、水印、UI、完好的飞艇、球状气囊、齐柏林飞艇、螺旋桨特写、整齐的断口、奇幻帆船。
```

**调色板**：`#6a9a88` · `#303830` · `#485850` · `#889890` · `#14100e` · `#8898a0`

**要点**：**必须坏**（断口要撕开感）；肋骨可数 ≥ 4 根；外壳是铜绿不是橙铜；尾部向云里沉。

---

### 06 · 熔管城市 · `lava_tube_city`

**id** `lava_tube_city` ｜ **平台** 金星 ｜ **类型** 结构 ｜ **体量** 41×15×41 格 ｜ **画幅** 16:9

```text
stylized Minecraft voxel architecture concept art, blocky one-metre cube construction, isometric view from high above at 45 degrees on a diagonal so all three tiers read at once, painted 2D illustration with 3-value cel shading, matte materials, crisp per-block surface detail, warm dusk key light from the upper left with soft ambient fill, the complex standing on a dark volcanic flat under a low ash haze, no characters, no creatures, no text, no watermark, no logo, no UI, a three-tier circular temple city, each tier a perfect disc stepping inward and upward: the outer disc 41 blocks across, the middle disc 29 blocks across raised 6 blocks, the inner disc 17 blocks across raised 12 blocks, every rim edged by a 2 block tall polished deepslate parapet broken by one gap every 6 blocks, four massive pylons standing clear of all three tiers at the four square corners, rooted in the dark ground and 14 blocks tall, each capped by one amber sulfur crystal, a ring of eight 3 block tall sulfur crystal columns evenly spaced around the middle tier, a 7 by 7 diamond-shaped pool of glowing sulfuric acid sunk into the middle tier floor, a stepped altar exactly 3 blocks tall at the very centre tipped with a sulfur crystal, one chest at each quadrant of every tier, copper grate vents and chain-hung lanterns set low along the walls, the whole mass reading as a stepped ziggurat of concentric discs, every form is a 1-block cube grid, clean voxel edges, strong readable silhouette at a glance, whole structure fully inside the frame with generous margin, production concept art, consistent material language. Colors: polished deepslate #4a4a4f, andesite grey, sulfur crystal #685010 #a88018 #f0e078 #f8e8a8, sulfuric acid #88e048 #b0e048, lantern #f0d78a, volcanic ground dark ash.
```

**负面词**
```text
characters, players, creatures, mobs, text, watermark, signature, logo, ui elements, multiple unrelated buildings, duplicated structure, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, photorealistic stone, organic curved walls, smooth sculpted geometry, gothic cathedral, fantasy castle, roman colosseum, round dome, neon glow, lens flare, oversaturated, tilted horizon, orange lava in the acid pool, glowing green crystals, three identical tiers, external grand staircase, throne statue
```

**参数**：MJ `--ar 16:9 --style raw --quality 2` ｜ SDXL `1344x768, steps 32, cfg 6.5, sampler DPM++ 2M Karras` ｜ Flux `16:9, guidance 3.5, steps 30`

**短版**
```text
concept art, Minecraft voxel architecture, high isometric, blocky, matte, dusk light, no text, no characters, three concentric circular temple tiers 41 then 29 then 17 blocks across stepping up 6 blocks each, deepslate parapets, four 14 block corner pylons with amber crystals, eight amber columns, glowing acid pool, #4a4a4f #a88018 #88e048
```

**中文版**
```text
我的世界方块风建筑设计概念图，全部由 1 格立方体构成，高机位四分之三俯视 45 度、对角线构图让三层同框，三渲二平涂插画，哑光材质，左上暖色黄昏主光 + 柔和环境光，整座建筑立在深色火山岩地面上、笼罩薄薄的火山灰雾，画面中没有人物、生物、文字、水印、UI，一座三层同心圆祭城，每一层都是正圆盘、逐层向内收缩并抬高：最外圆盘 41 格宽，中层圆盘 29 格宽、抬高 6 格，内层圆盘 17 格宽、再抬高 6 格共 12 格，每层边缘各有一圈 2 格高的磨制深板岩矮墙、每 6 格留一个缺口，四根粗壮的立柱在正方形四角、独立于三层之外，各 14 格高、顶端一颗琥珀色硫晶，中层台面上八根 3 格高的硫晶柱沿圆周均匀分布，中层地面正中嵌一个 7×7 的菱形硫酸池、酸绿发光，最中心是一座正好 3 格高的阶梯祭台、顶部一颗硫晶，每层四个方向各放一只箱子，墙脚有铜格栅通风口与锁链灯笼，整体读作同心圆递收的金字塔祭城，每处细节必须是整齐的方块，边缘干净，剪影一眼可辨，整座建筑完整入画并留边距，生产级概念设定图，统一的材质语言。配色：磨制深板岩 #4a4a4f、安山岩灰、硫晶 #685010 #a88018 #f0e078 #f8e8a8、硫酸 #88e048 #b0e048、灯笼 #f0d78a、火山灰地面。负面：人物、生物、文字、水印、UI、圆形竞技场、罗马斗兽场、圆顶、酸池里是橙红熔岩、发绿光的晶体、三层一样大、外挂大楼梯、王座雕像。
```

**调色板**：`#4a4a4f` · `#685010` · `#a88018` · `#f0e078` · `#88e048` · `#f0d78a`

**要点**：三层半径 20/14/8 的递收一眼可读；四角立柱高于所有层；硫晶柱 8 根；酸池必须**酸绿不是熔岩**。

---

### 07 · 启明星祭坛 · `phosphoros_altar`

**id** `phosphoros_altar` ｜ **平台** 金星 ｜ **类型** 结构 ｜ **体量** 9×6×9 格 ｜ **画幅** 1:1

```text
stylized Minecraft voxel architecture concept art, blocky one-metre cube construction, isometric view from above at 55 degrees, painted 2D illustration with 3-value cel shading, matte materials, crisp per-block surface detail, high-altitude sunlight raking in through the crystal columns from the upper right with soft ambient fill, the altar floating at the very top of the sky with nothing but pale blue sky and a thin lower cloud layer below it, no characters, no creatures, no text, no watermark, no logo, no UI, a small ceremonial altar that is the highest thing in the world: a plain 9 by 9 block smooth stone plate with chamfered corners, then two cut-copper step rings shrinking to 7 by 7 and 5 by 5 one block of height each so exactly three steps read from above, four identical sulfur crystal columns exactly 4 blocks tall standing at the four corners of the 7 by 7 step and clearly rising above it, a single 3 block tall core at dead centre made of a sulfur crystal block, a cut copper block and another sulfur crystal block stacked vertically, one small chest sitting on the third step, absolutely no other structure anywhere in the frame, amber light beams falling through the crystal columns onto the copper steps, every form is a 1-block cube grid, clean voxel edges, strong readable silhouette at a glance, whole structure fully inside the frame with generous margin, production concept art, consistent material language. Colors: smooth stone, cut copper #c16d4f, sulfur crystal #a88018 #f0e078 #f8e8a8, sky pale blue #bcd0dc, cloud #d8e2e8.
```

**负面词**
```text
characters, players, creatures, mobs, text, watermark, signature, logo, ui elements, multiple unrelated buildings, duplicated structure, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, photorealistic stone, organic curved walls, smooth sculpted geometry, gothic cathedral, fantasy castle, sci-fi glass curtain wall, neon glow, lens flare, oversaturated, tilted horizon, statues, archways, railings, floating orb core, more than three steps, surrounding buildings, trees, ground plane
```

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras` ｜ Flux `1:1, guidance 3.5, steps 30`

**短版**
```text
concept art, Minecraft voxel architecture, high isometric, blocky, matte, sky light, no text, no characters, a small altar alone in the sky, 9x9 stone plate and two copper steps shrinking to 7 and 5 blocks, four 4 block amber crystal columns at the corners, a central 3 block crystal-copper-crystal core, #c16d4f #a88018
```

**中文版**
```text
我的世界方块风建筑设计概念图，全部由 1 格立方体构成，高机位四分之三俯视 55 度，三渲二平涂插画，哑光材质，高空阳光从右上穿过晶柱斜射下来 + 柔和环境光，祭坛悬在天空最高处，除了淡蓝天空和下方一层薄云什么都没有，画面中没有人物、生物、文字、水印、UI，一座小巧的仪式祭坛：9×9 格平滑石台面、四角切角，其上两级切制铜台阶分别收成 7×7 与 5×5、每级 1 格高，所以从上往下正好读作三级台阶，四根完全相同的 4 格高硫晶柱立在 7×7 台阶的四角、明显高出台阶面，正中心一座 3 格高的核心，自下而上由硫晶块、切制铜块、硫晶块叠成，第三级台阶上放一只小箱子，画面里绝对没有任何别的建筑，琥珀色光束穿过晶柱洒在铜台阶上，每处细节必须是整齐的方块，边缘干净，剪影一眼可辨，整座祭坛完整入画并留边距，生产级概念设定图，统一的材质语言。配色：平滑石、切制铜 #c16d4f、硫晶 #a88018 #f0e078 #f8e8a8、天空淡蓝 #bcd0dc、云 #d8e2e8。负面：人物、生物、文字、水印、UI、雕像、拱门、围栏、悬浮球体核心、超过三级台阶、周围建筑、树木、地面。
```

**调色板**：`#a0a0a0` · `#c16d4f` · `#a88018` · `#f0e078` · `#f8e8a8` · `#bcd0dc`

**要点**：三级台阶（9/7/5）；四柱等长且高于台阶；中央是**三格竖塔**不是悬浮球；画面里只有它一座。

---

## 2. 月球 · 火星 · 氛围（4 条）

### 08 · 梦眠祭坛 · `dreamsleep_altar`

**id** `dreamsleep_altar` ｜ **平台** 月球 ｜ **类型** 结构 ｜ **体量** 7×3×7 格 ｜ **画幅** 1:1

```text
stylized Minecraft voxel architecture concept art, blocky one-metre cube construction, three-quarter view from a low angle of 15 degrees, painted 2D illustration with 3-value cel shading, matte materials, crisp per-block surface detail, cold indigo night with a single cyan rune glow as the only warm-adjacent light, deep shadow with soft ambient fill, no characters, no creatures, no text, no watermark, no logo, no UI, a tiny moon shrine exactly 7 blocks wide and 3 blocks tall that reads as a shrine lantern rather than a building: four stripped pale-violet log posts at the corners, horizontal log beams joining their tops, a 7 by 7 deck of pale violet planks at the base and a low plank plinth in the middle, a single dark indigo altar block standing on the plinth with faint cyan rune light leaking from a carved side, a flat low canopy of teal-green glowing leaves stretched across the top of the wooden frame like a lamp shade so the cyan light is trapped underneath, completely open sides with no walls and no door, the altar the only light source in the scene, huge pale tree trunks and faint hot-spring steam dissolved in the background, every form is a 1-block cube grid, clean voxel edges, strong readable silhouette at a glance, whole structure fully inside the frame with generous margin, production concept art, consistent material language. Colors: stripped moonlight wood #a8a0b8 #c0c0d0, moonlight planks #a8a0b8, moonlight leaves #409078 #68b8a8, altar body #282040 #383050, altar rune glow #88e0f8, moon bloom accent #d89828.
```

**负面词**
```text
characters, players, creatures, mobs, text, watermark, signature, logo, ui elements, multiple unrelated buildings, duplicated structure, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, photorealistic wood, organic curved walls, smooth sculpted geometry, gothic cathedral, fantasy castle, stone temple, neon glow, lens flare, oversaturated, tilted horizon, large temple, stone masonry, gold ornaments, second light source, dense fluffy tree crown, walls and doors, glowing green crystal
```

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras` ｜ Flux `1:1, guidance 3.5, steps 30`

**短版**
```text
concept art, Minecraft voxel architecture, low angle, blocky, matte, indigo night, no text, no characters, 7x7 3 block tall wooden moon shrine, four pale violet log posts, plank deck, dark indigo altar block with faint cyan rune glow, flat canopy of teal glowing leaves, no walls, #a8a0b8 #282040 #88e0f8
```

**中文版**
```text
我的世界方块风建筑概念图，全部由 1 格立方体构成，四分之三低机位仰视 15 度，三渲二平涂插画，哑光材质，冷靛蓝夜色、唯一的亮点是青色符文微光，暗部有柔和环境光，画面中没有人物、生物、文字、水印、UI，一座宽 7 格、高 3 格的小型月神龛，读作"一盏祭坛灯"而不是一栋建筑：四角四根去皮淡紫原木柱，柱顶用横向原木梁连成方框，底部一块 7×7 的淡紫木板台面、中间一块矮木板台座，台座上一块深靛色祭坛方块、雕刻的侧面透出微弱的青色符文光，木框顶部平铺一层青绿色发光树叶、像灯罩一样把青光罩在下面，四面完全敞开、没有墙也没有门，整幅画面只有祭坛这一个光源，背景是虚化的巨大浅色树干与一丝温泉蒸汽，每处细节必须是整齐的方块，边缘干净，剪影一眼可辨，整座龛完整入画并留边距，生产级概念设定图，统一的材质语言。配色：去皮月华木 #a8a0b8 #c0c0d0、月华木板 #a8a0b8、月华树叶 #409078 #68b8a8、祭坛本体 #282040 #383050、符文光 #88e0f8、月之花点缀 #d89828。负面：人物、生物、文字、水印、UI、大型神殿、石砌、金饰、第二个光源、蓬松浓密的树冠、有墙有门、发绿光的晶体。
```

**调色板**：`#a8a0b8` · `#c0c0d0` · `#409078` · `#68b8a8` · `#282040` · `#88e0f8`

**要点**：7×3×7 是硬上限；木框 + 平叶冠 + 唯一青色光源；**无墙无门**。

---

### 09 · 阿瑞斯祭坛 · `ares_altar`（方块）

**id** `ares_altar` ｜ **平台** 火星 ｜ **类型** 方块 ｜ **体量** 1×1×1 格 ｜ **画幅** 1:1

```text
stylized Minecraft block icon concept art, one single one-metre cube floating in the centre of the frame, three-quarter view from slightly above, painted 2D illustration with 3-value cel shading, matte materials, crisp per-pixel surface detail, warm ember light from inside the block with a cool rim from the upper left, no scene, no pedestal, no characters, no text, no watermark, no logo, no UI, a summoning altar block of obsidian-black basalt: the top face is a recessed square basin one block wide with a raised stone rim, three countable ember cracks on the front face radiating outward from the basin and glowing dull orange deep inside their seams, the rest of the body completely matte and soot-dark, faint heat shimmer rising above the basin, a thin crust of cooled lava on the basin floor, no gold, no runes, no carvings, no letters, every surface flat and blocky, clean voxel edges, readable at thumbnail size, production concept art, consistent material language. Colors: basalt #181010 #201818 #483038, deepest top face #100810, ember inner glow #783818 #c07838, cooled crust #3a2a24.
```

**负面词**
```text
characters, players, creatures, text, watermark, signature, logo, ui elements, multiple objects, floating gems, ornate gold altar, runes, lettering, pedestal, scene, background objects, photorealistic lava, glossy surfaces, neon glow, lens flare, oversaturated, cartoon flame, torches, statues, multiple blocks
```

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras` ｜ Flux `1:1, guidance 3.5, steps 30`

**短版**
```text
concept art, Minecraft block icon, single cube, three-quarter from above, matte, no text, no characters, obsidian basalt altar block, recessed square basin on the top face with a stone rim, three glowing ember cracks on the front radiating from the basin, cooled lava crust, #181010 #783818 #c07838
```

**中文版**
```text
我的世界方块图标概念图，画面正中悬浮着单独的 1 格立方体，四分之三俯视略高机位，三渲二平涂插画，哑光材质，方块内部透出暖色余烬光、左上冷色轮廓光，没有场景、没有台座、没有人物、没有文字、没有水印、没有 UI，一块黑曜玄武岩质地的召唤祭坛方块：顶面是一个 1 格见方的内凹方池、带一圈石质凸边，正面上有 3 条可数的余烬裂纹、自方池向外放射、只在缝隙深处发出暗橙色的光，其余所有面完全哑光并覆着烟灰，方池上方有淡淡的热气扭曲，池底有一层冷凝固的熔岩壳，不要金色、不要符文、不要雕花、不要文字，所有表面平整方正，边缘干净，缩成缩略图仍可辨认，生产级概念设定图，统一的材质语言。配色：玄武岩 #181010 #201818 #483038、顶面最深 #100810、余烬内光 #783818 #c07838、熔岩壳 #3a2a24。负面：人物、生物、文字、水印、UI、多个物体、悬浮宝石、华丽金色神坛、符文、文字刻字、台座、场景、背景物体、写实熔岩、油亮表面、霓虹光、卡通火焰、火把、雕像、多个方块。
```

**调色板**：`#100810` · `#181010` · `#201818` · `#483038` · `#783818` · `#c07838`

**要点**：顶面最暗、裂纹最亮；三条可数裂纹；本体是"哑光黑石"，不是金色神坛。

---

### A1 · 金星云海氛围（背景板，不单独验收）

**id** `atmosphere_venus_cloudsea` ｜ **平台** 金星 ｜ **类型** 氛围 ｜ **体量** — ｜ **画幅** 16:9

```text
stylized Minecraft voxel environment concept art, painted 2D illustration with 3-value cel shading, matte materials, blocky one-metre cube formations, three-quarter high aerial view at 30 degrees, warm dusk key light from the upper left with soft ambient fill, no characters, no creatures, no text, no watermark, no logo, no UI, an endless sea of pale blue-grey clouds at high altitude broken by floating flat-topped cloud-stone islands with inverted cone undersides, a few detached 2 by 2 stone fragments drifting between them, distant lightning inside a dark cloud bank on the horizon, faint sulfuric haze low in the frame, cold rim light on every cloud edge, strong depth layering from near island to horizon, clean voxel edges, production concept art, consistent material language. Colors: cloud stone #586070 #788090 #8898a0, cloud sea #d8e2e8, distant storm #4a5560, warm dusk break #e8c090.
```

**负面词**
```text
characters, players, creatures, text, watermark, signature, logo, ui elements, buildings, ruins, machinery, photorealistic clouds, painterly soft clouds, neon glow, lens flare, oversaturated, tilted horizon, ground plane, forests, ocean waves
```

**参数**：MJ `--ar 16:9 --style raw --quality 2` ｜ SDXL `1344x768, steps 32, cfg 6.5` ｜ Flux `16:9, guidance 3.5, steps 30`

**短版**
```text
concept art, Minecraft voxel landscape, aerial view, blocky islands, matte, dusk light, no text, no characters, endless pale blue-grey cloud sea with flat-topped cloud-stone islands that have inverted cone bottoms, small drifting fragments, distant storm bank with lightning, #586070 #8898a0 #d8e2e8
```

**中文版**
```text
我的世界方块风场景概念图，三渲二平涂插画，哑光材质，全部由 1 格立方体构成，四分之三高空俯视 30 度，左上暖色黄昏主光 + 柔和环境光，画面中没有人物、生物、文字、水印、UI，一片无边无际的淡蓝灰云海，云海之上散布着顶部平坦、底部倒锥的云岩浮岛，岛与岛之间有零星的 2×2 石块碎片漂浮，地平线深处一片暗色雷暴云团内部有闪电，画面下方低处有一层薄薄的硫磺雾气，每朵云的边缘都有冷色轮廓光，从近处浮岛到地平线有明确的层次纵深，边缘干净，生产级概念设定图，统一的材质语言。配色：云岩 #586070 #788090 #8898a0、云海 #d8e2e8、远处雷暴 #4a5560、黄昏暖光 #e8c090。负面：人物、生物、文字、水印、UI、建筑、遗迹、机械、写实云、柔光油画感云、霓虹光、炫光、过饱和、地平线倾斜、地面、森林、海浪。
```

**调色板**：`#4a5560` · `#586070` · `#788090` · `#8898a0` · `#d8e2e8` · `#e8c090`

**要点**：浮岛顶平底锥；无任何建筑；用来当 S1/S3/S5/S6/S7 的背景板。

---

### A2 · 月林夜间氛围（背景板，不单独验收）

**id** `atmosphere_moon_forest` ｜ **平台** 月球 ｜ **类型** 氛围 ｜ **体量** — ｜ **画幅** 16:9

```text
stylized Minecraft voxel environment concept art, painted 2D illustration with 3-value cel shading, matte materials, blocky one-metre cube formations, eye-level three-quarter view, cold indigo moonlight from the upper right as the only key light with soft ambient fill, no characters, no creatures, no text, no watermark, no logo, no UI, a moon forest at night: tall pale violet blocky tree trunks with flat teal-green glowing leaf canopies high above, patches of pale violet soil and short teal grass on the floor, sparse orange moon blooms glowing faintly at ground level, a small steaming hot spring pool of milky water in the middle distance with its vapour catching the moonlight, heavy indigo shadow pooling between the trunks, strong depth layering, clean voxel edges, production concept art, consistent material language. Colors: trunk #a8a0b8 #c0c0d0, leaves #409078 #68b8a8, soil #686888 #888098, moon bloom #d89828, night indigo #1a1a30, hot spring vapour #cfe8f0.
```

**负面词**
```text
characters, players, creatures, text, watermark, signature, logo, ui elements, buildings, ruins, machinery, beds, tables, photorealistic forest, dense leaves, neon glow, lens flare, oversaturated, tilted horizon, daytime, warm sunset, fireflies everywhere
```

**参数**：MJ `--ar 16:9 --style raw --quality 2` ｜ SDXL `1344x768, steps 32, cfg 6.5` ｜ Flux `16:9, guidance 3.5, steps 30`

**短版**
```text
concept art, Minecraft voxel landscape, eye level, blocky, matte, cold indigo moonlight, no text, no characters, night moon forest, tall pale violet trunks, flat teal glowing leaf canopies, violet soil, sparse orange moon blooms, one steaming hot spring, deep indigo shadows, #a8a0b8 #409078 #d89828
```

**中文版**
```text
我的世界方块风场景概念图，三渲二平涂插画，哑光材质，全部由 1 格立方体构成，平视四分之三视角，右上冷靛蓝月光作为唯一主光 + 柔和环境光，画面中没有人物、生物、文字、水印、UI，一片夜间月林：高大淡紫色的方块树干、上方是平展的青绿色发光树叶冠层，地面是淡紫色土壤与短青草，地面零星几株橙色发光的月之花，中景一处冒着蒸汽的小温泉、奶白色水面把月光映开，树干之间积着浓重的靛蓝色阴影，层次纵深深，边缘干净，生产级概念设定图，统一的材质语言。配色：树干 #a8a0b8 #c0c0d0、树叶 #409078 #68b8a8、土壤 #686888 #888098、月之花 #d89828、夜色靛蓝 #1a1a30、温泉蒸汽 #cfe8f0。负面：人物、生物、文字、水印、UI、建筑、遗迹、机械、床桌家具、写实森林、浓密树叶、霓虹光、炫光、过饱和、地平线倾斜、白天、暖色夕阳、满地萤火。
```

**调色板**：`#686888` · `#a8a0b8` · `#c0c0d0` · `#409078` · `#68b8a8` · `#d89828`

**要点**：唯一暖色是月之花；树冠平展不成蓬；温泉蒸汽把月光打散。

---

## 3. 视图与变体追加句（接在长版末尾，不用重写）

| 要什么 | 追加句 |
|---|---|
| 去掉近侧墙看内部 | `cutaway version: the near-side wall and roof removed, interior floor and chests visible, same materials and lighting` |
| 俯视平面图 | `orthographic top-down plan view, grid aligned, no perspective, block layout clearly readable` |
| 正立面 | `orthographic front elevation, flat and straight on, no perspective, full height fills the frame` |
| 剪影测试 | `pure black silhouette of the same structure on a flat white background, no internal detail` |
| 雷暴版 | `the same structure under a dark storm sky with distant lightning, cold rim light, same materials` |
| 夜晚版 | `the same structure at night, only its own crystal and lantern light, deep blue ambient, same materials` |
| 远景（30 格外） | `wide shot from 60 blocks away, the structure small in frame, silhouette dominant, cloud sea filling the lower half` |

> 「远景」这句最重要：所有结构的首要验收标准是**远看认得出**，先出这一张过剪影关，再出主视觉。

---

## 4. 常见失败与修法

| 症状 | 修法 |
|---|---|
| 出成写实石建筑 / 哥特教堂 | 长版开头加 `no realistic masonry, strictly one-metre cubes`，负面词里保留 `photorealistic stone, gothic cathedral` |
| 硫晶出成荧光绿 | 长版颜色句里把 `#f8e8a8` 提前，负面词加 `glowing green crystal` |
| 四角塔/柱数量不对 | 把「four」写成 `exactly four, no more`，并加 `countable` |
| 建筑跑出画面 / 被裁 | 末尾加 `whole structure fully inside the frame, thick margin`，并把画幅从 3:4 改 4:3 |
| 复制出两栋 | 负面词保留 `duplicated structure, multiple unrelated buildings` |
| 比例被压扁（尤其观测塔） | 改用「竖构图 + 低机位 20 度」，并删掉所有俯视词 |
| 细节糊成一团 | 加 `big readable blocks, no tiny greebles`，把画布放大一档（1024 → 1344） |
