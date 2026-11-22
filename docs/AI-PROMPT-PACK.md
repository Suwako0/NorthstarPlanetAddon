# AI 绘图指令包 · 武器装备（月球除外）

> 配套：[美术需求案](EQUIPMENT-ART-BRIEF.md)。本文件是**喂给 AI 的指令**，不是需求说明；
> 每条 prompt 都已内联统一风格锚点，**复制即用，不需要再拼别的句子**。
> 机器可读版本：`docs/ai-prompts/equipment-prompts.json`（`prompt` / `prompt_short` / `negative` / `size`）；
> 纯文本合集：`docs/ai-prompts/all-prompts.txt`（一次看完 29 条，方便搜索）。

---

## 0. 怎么用

1. 找到对应物品的小节，**只复制 ```text 代码块里的那一段**，粘进软件。
2. **先看你的软件**：Midjourney / DALL·E 3 / SDXL 用第一段「长版」；SD 1.5、NovelAI、老版 WebUI 用第二段「短版」（长版会被 CLIP 在 77 token 处截断，尾巴全丢）。
3. 代码块下面的「负面词」在 Midjourney 里写成 `--no <内容>`；SD / ComfyUI 粘到 Negative 框；DALL·E 3 忽略即可。
4. 「参数」一行给的是该平台的建议值，按你用的软件取用。
5. 要试像素风，把「像素追加句」（见 §10）接在末尾，画幅改 1:1。

### 0.1 关于像素成品的现实提醒

AI 画不出严格对齐 16×16 网格、且 UV 区域正确的游戏贴图。可行的用法只有两种：

- **A（推荐）**：用本文件的 prompt 出**概念参考图**，交给像素美术照着画 16×16 / 128×128。
- **B**：出 64×64 的像素风图，手工降采样到 16×16 并逐像素修整（耗时约 20–40 分钟/件）。

所以本文件的定位是「**把美术意图压成 AI 能听懂的话**」，不是「一键出成品贴图」。

---

### 0.2 全部 29 条索引

| # | 装备 | 阵营 | 类型 | 阶层 | 画幅 | 所在节 |
|---|---|---|---|---|---|---|
| 1 | 融核巨剑 | 火星 | 武器 | EPIC | 3:4 | §2 |
| 2 | 星核炮 | 火星 | 武器 | EPIC | 3:4 | §2 |
| 3 | 熔核套（四件） | 火星 | 护甲 | EPIC | 3:4 | §2 |
| 4 | 沙尘暴护目镜 | 火星 | 工具 / 随身 | UNCOMMON | 1:1 | §2 |
| 5 | 便携式冷却器 | 火星 | 工具 / 随身 | UNCOMMON | 1:1 | §2 |
| 6 | 冷却液罐（空/水/液氧） | 火星 | 物品组 | UNCOMMON | 16:9 | §2 |
| 7 | 酸蚀矛 | 金星 | 武器 | UNCOMMON | 3:4 | §3.1 |
| 8 | 压力锤 | 金星 | 武器 | RARE | 3:4 | §3.1 |
| 9 | 雷羽弩 | 金星 | 武器 | RARE | 3:4 | §3.1 |
| 10 | 云钩 | 金星 | 武器 | UNCOMMON | 1:1 | §3.1 |
| 11 | 硫火喷射器 | 金星 | 武器 | RARE | 1:1 | §3.1 |
| 12 | 高压钻 | 金星 | 武器 | RARE | 3:4 | §3.1 |
| 13 | 菌剪 | 金星 | 工具 / 随身 | UNCOMMON | 1:1 | §3.2 |
| 14 | 云图仪 | 金星 | 工具 / 随身 | UNCOMMON | 1:1 | §3.2 |
| 15 | 气压计 | 金星 | 工具 / 随身 | UNCOMMON | 1:1 | §3.2 |
| 16 | 耐酸镀层 | 金星 | 工具 / 随身 | UNCOMMON | 1:1 | §3.2 |
| 17 | 压力补偿器 | 金星 | 工具 / 随身 | UNCOMMON | 1:1 | §3.2 |
| 18 | 散热片 | 金星 | 工具 / 随身 | UNCOMMON | 1:1 | §3.2 |
| 19 | 气囊背包 | 金星 | 工具 / 随身 | UNCOMMON | 1:1 | §3.2 |
| 20 | 滤芯 | 金星 | 工具 / 随身 | UNCOMMON | 1:1 | §3.2 |
| 21 | 弹道校准器 | 金星 | 工具 / 随身 | RARE | 1:1 | §3.2 |
| 22 | 鲸骨号角 | 金星 | 召唤物 | RARE | 1:1 | §3.3 |
| 23 | 压力阀 | 金星 | 召唤物 | RARE | 1:1 | §3.3 |
| 24 | 硫心 | 金星 | 召唤物 | EPIC | 1:1 | §3.3 |
| 25 | 钨钢压力服 | 金星 | 护甲 | T2 | 3:4 | §3.4 |
| 26 | 云行者 | 金星 | 护甲 | T2 | 3:4 | §3.4 |
| 27 | 熔管重甲 | 金星 | 护甲 | T4 | 3:4 | §3.4 |
| 28 | 启明星战甲 | 金星 | 护甲 | T5 | 3:4 | §3.4 |
| 29 | 日冕轻甲（提案） | 水星 | 护甲 | T1.5 | 3:4 | §4 |

---

## 1. 全局块（已内联，仅供改动参考）

**风格锚点**（每条 prompt 的开头都是它，改这里等于改整包风格）：

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI
```

**短版锚点**（短版 prompt 的开头，足够短、不吃 token 额度）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text
```

**渲染后缀**（每条 prompt 的结尾）：

```text
clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language
```

**基础负面词**（按类别自动换用 `armor` / `weapon` / `item` / `row` 四套）：

```text
text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles
```

四种取景框架（武器双视图 / 护甲三视图 / 单件悬浮 / 三态并排）已经按类别内联，不需要手选。

---

## 2. 火星线 · 熔炉与战神（6 件）

火星线只有两件武器、一套甲和三件野外装备。调性统一为**熔铸 + 黑曜石 + 余烬**：所有华丽感来自熔壳裂纹与青铜包边，不是魔法光效。

### 融核巨剑 · `molten_core_sword`

*火星线 · 武器 · EPIC · 巨剑 / 伤害 12 · 画幅 3:4*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, weapon design sheet: one large three-quarter view at 45 degrees beside a smaller flat side view of the identical design, tip or muzzle pointing up-right, constant thickness, a colossal two-handed greatsword, blade is a single slab of black basalt-obsidian split by three glowing ember cracks that fork toward the edge, a molten core burning at the blade root, a continuous thin ember line inside the fuller offset slightly to one side, heavy asymmetrical crossguard with a four-pointed star cut into the left end and a notch on the right, leather-wrapped grip with four cord rings spaced tighter near the guard, heavy pommel with a short burnt tassel hanging loose. Blade takes 62 percent of total length, brutal and heavy, not elegant. Colors: obsidian black #2a1512, scorched iron #8a3418, ember orange #ff7a3c, blade edge highlight #ffd9a0, aged brass ferrules #d8a838, deep soot #14100e, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, bent blade, floating gems, pure energy blade, cartoonish flame, toy proportions, symmetrical guard, elegant silver blade, clean polished steel`

**参数**：MJ `--ar 3:4 --style raw --quality 2` ｜ SDXL `896x1152, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, colossal two-handed greatsword, obsidian blade split by three glowing ember cracks, a molten core at the blade root, brass guard with a four-pointed star, burnt tassel, colors #2a1512 #8a3418 #ff7a3c.
```

**调色板**：`#14100e` · `#2a1512` · `#8a3418` · `#ff7a3c` · `#ffd9a0` · `#d8a838`

**要点**：刃身要有 3 条可数的裂纹；余烬面积不超过 12%；流苏必须烧焦毛边；护手两端不对称。

---

### 星核炮 · `starcore_cannon`

*火星线 · 武器 · EPIC · 重炮 / 伤害 20 · 画幅 3:4*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, weapon design sheet: one large three-quarter view at 45 degrees beside a smaller flat side view of the identical design, tip or muzzle pointing up-right, constant thickness, a heavy handheld mortar cannon, muzzle as wide as two and a half grip diameters and tapering back to a leather-wrapped pistol grip, three concentric rifling rings visible down the bore with the innermost ring white-hot, one wide brass band at mid-barrel, a round observation port on the side with a brass bezel and glass showing a four-pointed star core inside, a small pressure tank at the rear joined by two ribbed hoses, a transverse four-slat heat vent under the muzzle, squared trigger guard, three cord rings on the grip. Colors: mars steel #373e3a, shadow steel #554a44, brass #d8a838, star core glow #ffb03a, hot core #ffe9b0, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, bent blade, floating gems, pure energy blade, cartoonish flame, toy proportions, thin staff, magical wand, energy cannon, sci-fi laser rifle`

**参数**：MJ `--ar 3:4 --style raw --quality 2` ｜ SDXL `896x1152, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, heavy handheld mortar cannon, wide rifled muzzle with hot inner rings, brass band, side window showing a glowing four-pointed star core, rear pressure tank and hoses, #373e3a #d8a838.
```

**调色板**：`#373e3a` · `#554a44` · `#d8a838` · `#ffb03a` · `#ffe9b0`

**要点**：第一眼必须读成炮，不能像法杖；炮口内壁要有 3 阶明度；观察窗是唯一亮点。

---

### 熔核套（四件） · `molten_core_set`

*火星线 · 护甲 · EPIC · 重甲 / 护甲 5-12-9-5 · 画幅 3:4*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, full-body armor design sheet on a featureless matte grey mannequin: front view, three-quarter view and back view side by side, identical design in all three views, no visible face, dark featureless visor, neutral stand, a full suit of volcanic plate armor, tall and lean with a raised shoulder line: a five-spike crown helmet whose center spike is tallest and two dark eyeslits glow faintly warm, a chestplate with a round molten core opening in the middle and a pair of swept back fins behind the shoulders plus two hanging chains, a thick belt with a square brass buckle, layered thigh plates and knee guards, and heavy boots whose soles glow dull orange at the front. Soot-blackened edges, welded seams, uneven hammer marks. Colors: scorched iron #2a1512, ember crust #5a2a1e, sulfur brass trim #c9a227, ember glow #ff6a00, garnet gem #ff3b6b, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, visible face, bare head, anime face, big eyes, thin legs, clipping cape, floating pieces, cape, angel wings, symmetric glow wings, cute proportions`

**参数**：MJ `--ar 3:4 --style raw --quality 2` ｜ SDXL `896x1152, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, full volcanic plate armor, five-spike crown, glowing round core in the chestplate, back fins and hanging chains, glowing boot soles, #2a1512 #c9a227 #ff6a00.
```

**调色板**：`#2a1512` · `#5a2a1e` · `#c9a227` · `#ff6a00` · `#ff3b6b`

**要点**：高瘦剪影；五尖冠是识别点；冠尖、腰带扣、熔核开口是视觉重心；不要布披风。

**分件追加句**（在完整 prompt 末尾加一句，只出单件）：

- 头盔：`emphasize the five-spike crown, twin eyeslits and a rear neck guard`
- 胸甲：`emphasize the round molten core opening, the two back fins and hanging chains`
- 护腿：`emphasize the wide belt with square buckle, layered thigh plates and knee guards`
- 靴子：`emphasize the glowing front sole and the ankle rings`

---

### 沙尘暴护目镜 · `sandstorm_goggles`

*火星线 · 工具 / 随身 · UNCOMMON · 头部护具 / 铁材质 · 画幅 1:1*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, single item concept, floating three-quarter view with a slight top-down tilt, no hands, no character, no scene, field aviator goggles built for a dust storm: two round amber lenses in a brass frame joined by a small nose bridge, one lens with a single diagonal scratch, each lens with one crisp specular dot in the upper left, thick dark brown leather strap running to a small brass buckle on the right, stitched edge, no glass reflection gradient. Colors: brass frame #b08a3a, amber lens #ffb648, leather strap #30230f, brass highlight #f5e08a, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, oversized prop, decorative scene, glowing aura, extra copies of the same object`

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, brass aviator goggles, two amber lenses, one scratched, dark leather strap with a brass buckle.
```

**调色板**：`#b08a3a` · `#ffb648` · `#30230f` · `#f5e08a`

**要点**：横向 12 px 占满图；镜片是唯一亮点；右镜片划痕做不对称。

---

### 便携式冷却器 · `portable_cooler`

*火星线 · 工具 / 随身 · UNCOMMON · 工具 / 冷却枪 · 画幅 1:1*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, single item concept, floating three-quarter view with a slight top-down tilt, no hands, no character, no scene, a handheld high-pressure coolant gun: pistol grip at the back, twin stacked canisters in the middle body, a short flared barrel with a three-hole spray nozzle at the front, a round pressure gauge on the side with its needle at two o'clock, a narrow translucent level window holding a cold blue liquid, a small brass relief valve, three anti-slip grooves on the grip. Frost forming on the nozzle. Colors: gunmetal #4c545a, polished steel #8f9aa0, gauge teal #48c8c0, cold mist #cfe9ff, brass #b08a3a, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, oversized prop, decorative scene, glowing aura, extra copies of the same object`

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, handheld coolant gun, pistol grip, twin canisters, three-hole nozzle, side pressure gauge at two o'clock, frosted nozzle, #4c545a #48c8c0.
```

**调色板**：`#4c545a` · `#8f9aa0` · `#48c8c0` · `#cfe9ff` · `#b08a3a`

**要点**：工具感大于武器感；压力表与液位窗不要在同一水平线。

---

### 冷却液罐（空/水/液氧） · `coolant_tanks`

*火星线 · 物品组 · UNCOMMON · 消耗品 / 三态 · 画幅 16:9*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, a straight row of three identical objects side by side, exactly the same silhouette and scale, differing only in fill state, three identical industrial coolant canisters standing in a row, same vertical cylinder silhouette with a valve on top pointing right, a thick base ring and a narrow vertical sight window: the first one empty and dark inside, the second half filled with blue water, the third completely filled with pale liquid oxygen and covered in white frost specks, each wearing one brass label band, the liquid oxygen label stamped with three frost dashes. Colors: canister steel #3f464b, empty interior #241f1d, water #4aa3d8, liquid oxygen #9fe8ff, label brass #d8a838, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, different shapes, different sizes, scattered layout`

**参数**：MJ `--ar 16:9 --style raw --quality 2` ｜ SDXL `1344x768, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, three identical steel canisters in a row, one empty, one half full of blue water, one frosted with liquid oxygen, brass label bands.
```

**调色板**：`#3f464b` · `#241f1d` · `#4aa3d8` · `#9fe8ff` · `#d8a838`

**要点**：三态轮廓必须完全一致，只改填充；否则玩家会以为是三种物品。

---

## 3.1 金星线 · 武器与器械（6 件）

六把武器分三个材质家族，**不要混用**：压力系（钨钢 + 黄铜 + 仪表青）、酸系（钨钢 + 酸绿）、雷系（抛光钨 + 电青）。

### 酸蚀矛 · `acidic_spear`

*金星线 · 武器 · UNCOMMON · 长枪 / 伤害 9 / 攻速 1.4 · 画幅 3:4*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, weapon design sheet: one large three-quarter view at 45 degrees beside a smaller flat side view of the identical design, tip or muzzle pointing up-right, constant thickness, a long tungsten spear: leaf-shaped head with two narrow grooves holding two separate beads of glowing acid instead of one continuous line, three acid pits clustered at the head base, two brass bands on the shaft spaced unevenly, six wraps of dark cord at the grip with one oily stain, a counterweight at the butt and a short forked acid-green tassel. Industrial, utilitarian, slightly corroded. Colors: tungsten #3f464b to #eaf3f6 four-value ramp, acid glow #7dff4a, corrosion dark #2e5b1e, brass #b08a3a, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, bent blade, floating gems, pure energy blade, cartoonish flame, toy proportions, holy lance, full green glow, ornate fantasy spear`

**参数**：MJ `--ar 3:4 --style raw --quality 2` ｜ SDXL `896x1152, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, long tungsten spear, leaf head with two separate glowing acid beads in its grooves, brass bands, cord grip, forked acid-green tassel, #3f464b #7dff4a.
```

**调色板**：`#3f464b` · `#9aa0a6` · `#eaf3f6` · `#7dff4a` · `#2e5b1e` · `#b08a3a`

**要点**：枪头槽内是两段断开酸液；蚀孔只在枪头根部与箍下；不要整根发绿光。

---

### 压力锤 · `pressure_hammer`

*金星线 · 武器 · RARE · 战锤 / 伤害 13 / 攻速 0.75 · 画幅 3:4*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, weapon design sheet: one large three-quarter view at 45 degrees beside a smaller flat side view of the identical design, tip or muzzle pointing up-right, constant thickness, a warhammer whose head is a cylindrical pressure vessel: a round pressure gauge on each side with needles at different angles, a hexagonal dent in the striking face ringed by four rivets, two triangular reinforcing ribs and a small brass relief valve where the head meets the shaft, a short thick shaft with four cord wraps, a heavy collar at the butt and a safety loop, one old crack on the head welded shut leaving a visible scar. Colors: tungsten #383838, steel fitting #8f9aa0, gauge teal #48c8c0, polished bevel #c2ccd4, brass #d8a838, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, bent blade, floating gems, pure energy blade, cartoonish flame, toy proportions, fantasy giant hammer, glowing hammer face, oversized head`

**参数**：MJ `--ar 3:4 --style raw --quality 2` ｜ SDXL `896x1152, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, warhammer with a cylindrical pressure-vessel head, a gauge on each side, hex dent with rivets, reinforcing ribs, one welded crack, #383838 #48c8c0.
```

**调色板**：`#383838` · `#8f9aa0` · `#48c8c0` · `#c2ccd4` · `#d8a838`

**要点**：两个压力表可读；锤头体积明显重于柄；焊补痕迹不抢主视觉。

---

### 雷羽弩 · `thunder_crossbow`

*金星线 · 武器 · RARE · 弩 / 伤害 9 / 蓄力 1.2s · 画幅 3:4*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, weapon design sheet: one large three-quarter view at 45 degrees beside a smaller flat side view of the identical design, tip or muzzle pointing up-right, constant thickness, a heavy crossbow with two-segment steel limbs that end in three thin thunder-feather fins, a dashed electric arc broken into three short segments across the string, a three-segment stock with a bolt loaded showing an arrowhead and twin fletching, a rectangular charge window with four tick marks on the side, a small scope block and a squared trigger guard, brass fittings and three cord rings on the grip. Colors: steel #3f4750 to #c2ccd4, lightning #8fd8ff, arc white #eaf6ff, brass #b08a3a, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, bent blade, floating gems, pure energy blade, cartoonish flame, toy proportions, glowing blue energy bow, cartoon lightning sticker, recurve fantasy bow`

**参数**：MJ `--ar 3:4 --style raw --quality 2` ｜ SDXL `896x1152, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, heavy crossbow, two-segment limbs with thunder-feather fins, dashed electric arc on the string, four-tick charge window, loaded bolt, #3f4750 #8fd8ff.
```

**调色板**：`#3f4750` · `#c2ccd4` · `#8fd8ff` · `#eaf6ff` · `#b08a3a`

**要点**：弩臂必须能看出是两段；充能窗 4 格；电弧不超过弩身 1/3。

---

### 云钩 · `cloud_hook`

*金星线 · 武器 · UNCOMMON · 抓钩 / 24 格 · 画幅 1:1*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, single item concept, floating three-quarter view with a slight top-down tilt, no hands, no character, no scene, a J-shaped grappling hook made of cloud-white steel sheathed in a translucent sail membrane, a barbed inner tip, two thin membrane ribs radiating across the hook's back, a two-section handle with one brass collar and a coil of rope hanging at the end, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, oversized prop, decorative scene, glowing aura, extra copies of the same object`

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, J-shaped grappling hook of white steel sheathed in translucent sail membrane, barbed tip, cord-wrapped handle.
```

**调色板**：`#cfe9ff` · `#8a9199` · `#2a4a6a` · `#b08a3a`

**要点**：钩 + 柄两段清晰；倒刺可见；不要画成海盗铁钩。

---

### 硫火喷射器 · `sulfur_flamethrower`

*金星线 · 武器 · RARE · 喷射器 / 6 伤每秒 · 画幅 1:1*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, single item concept, floating three-quarter view with a slight top-down tilt, no hands, no character, no scene, a compact handheld flamethrower: one sulfur-yellow fuel canister on the left with a narrow level strip, two ribbed hoses bending into a stubby barrel on the right, a three-stage flared nozzle emitting a broken dotted flame of orange yellow with a green sulfur core, leather straps, a squared trigger guard and three grip grooves. The flame must not cover the body, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, oversized prop, decorative scene, glowing aura, extra copies of the same object, solid flame covering the device, rocket launcher`

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, compact flamethrower, yellow fuel canister with level strip, two hoses, flared three-stage nozzle with a dotted orange flame, #8a9199 #e8d97a.
```

**调色板**：`#8a9199` · `#e8d97a` · `#ff9a3c` · `#7dff4a` · `#30230f`

**要点**：罐/管/火三段分明；火苗是点状不是实心色块。

---

### 高压钻 · `pressure_drill`

*金星线 · 武器 · RARE · 镐类 / 挖掘 10 / 3x3 · 画幅 3:4*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, weapon design sheet: one large three-quarter view at 45 degrees beside a smaller flat side view of the identical design, tip or muzzle pointing up-right, constant thickness, a pneumatic mining drill: a four-flight auger bit with two collar rings and a four-facet tip, a cylindrical body with three bands and three vent slots, a round pressure gauge and a rear air-hose port, a pistol grip with three cord wraps, a trigger and a base plate, four bolts with one missing, rust streaks inside the auger flutes. Colors: steel #4c545a to #d5dde2, brass #b08a3a, gauge teal #48c8c0, rust #5a3226, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, bent blade, floating gems, pure energy blade, cartoonish flame, toy proportions, short drill bit, glowing sci-fi drill`

**参数**：MJ `--ar 3:4 --style raw --quality 2` ｜ SDXL `896x1152, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, pneumatic drill, four-flight auger bit, body with vent slots, side pressure gauge, rear air port, pistol grip, #4c545a #b08a3a.
```

**调色板**：`#4c545a` · `#d5dde2` · `#b08a3a` · `#48c8c0` · `#5a3226`

**要点**：钻头螺旋可数出 4 段；压力表与气喉在视觉上连成一条气路。

---

## 3.2 金星线 · 工具与随身模块（9 件）

工具是 16×16 图标，构图必须**横向或对角铺满**；六件模块要成组：统一 2 px 厚边框、右下角一颗铆钉，让玩家一眼看出它们是一套系统。

### 菌剪 · `fungus_shears`

*金星线 · 工具 / 随身 · UNCOMMON · 工具 / 耐久 250 · 画幅 1:1*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, single item concept, floating three-quarter view with a slight top-down tilt, no hands, no character, no scene, garden shears with X-crossed steel blades of unequal length, a round sulfur-yellow pivot boss with one specular dot, dark leather wrapped grips and a small hole with a hanging cord at the end, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, oversized prop, decorative scene, glowing aura, extra copies of the same object, perfectly symmetric scissors`

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, X-crossed garden shears, unequal blades, sulfur-yellow pivot boss, dark leather grips.
```

**调色板**：`#8a9199` · `#e8d97a` · `#30230f`

**要点**：双刃不等长；轴心是唯一亮点。

---

### 云图仪 · `cloud_chart`

*金星线 · 工具 / 随身 · UNCOMMON · 仪器 / 扫描 64 格 · 画幅 1:1*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, single item concept, floating three-quarter view with a slight top-down tilt, no hands, no character, no scene, a handheld survey instrument: squared dark steel body, a two-section telescoping antenna on top, a round glass screen on the front showing three contour arcs and one bright location dot, a brass bezel with two rivets and a small side switch, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, oversized prop, decorative scene, glowing aura, extra copies of the same object, modern smartphone, sci-fi hologram`

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, handheld survey device, square body, telescoping antenna, round screen with three contour arcs and one location dot, brass bezel.
```

**调色板**：`#3f464b` · `#48c8c0` · `#b08a3a`

**要点**：屏幕是唯一亮点；天线不与机身同宽。

---

### 气压计 · `barometer`

*金星线 · 工具 / 随身 · UNCOMMON · 仪器 / 读数式 · 画幅 1:1*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, single item concept, floating three-quarter view with a slight top-down tilt, no hands, no character, no scene, an antique aneroid barometer: a large round ivory dial taking three quarters of the shape with six tick marks, a dark red needle pointing at eleven o'clock, a brass rim, a short handle below with a hanging hole and a glass cover with one soft highlight, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, oversized prop, decorative scene, glowing aura, extra copies of the same object, digital display, wristwatch`

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, antique round barometer, ivory dial with six ticks, dark red needle at eleven o'clock, brass rim.
```

**调色板**：`#e8d97a` · `#30230f` · `#8a3418` · `#b08a3a`

**要点**：6 格刻度可数；指针指 11 点。

---

### 耐酸镀层 · `acid_coating`

*金星线 · 工具 / 随身 · UNCOMMON · 模块 / 腐蚀 -60% · 画幅 1:1*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, single item concept, floating three-quarter view with a slight top-down tilt, no hands, no character, no scene, a small paint tin of anti-acid coating, screw lid half open with one green drop hanging from the rim, one brass label band, a single acid drip caught mid fall, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, oversized prop, decorative scene, glowing aura, extra copies of the same object`

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, small paint tin of anti-acid coating, open screw lid, one green drip, brass label band.
```

**调色板**：`#3f464b` · `#7dff4a` · `#b08a3a`

**要点**：罐 + 一滴酸；统一 2 px 厚边框、右下角铆钉。

---

### 压力补偿器 · `pressure_compensator`

*金星线 · 工具 / 随身 · UNCOMMON · 模块 / 耐压 +20 atm · 画幅 1:1*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, single item concept, floating three-quarter view with a slight top-down tilt, no hands, no character, no scene, a compact cylindrical pressure regulator with a four-spoke brass handwheel on top, two side pressure ports, a threaded base and one small gauge window, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, oversized prop, decorative scene, glowing aura, extra copies of the same object`

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, small cylindrical pressure regulator, four-spoke brass handwheel, two side ports, threaded base.
```

**调色板**：`#6b7379` · `#b08a3a` · `#241f1d`

**要点**：四辐手轮是识别点。

---

### 散热片 · `heat_sink`

*金星线 · 工具 / 随身 · UNCOMMON · 模块 / 火焰 -25% · 画幅 1:1*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, single item concept, floating three-quarter view with a slight top-down tilt, no hands, no character, no scene, a stacked fin heat sink: five parallel aluminum fins with dark gaps between them, mounted on a small brass base plate with two mounting tabs, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, oversized prop, decorative scene, glowing aura, extra copies of the same object`

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, stacked heat sink, five parallel aluminum fins with dark gaps, mounted on a brass base plate.
```

**调色板**：`#9aa0a6` · `#241f1d` · `#b08a3a`

**要点**：5 片鳍，缝要黑。

---

### 气囊背包 · `air_bag`

*金星线 · 工具 / 随身 · UNCOMMON · 模块 / 缓降 · 画幅 1:1*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, single item concept, floating three-quarter view with a slight top-down tilt, no hands, no character, no scene, a folded small air bag of pale blue sail cloth with three creases, one brass buckle and a thin release cord trailing at the bottom, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, oversized prop, decorative scene, glowing aura, extra copies of the same object`

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, folded pale blue air bag, three creases, brass buckle, thin release cord.
```

**调色板**：`#cfe9ff` · `#b08a3a` · `#2a4a6a`

**要点**：3 道折痕 + 铜扣 + 泄气绳。

---

### 滤芯 · `filter`

*金星线 · 工具 / 随身 · UNCOMMON · 模块 / 免疫孢子失明 · 画幅 1:1*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, single item concept, floating three-quarter view with a slight top-down tilt, no hands, no character, no scene, a round filter cartridge, front face a dark honeycomb mesh with six visible holes, one clamp band around the middle and a small brass locking tab, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, oversized prop, decorative scene, glowing aura, extra copies of the same object`

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, round filter cartridge, dark honeycomb face with six holes, one clamp band, brass locking tab.
```

**调色板**：`#6b7379` · `#30230f` · `#b08a3a`

**要点**：蜂窝 6 孔；卡箍 1 道。

---

### 弹道校准器 · `ballistic_calibrator`

*金星线 · 工具 / 随身 · RARE · 模块 / 弱点 +1 过载 · 画幅 1:1*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, single item concept, floating three-quarter view with a slight top-down tilt, no hands, no character, no scene, a small optical calibration device: dark rectangular body, three brass tick marks on the side, one round lens with a hard specular dot, a knurled knob on top and a short mounting rail, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, oversized prop, decorative scene, glowing aura, extra copies of the same object`

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, small optical calibration device, dark body, three brass tick marks, one lens with a single reflection, knurled top knob.
```

**调色板**：`#3f464b` · `#8fd8ff` · `#e8d97a`

**要点**：镜片反光只 1 点；3 格刻度。

---

## 3.3 金星线 · Boss 召唤物（3 件）

三件都是**可以摆在祭坛上的器物**，不是穿戴件。共同调性：工业器物 + 一点点不祥。

### 鲸骨号角 · `whale_bone_horn`

*金星线 · 召唤物 · RARE · 召唤物 / 涅斐勒 · 画幅 1:1*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, single item concept, floating three-quarter view with a slight top-down tilt, no hands, no character, no scene, a war horn carved from a whale rib: pale bone body with two hairline cracks, a three-stage flared mouth, two brass bands and a cord-wrapped mouthpiece, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, oversized prop, decorative scene, glowing aura, extra copies of the same object`

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, war horn carved from a pale whale rib, two hairline cracks, three-stage flared mouth, brass bands.
```

**调色板**：`#e8dcc0` · `#a89a7a` · `#b08a3a` · `#30230f`

**要点**：骨纹 2 道裂纹；号口 3 段收口。

---

### 压力阀 · `pressure_valve`

*金星线 · 召唤物 · RARE · 召唤物 / 赫菲斯托斯 · 画幅 1:1*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, single item concept, floating three-quarter view with a slight top-down tilt, no hands, no character, no scene, an eight-inch industrial gate valve: four-spoke brass handwheel on a short stem, cast iron body with a six-bolt flange, one small round pressure gauge window and rust streaks running down from the bolts, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, oversized prop, decorative scene, glowing aura, extra copies of the same object`

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, eight-inch gate valve, four-spoke brass handwheel on a short stem, six-bolt flange, small gauge window, rust streaks.
```

**调色板**：`#6b7379` · `#b08a3a` · `#241f1d`

**要点**：法兰 6 颗螺栓；锈迹顺螺栓向下。

---

### 硫心 · `sulfur_heart`

*金星线 · 召唤物 · EPIC · 召唤物 / 阿佛洛狄忒 · 画幅 1:1*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, single item concept, floating three-quarter view with a slight top-down tilt, no hands, no character, no scene, a still beating heart grown from sulfur crystal: three crystal petals, a white-hot core visible through the gaps, two root-like veins wrapping the base and a small gold mounting ring underneath, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, oversized prop, decorative scene, glowing aura, extra copies of the same object, magic runes, glowing orb, anatomical heart`

**参数**：MJ `--ar 1:1 --style raw --quality 2` ｜ SDXL `1024x1024, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, heart of sulfur crystal, three petals, white-hot core visible inside, two root veins, small gold base ring, #e8d97a #fdf6c4.
```

**调色板**：`#e8d97a` · `#a8912a` · `#5a2a1e` · `#fdf6c4`

**要点**：3 片晶瓣 + 白热芯 + 2 条血管；不要画成魔法符文。

---

## 3.4 金星线 · 四套护甲（逐套独立）

**四套必须一眼分清**。每套的 prompt 里都有互斥的识别词，删掉就会互相靠拢。

先看下表确认差异，再往下取用；第五套「熔核」属火星线，见 §2。

| 套装 | 剪影关键词 | 主色 | 一眼认出的标志件 | 与其它套的区别 |
|---|---|---|---|---|
| 钨钢压力服 | `broad` 宽肩方盔 | 冷灰 `#5a6068` | 双线焊缝 + 前额压力表 + 波纹颈圈 | 唯一「硬壳密封」套，禁止一切布料 |
| 云行者 | `winged` 头巾薄膜翼 | 青蓝 `#3f5a66` | 头巾两条不等长尾带 + 背后透光膜翼 | 唯一「布 + 膜」轻甲，细 1 px 包边 |
| 熔管重甲 | `spiked` 肩刺 | 赤褐 `#5a2a1e` | 三根带底座的肩刺 + 排酸沟槽 + 烧熔披风 | 唯一「脏 + 破损」套，必须有烧伤与缺口 |
| 启明星战甲 | `caped` 双层光环 | 金 `#d4af37` | 双层断口光环 + 双角冠 + 菱形胸宝 | 唯一「金 ≥65% + 宝石」套，青色只做光 |

### 钨钢压力服 · `tungsten_set`

*金星线 · 护甲 · T2 · 重甲 / 护甲 4-8-6-3 / 耐压 100 · 画幅 3:4*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, full-body armor design sheet on a featureless matte grey mannequin: front view, three-quarter view and back view side by side, identical design in all three views, no visible face, dark featureless visor, neutral stand, a hard-shell pressure suit built like a deep sea diving armor: broad shoulders with pauldrons flaring outward, a boxy squared helmet with a small round pressure gauge on the front and a three-segment corrugated neck seal behind, every plate edged with a double line of weld bead and rivets spaced unevenly, two reinforcing ribs across each pauldron, an engraved name plate on the chest, articulated joint seams at elbows and knees, thick sealed collar. Clean engineering, no cloth. Colors: tungsten plate #5a6068, bright weld edge #c9ced4, mid grey #9aa0a6, gem #c9a227, dark liner #3a3f45, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, visible face, bare head, anime face, big eyes, thin legs, clipping cape, floating pieces, cloth cape, fabric strips, glowing lines, rounded organic helmet`

**参数**：MJ `--ar 3:4 --style raw --quality 2` ｜ SDXL `896x1152, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, hard-shell pressure suit armor, broad pauldrons, boxy helmet with a front gauge and corrugated neck seal, double weld lines, uneven rivets, no cloth, #5a6068 #c9ced4.
```

**调色板**：`#5a6068` · `#c9ced4` · `#9aa0a6` · `#c9a227` · `#3a3f45`

**要点**：识别点 = 宽肩 + 方盔 + 焊缝双线；最'工程师'的一套，禁止任何布片飘带。

**分件追加句**（在完整 prompt 末尾加一句，只出单件）：

- 头盔：`emphasize the squared helmet, front gauge and corrugated neck seal`
- 胸甲：`emphasize the broad pauldrons with double weld lines and the chest name plate`
- 护腿：`emphasize rigid thigh shells with hinge seams and the sealed waist ring`
- 靴子：`emphasize heavy sealed boots with double toe caps and ankle bolts`

---

### 云行者 · `cloud_walker_set`

*金星线 · 护甲 · T2 · 轻甲 / 护甲 3-6-5-2 / 滑翔 · 画幅 3:4*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, full-body armor design sheet on a featureless matte grey mannequin: front view, three-quarter view and back view side by side, identical design in all three views, no visible face, dark featureless visor, neutral stand, a lightweight gliding armor of sail membrane and thin steel frame: a hooded head wrap with two tail ribbons of unequal length, a pair of translucent membrane wings spread behind the shoulders with two radial ribs each and a bright 1px light bleed along the wing edge, a chest plate that is plate on the upper two thirds and cloth on the lower third, a tattered robe hem with a jagged lower edge cut shorter on one side, thin knee guards, slim boots. Thin 1px edging everywhere, no thick welds. Colors: membrane steel #3f5a66, pale trim #cfe9ff, accent #7fd8ff, cloth #2a4a6a, gem #9fe8ff, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, visible face, bare head, anime face, big eyes, thin legs, clipping cape, floating pieces, heavy plate, thick rivets, angel feathers, bird wings`

**参数**：MJ `--ar 3:4 --style raw --quality 2` ｜ SDXL `896x1152, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, light gliding armor, hood with two unequal ribbon tails, translucent membrane wings behind the shoulders, jagged robe hem, thin 1px edging, #3f5a66 #cfe9ff.
```

**调色板**：`#3f5a66` · `#cfe9ff` · `#7fd8ff` · `#2a4a6a` · `#9fe8ff`

**要点**：识别点 = 头巾长尾 + 背后薄膜翼 + 锯齿下摆；与钨钢并排必须立刻分出轻重。

**分件追加句**（在完整 prompt 末尾加一句，只出单件）：

- 头巾：`emphasize the hood with two unequal tail ribbons`
- 胸甲：`emphasize the translucent wing membranes with radial ribs`
- 护腿：`emphasize the cloth robe hem with jagged edge and thin knee guards`
- 靴子：`emphasize slim membrane-wrapped boots with light soles`

---

### 熔管重甲 · `tube_heavy_set`

*金星线 · 护甲 · T4 · 重甲 / 护甲 5-9-7-4 / 击退抗性 0.2 · 画幅 3:4*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, full-body armor design sheet on a featureless matte grey mannequin: front view, three-quarter view and back view side by side, identical design in all three views, no visible face, dark featureless visor, neutral stand, brutal volcanic tunnel plate armor, the dirtiest of the family: three short back-swept shoulder spikes each seated in a reinforced socket, a plume on the crown, two horizontal eyeslits in a flat faceplate, vertical acid drainage grooves running down the chest and thigh plates with darkened channels, a large irregular burn scar covering part of the chest, a short torn cape with three melted notches along the hem, heavy greaves and knee guards. Soot, rust pits and scorch marks. Colors: heat-scaled iron #5a2a1e, sulfur brass trim #c9a227, vent glow #ff6a00, cape cloth #7a1020, gem #ff3b6b, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, visible face, bare head, anime face, big eyes, thin legs, clipping cape, floating pieces, clean armor, pretty cape, symmetric spikes, glowing eyes`

**参数**：MJ `--ar 3:4 --style raw --quality 2` ｜ SDXL `896x1152, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, brutal tunnel plate armor, three back-swept shoulder spikes in sockets, acid drainage grooves, burn scar, torn cape with melted notches, #5a2a1e #c9a227 #ff6a00.
```

**调色板**：`#5a2a1e` · `#c9a227` · `#ff6a00` · `#7a1020` · `#ff3b6b`

**要点**：识别点 = 肩刺 + 排酸沟槽 + 烧熔披风；比熔核更脏更凶。

**分件追加句**（在完整 prompt 末尾加一句，只出单件）：

- 头盔：`emphasize the flat faceplate with two eyeslits and the crown plume`
- 胸甲：`emphasize shoulder spikes with sockets, acid grooves and the burn scar`
- 护腿：`emphasize drainage grooves on the thighs, heavy knee guards, torn belt`
- 靴子：`emphasize soot-blackened boots with thick soles and strap plates`

---

### 启明星战甲 · `phosphoros_set`

*金星线 · 护甲 · T5 · 终局 / 护甲 5-10-8-4 / 耐压 130 · 画幅 3:4*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, full-body armor design sheet on a featureless matte grey mannequin: front view, three-quarter view and back view side by side, identical design in all three views, no visible face, dark featureless visor, neutral stand, the final ceremonial aegis: a double halo floating behind the head, an inner disc engraved with an eight-pointed star and an outer ring broken in four places, a crown of two horns slightly unequal in height, gold filigree armor plates with a diamond chest gemstone in a four-claw setting, broad pauldrons trailing into a shoulder mantle lined with dawn-rose cloth visible only at the edge, two bracers with different numbers of engraved lines, hanging tassets and small knee gems. Gold dominates seventy percent of the surface, dawn rose twenty, cyan only as thin light in the halo and gems. Materials: old gold #7a6420 mid #b8912e bright #d4af37 highlight #ffe08a, dawn rose #ff8a5c, halo cyan #67f5ff, garnet #ff3b6b, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, visible face, bare head, anime face, big eyes, thin legs, clipping cape, floating pieces, three equal colors, purple accent, full body glow, extra wings, floating gemstones`

**参数**：MJ `--ar 3:4 --style raw --quality 2` ｜ SDXL `896x1152, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, ceremonial gold aegis, double halo broken in four places, two-horn crown, diamond chest gem, dawn-rose mantle, gold #d4af37 with thin cyan light only in the halo.
```

**调色板**：`#7a6420` · `#b8912e` · `#d4af37` · `#ffe08a` · `#ff8a5c` · `#67f5ff` · `#ff3b6b`

**要点**：识别点 = 双层光环 + 双角冠 + 前胸菱形宝石 + 披肩；金 ≥65%，青色只出现在光环与宝石。

**分件追加句**（在完整 prompt 末尾加一句，只出单件）：

- 头盔：`emphasize the double halo with four breaks, the two horns and a solid gold faceplate`
- 胸甲：`emphasize the diamond chest gem in a four-claw setting and the shoulder mantle`
- 护腿：`emphasize hanging tassets, small knee gems and a segmented gold waist`
- 靴子：`emphasize gold boots with talon toe caps and a ring of small rivets`

---

## 4. 水星线 · 提案（1 件）

水星模块目前**没有任何装备**（只有食物与刷怪蛋）。这条 prompt 是提案稿，用于评估「日冕轻甲」方向是否成立；先出图看调性，再决定是否立项。

### 日冕轻甲（提案） · `mercury_proposal`

*水星线 · 护甲 · T1.5 · 轻甲 / 待立项 · 画幅 3:4*

```text
stylized blocky-industrial game concept art, hard-surface design, chunky silhouette with strong shape separation, painted 2D illustration with 3-value cel shading, matte materials, crisp specular edges, top-left key light with soft ambient fill, single centered subject on a flat neutral warm-grey seamless background, no text, no watermark, no logo, no UI, full-body armor design sheet on a featureless matte grey mannequin: front view, three-quarter view and back view side by side, identical design in all three views, no visible face, dark featureless visor, neutral stand, a very light solar corona armor: thin polished plates of pale gold with black sunspot voids cut into them, a corona arc motif sweeping across the chest, exposed dark underlayer at the joints, a minimal open helmet with a thin visor band, almost no ornament but a high polish. Sleek and thin, the visual opposite of the heavy Venus pressure suits. Colors: solar gold #ffd166, corona orange #ff6a3c, sunspot black #2b1a12, white hot #fff3c4, clean linework, precise small fittings, readable when scaled down to a thumbnail, production concept art, consistent material language.
```

**负面词**：`text, watermark, signature, logo, ui elements, multiple unrelated objects, duplicated parts, cropped, cut off, blurry, jpeg artifacts, low contrast, muddy colors, glossy plastic, neon glow haze, lens flare, oversaturated, photorealistic skin, hands, character, busy background, random sparkles, visible face, bare head, anime face, big eyes, thin legs, clipping cape, floating pieces, heavy armor, spikes, cape, glowing energy lines`

**参数**：MJ `--ar 3:4 --style raw --quality 2` ｜ SDXL `896x1152, steps 32, cfg 6.5, sampler DPM++ 2M Karras, denoise 0.55 (img2img)`

**短版**（超长 prompt 会被 CLIP 截断到 77 token；SD 1.5 / NovelAI / 老版 WebUI 用这条）：

```text
concept art, stylized blocky-industrial hard-surface design, matte materials, top-left light, flat neutral grey background, no text, thin solar corona armor, pale gold plates with black sunspot voids, corona arc across the chest, open visor band, #ffd166 #ff6a3c.
```

**调色板**：`#ffd166` · `#ff6a3c` · `#2b1a12` · `#fff3c4`

**要点**：若立项：与金星'厚、钝、耐酸'形成对照，走'薄、亮、易熔'。

---

## 5. 一致性技巧（多件一起出图时必看）

AI 每张图都会重新发挥，想让 45 件看起来像同一个模组，按下面做：

1. **锁种子**：Midjourney 用 `--seed 12345`，SD 固定 Seed 值；同套装内所有图用同一个种子。
2. **锁风格锚点**：不要手改 §1 的锚点句；想换风格整包一起换。
3. **先出「全家福」再出单品**：先跑一次该阵营 4–6 件的拼图 prompt，挑一张构图作为后续 img2img 的底，风格最稳。
4. **跨套差异**：五套护甲在 prompt 里已经写了互斥的识别词（宽肩焊缝 / 薄膜翼 / 肩刺沟槽 / 双环金光 / 五尖冠），不要删掉这些词，删了就会互相靠拢。
5. **分辨率**：概念图 1024–1536 px 足够；不要直接出 512，细节会糊成一片。
6. **出图后**：用 §9 的验收清单快速筛，不合格的调「必写细节」那几句，而不是堆形容词。

---

## 6. 批量生成

`docs/ai-prompts/equipment-prompts.json` 每项包含：

| 字段 | 含义 |
|---|---|
| `id` | 物品注册名，也是文件名 |
| `name` / `name_zh` | 中英名 |
| `planet` / `kind` / `tier` | 归属、资产类型、阶层 |
| `prompt` | 完整正向提示词（已内联风格锚点） |
| `prompt_pixel` | 像素风版本（末尾已接 64×64 网格句） |
| `prompt_short` | 77 token 以内的短版（SD 1.5 / NovelAI 用） |
| `negative` / `negative_pixel` | 负向提示词 |
| `aspect` / `size` | 画幅与 SDXL 推荐分辨率 |
| `palette` | 该件的 hex 调色板 |
| `piece_variants` | 分件追加句（仅护甲） |

ComfyUI 里可直接用 JSON 的 `prompt` 做批量队列；脚本读法示例：

```python
import json
items = json.load(open('docs/ai-prompts/equipment-prompts.json', encoding='utf-8'))['items']
for it in items:
    print(it['id'], '|', it['size'], '|', it['prompt'][:60], '...')
```

---

## 7. 常见失效与修法

| 症状 | 原因 | 改法 |
|---|---|---|
| 五套甲长得一样 | 删掉了识别词 | 把该套的识别词（如 `broad shoulders, double weld lines`）加回开头 |
| 武器像玩具 | 缺比例描述 | 在 subject 前加 `oversized, heavy, two meters long` |
| 满屏发光 | 负面词不足 | `--no glow haze, bloom, neon`，并把发光词降到 1 处 |
| 护甲变科幻机甲 | 材质词太现代 | 保留 `welded seams, rivets, pressure gauge, cast iron`，删 `sleek, futuristic` |
| 图里出现脸 | armor 框架被忽略 | 强调 `dark featureless visor, no face`，并加负面 `visible face` |
| 颜色跑偏 | 只写 hex 不写色名 | hex 前必须带色名（`obsidian black #2a1512`） |

---

## 8. 调色板速查

| 物品 | 主色 |
|---|---|
| 融核巨剑 | `#14100e` `#2a1512` `#8a3418` `#ff7a3c` `#ffd9a0` `#d8a838` |
| 星核炮 | `#373e3a` `#554a44` `#d8a838` `#ffb03a` `#ffe9b0` |
| 熔核套（四件） | `#2a1512` `#5a2a1e` `#c9a227` `#ff6a00` `#ff3b6b` |
| 沙尘暴护目镜 | `#b08a3a` `#ffb648` `#30230f` `#f5e08a` |
| 便携式冷却器 | `#4c545a` `#8f9aa0` `#48c8c0` `#cfe9ff` `#b08a3a` |
| 冷却液罐（空/水/液氧） | `#3f464b` `#241f1d` `#4aa3d8` `#9fe8ff` `#d8a838` |
| 酸蚀矛 | `#3f464b` `#9aa0a6` `#eaf3f6` `#7dff4a` `#2e5b1e` `#b08a3a` |
| 压力锤 | `#383838` `#8f9aa0` `#48c8c0` `#c2ccd4` `#d8a838` |
| 雷羽弩 | `#3f4750` `#c2ccd4` `#8fd8ff` `#eaf6ff` `#b08a3a` |
| 云钩 | `#cfe9ff` `#8a9199` `#2a4a6a` `#b08a3a` |
| 硫火喷射器 | `#8a9199` `#e8d97a` `#ff9a3c` `#7dff4a` `#30230f` |
| 高压钻 | `#4c545a` `#d5dde2` `#b08a3a` `#48c8c0` `#5a3226` |
| 菌剪 | `#8a9199` `#e8d97a` `#30230f` |
| 云图仪 | `#3f464b` `#48c8c0` `#b08a3a` |
| 气压计 | `#e8d97a` `#30230f` `#8a3418` `#b08a3a` |
| 耐酸镀层 | `#3f464b` `#7dff4a` `#b08a3a` |
| 压力补偿器 | `#6b7379` `#b08a3a` `#241f1d` |
| 散热片 | `#9aa0a6` `#241f1d` `#b08a3a` |
| 气囊背包 | `#cfe9ff` `#b08a3a` `#2a4a6a` |
| 滤芯 | `#6b7379` `#30230f` `#b08a3a` |
| 弹道校准器 | `#3f464b` `#8fd8ff` `#e8d97a` |
| 钨钢压力服 | `#5a6068` `#c9ced4` `#9aa0a6` `#c9a227` `#3a3f45` |
| 云行者 | `#3f5a66` `#cfe9ff` `#7fd8ff` `#2a4a6a` `#9fe8ff` |
| 熔管重甲 | `#5a2a1e` `#c9a227` `#ff6a00` `#7a1020` `#ff3b6b` |
| 启明星战甲 | `#7a6420` `#b8912e` `#d4af37` `#ffe08a` `#ff8a5c` `#67f5ff` `#ff3b6b` |
| 鲸骨号角 | `#e8dcc0` `#a89a7a` `#b08a3a` `#30230f` |
| 压力阀 | `#6b7379` `#b08a3a` `#241f1d` |
| 硫心 | `#e8d97a` `#a8912a` `#5a2a1e` `#fdf6c4` |
| 日冕轻甲（提案） | `#ffd166` `#ff6a3c` `#2b1a12` `#fff3c4` |

---

## 9. 出图验收（30 秒版）

- [ ] 缩到 64 px 还认得出是什么（剪影成立）
- [ ] 主色与 §8 调色板一致，没有第三种抢眼色
- [ ] 该件的「识别点」都在（见各节要点）
- [ ] 没有文字、水印、多余手指、糊掉的细节
- [ ] 护甲没有露出脸；武器没有被手挡住主体

---

## 10. 像素风追加句

把下面这句**接在任意 prompt 末尾**，并把画幅改成 1:1：

```text
render as pixel art on a strict 64x64 grid, hard pixel edges, no anti-aliasing, an 8-color palette, one-pixel dark outline, flat shading only
```

对应负面词追加：

```text
anti-aliasing, gradient dithering, soft blur, 3d render, photograph, smooth gradients
```

*生成于 tools/gen-ai-prompts.py · 覆盖火星线 6 件 / 金星线 22 件 / 水星提案 1 件，共 29 条。*
