# 涅斐勒 · 云鲸 —— AI 出图提示词（可直接粘贴）

> 配套设定：`docs/NEPHELE-ART-BRIEF.md`（尺度、色板、结构全在那里）
> 本文目标：**复制 → 粘贴 → 出图**。四个版本覆盖主流工具，选你用的那一个，其余不用看。
> 题材锁：这是**生物 Boss**，不是人形角色。参考图（蓝发少女）只当**画风参考**喂，绝不能让它把题材带成少女——所有版本都带负向词压这一项。

---

## 0. 选哪一版

| 你用的工具 | 用哪版 | 比例 | 备注 |
|---|---|---|---|
| 即梦 / 可灵 / 豆包 / Qwen-Image / Nano Banana（Gemini）/ GPT 图像 | **§1 中文自然语言** | 16:9 | 长句吃得下，最稳 |
| Midjourney（v7 / niji 6） | **§2 MJ 参数版** | `--ar 16:9` | 去掉 hex，MJ 不认色号 |
| Stable Diffusion / SDXL / WebUI / NovelAI | **§3 Tag 版 + 负向词** | 1024×576 起 | 需要负向词 + CFG |
| **ComfyUI + FLUX.2** | **§5 散文版 + 节点参数** | 1536×864 | **不要用 §3 的权重括号**，Flux 不解析 |
| 任意工具，三视图总是崩 | **§4 两段式** | 1:1 → 16:9 | 先出正面，再用它做参考图补侧背 |

**通用操作**：把参考图（那张蓝发少女）作为「风格参考 / sref / 图生图参考」一起传，权重 0.3–0.5，**只借画风与配色**。

---

## 1. 中文自然语言版（推荐，直接粘）

### 1.1 正向提示词

```
角色三视图设定稿，日系动画赛璐璐着色，冷调，平光，正交视角，无透视。

同一只巨型云鲸生物，正面、侧面、背面三个视角横向等距并排，三个视角完全一致、同一比例、同一基线。

主体造型：一只漂浮在空中的云鲸。冰蓝色与珍珠白的哑光皮革质感，局部有细微珠光。
头部钝圆，吻端平切，嘴是一条平直的下颌线，没有牙齿；眼睛只有两道横向的发光细缝，没有瞳孔，没有眼白。
头顶上方悬着一枚很细的发光光环，环的直径约为体长的三分之一，向后倾斜约十二度，从正面看是一个扁椭圆，环身极细，不是粗环也不是实心圆盘。
躯干分为前中后三段，接缝处略微收窄；背部铺开四片半透明的冰晶甲片，每片边缘有一圈冷白色亮线，甲片之间的接缝清晰、可以数清是四片。
背脊有五片半透明的背鳍，从头顶后方到腰后由大到小依次变短。
两侧胸鳍宽大如翼，展开成半透明的云膜，膜里能看到三到四道骨架分叉，鳍尖略微下垂。
腹部有五道横向的褶皱。
背部中线偏上的位置有两个半透明白色的气囊，圆润、不发光。
一条长尾先向后延伸，再向下垂落，尾巴两侧各有一条蓝白渐变的缎带，末端是几何切角。
尾端收成一圈十二片层层收拢的鳍片，构成一个散热环，环面朝后，环内透出唯一的暖色橙光，环外的皮肤被烤得微微发黑。

整体：主体悬浮，腹部离地约体高的五分之一，没有腿、没有脚、没有地面接触。

画面：中性灰的纯色背景，没有地面，没有投影，没有装饰元素。三个视图占画面高度约四分之三。
柔和的左上方主光，加冷蓝色环境光，背面视图能看到轮廓光。材质细节清晰。
画面里没有文字，没有标注，没有箭头，没有边框，没有水印。
```

### 1.2 负向提示词（有负向框就填，没有就跳过）

```
人形，少女，女孩，人脸，头发，双马尾，裙装，腿，靴子，手指，手持物，写实照片，
三维塑料渲染，透视，仰视，俯视，鱼眼，地面，阴影投影，渐变背景，雾气，文字，标注，箭头，
水印，签名，边框，UI，模糊背景，过曝，低分辨率，畸形肢体，多出的鳍，重复部件，
粗光环，实心圆盘，像素风，Q版，玩具感，纯色平涂大色块
```

### 1.3 参数

- 比例 **16:9**（2000×1125 或 1619×971）
- 一次出 4 张，挑三视图最完整的；不满意只改一句话再出（见 §5 修法）
- 参考图：作为**风格参考**上传，强度 0.3–0.5

---

## 2. Midjourney 版（v7 / niji 6）

去掉十六进制约束（MJ 基本忽略它们），只留可读的形与色，句尾挂参数。

### 2.1 正向

```
character turnaround sheet, three views of the same creature in a row: front view, side view, back view, evenly spaced, identical scale, identical baseline, orthographic, no perspective -- anime cel shading, cool palette, flat lighting, plain neutral grey background, no ground, no shadow, no text, no watermark, no border -- a giant floating cloud whale creature, icy blue and pearl white matte leather skin, blunt rounded snout, flat-cut jaw, two thin glowing horizontal eye slits with no pupils, a very thin glowing halo ring hovering above its head tilted back about twelve degrees, three-segment torso, four translucent ice-crystal back plates with pale cyan rim highlights and countable seams, five translucent dorsal fins decreasing in size, wide pectoral fins spread into translucent cloud membranes with visible rib forks, five belly folds, two translucent white air sacs on the upper back, a long tail extending backward then drooping down with two blue-white gradient ribbon streamers with cut corners, the tail ends in a vent ring of twelve folded fins with the only warm orange glow inside, the creature floats with its belly about one fifth of its height above the ground line
```

### 2.2 参数与排列

```
--ar 16:9 --style raw --v 7 --seed 1234 --sref <你的参考图URL> --sw 350
```

- 动画风更强就把 `--v 7` 换成 **`--niji 6`**（同时删掉 `--style raw`）
- 想固定同一只，加 `--seed`；换姿态时保留 seed 只改造型词
- 三张出片里取一张，再用 `--cref` 指向它微调（§4）

### 2.3 MJ 负向

```
--no humanoid, girl, face, hair, skirt, legs, boots, hands, realistic photo, 3d render, perspective, ground, shadow, gradient background, text, watermark, border, thick halo ring, solid disc
```

---

## 3. Stable Diffusion / SDXL / NovelAI Tag 版

SD 吃 tag 不吃长句；色号写成描述词，权重靠括号。

### 3.1 正向 Tag

```
(character sheet:1.3), (turnaround sheet:1.2), (three views:1.3), front view, side view, back view,
same creature, evenly spaced, identical scale, orthographic, no perspective,
full body, (giant floating whale creature:1.2), flying, hovering,
(icy blue and pearl white:1.2), matte leather texture, subtle pearlescent sheen,
blunt rounded snout, flat cut jaw, (thin glowing horizontal eye slits, no pupils:1.15),
(thin glowing halo ring above head:1.3), shallow ellipse halo, tilted back,
three segment body, (four translucent ice crystal back plates:1.25), pale cyan rim light on plate edges,
five translucent dorsal fins, decreasing size,
wide pectoral fins, translucent cloud membrane, visible rib bones inside fin,
five belly folds, two translucent white air sacs on upper back,
long tail, drooping tail, (vent ring of twelve folded fins:1.2), warm orange glow inside the vent ring,
two blue-white gradient ribbon streamers, angled cut ends,
floating, no legs, no feet,
(plain neutral grey background:1.3), no ground, no shadow,
anime cel shading, clean lineart, flat color, cool ambient light, top left key light, rim light,
best quality, highres, intricate detail, crisp material detail
```

### 3.2 负向 Tag

```
(humanoid:1.3), (girl:1.3), face, hair, twin tails, skirt, dress, legs, boots, hands, fingers,
realistic photo, 3d render, plastic, perspective, foreshortening, low angle, high angle, fisheye,
ground, floor, cast shadow, gradient background, fog, scenery, props,
text, label, arrow, watermark, signature, border, ui, frame,
blurry, lowres, jpeg artifacts, overexposed, extra fins, duplicate parts, fused parts, missing head,
thick halo, solid disc halo, pixel art, chibi, toy, flat single color block
```

### 3.3 参数

| 项 | 值 |
|---|---|
| 分辨率 | 1024×576（SDXL 直接出；SD1.5 先 512×288 再 hires ×2） |
| 采样 | DPM++ 2M Karras / Euler a（动画模型用后者） |
| Steps / CFG | 30 / **5.5–6.5**（CFG 超过 7 会把三视图压成一张） |
| Seed | 固定一个，只改提示词微调 |
| 模型 | 二次元系（Nai3 / AnimagineXL / Illustrious 类）；写实模型出来会是塑料鲸 |
| Hires fix | 1.5×，denoise 0.35（高了会把三视图重新洗掉） |

---

## 4. 三视图总是不稳？两段式（成功率最高）

单张模型的「三视图」是它最弱的技能，崩了别硬怼，改成两步：

**第 1 步（1:1，只出正面）**

```
角色设定图，日系动画赛璐璐着色；一只冰蓝与珍珠白的巨型云鲸从正面看向镜头，
钝圆吻端、平切下颌、两道横向发光眼缝；头顶悬一枚极细的发光光环，正视呈扁椭圆；
两侧胸鳍展开成半透明云膜；背部四片半透明冰晶甲片露出上缘；腹部五道褶皱；
长尾垂在身后、尾端一圈鳍片散热环透出橙光；身体悬浮；
中性灰纯色背景，无地面无投影，柔光，材质清晰，无文字无水印
```

负向同上 §1.2。**出到满意的一张就停**。

**第 2 步（转身）**：把这张正面图作为**参考图 / 图生图**（SD 用 ControlNet reference，MJ 用 `--cref <URL> --cw 100`），提示词只改视角句：

```
同一只生物，转为侧面视角，头朝画面左，其余造型完全不变；正交视角，无透视；中性灰背景，无地面无投影，无文字
```

```
同一只生物，转为背面视角，其余造型完全不变；要能数清背部四片甲片与两个气囊；正交视角，无透视；中性灰背景，无地面无投影，无文字
```

**第 3 步**：三张图拼成一张（任意拼图工具），或在同一提示词里用「同一只」+ 固定 seed 重出三视图。这比一步出三视图稳得多，而且三张的一致性更高。

---

## 5. ComfyUI + FLUX.2 节点参数

FLUX.2 有三条不同的参数线，**先确认你加载的是哪个 checkpoint**，填错了会比参数本身更致命。

### 5.1 空Latent图像（Flux2）

| 项 | 填 | 说明 |
|---|---|---|
| width / height | **1536 × 864** | 精确 16:9，三视图横排才放得下；**必须是 16 的倍数**（latent 为 /16，填 1000 会报错） |
| 显存吃紧 | 1024 × 576 | 同为精确 16:9，出完再 hires 1.5×（降噪 0.3） |
| batch_size | **4**（显存不够降到 2） | 三视图崩坏率高，靠多张挑 |

> 必须用 `EmptyFlux2LatentImage`，**不能**换成普通 Empty Latent Image —— Flux2 的潜空间布局与 Flux.1 不同。

### 5.2 K采样器

| 模型 | cfg | 步数 | 采样器 / 调度器 | 降噪 | 负面条件 |
|---|---|---|---|---|---|
| **FLUX.2 dev**（工作流带 Guidance/引导 节点，填 2.5–3.5） | **1.0** | 24–28 | euler / simple | 1.00 | **留空** |
| **FLUX.2 [klein] base**（CFG-based） | **4.2–4.5** | 24–30 | euler 或 dpmpp_2m / simple | 1.00 | 可填，但要短 |
| **FLUX.2 [klein] 蒸馏 4 步** | **1.0** | **4** | euler / simple | 1.00 | **留空** |

- **判据：cfg > 1 才真的跑负面条件**。cfg=1 时负面框是装饰——[BFL 的指南明确写 FLUX.2 不支持负面提示词](https://www.versely.studio/blog/why-flux-ignores-your-negative-prompt)，此时「人形 / 少女」压不住，只能靠正向描述写清楚。
- 种子：探索期 `randomize`；出到满意构图后切 `fixed`，之后只改提示词迭代。
- 采样器固定用 euler/simple 或 dpmpp_2m/simple；**别用** dpmpp_3m_sde、uni_pc，容易把三个视角糊成一个。

### 5.3 Flux 专用英文散文提示词（本任务主力）

Flux **不解析** `(word:1.2)` 权重，括号会作为字面文本进 T5，所以这里全是句子、零括号：

```
Character turnaround sheet on a plain neutral grey background. The same giant floating cloud whale is shown three times in a row: front view, side view and back view, evenly spaced, identical scale, identical baseline, orthographic camera, no perspective. Its skin is icy blue and pearl white matte leather with a faint pearlescent sheen. The head is blunt and rounded with a flat cut jaw and no teeth; the eyes are two thin glowing horizontal slits with no pupils. A very thin glowing halo ring floats in the air above its head, tilted back about twelve degrees so it reads as a flat ellipse from the front; the ring is a thin wire, not a thick band and not a solid disc. The torso has three segments. Four translucent ice crystal plates lie across its back, each edged with a pale cyan rim light, with clear seams so the four plates can be counted. Five translucent dorsal fins run along the spine, getting shorter toward the tail. Wide pectoral fins spread out like wings into translucent cloud membranes with visible rib forks. Five folds cross its belly. Two translucent white air sacs sit on the upper back on either side of the spine. A long tail runs backward and then droops down, flanked by two blue-white gradient ribbon streamers with angled cut ends; its tip closes into a vent ring of twelve folded fins glowing with the only warm orange light in the image. The creature floats with its belly about one fifth of its body height above the ground line, and it has no legs and no feet. Soft top left key light, cool blue ambient light, visible rim light on the back view. Anime cel shading, clean flat colour, crisp material detail. No text, no labels, no arrows, no border, no watermark.
```

若模型支 CFG 且你想给负面：一句话、只写可见缺陷，**别**在负面里重复正向内容（`humanoid, girl, face, hair, skirt, legs, boots, realistic photo, 3d render, perspective, ground, cast shadow, text, watermark, thick halo ring, solid disc`）。

### 5.4 三个 Flux 专属陷阱

1. **权重括号无效** —— 别把 §3 的 Tag 版搬进 Flux；
2. **中文条件偏弱** —— 三视图这种结构严苛的活，用上面这段英文散文，中文留给即梦/可灵；
3. **改图别用降噪 1.00** —— 那是 txt2img 的值；做两段式转身时图生图要降到 0.55–0.7。

---

### 5.5 加载VAE / 解码

VAE 节点**只有文件名一个可填项**，没有其它参数。要点是"配对 + 接线 + 精度"三件事：

| 项 | 规定 |
|---|---|
| 用哪个文件 | **必须是 FLUX.2 系 VAE**：`flux2-vae_small.safetensors`（轻量解码器，快、省显存、细节略软）或全量 `flux2-vae.safetensors` |
| 不能用 | Flux.1 的 `ae.safetensors`、SDXL / SD1.5 的 VAE —— 出图会整体发灰或直接报错 |
| 接线 | 同一个 VAE 要同时喂给 **VAE解码**（出图）与 **VAE编码**（做两段式转身的图生图）；别挂两个加载器 |
| 高分辨率 | 1536×864 若显存顶不住，用 **VAE解码（平铺）**，tile 512、overlap 64；平铺可能出接缝，只在必要时用 |
| 精度 | 30/40 系默认 bf16 即可；出黑图 / NaN 时在启动参数关掉 `--fp16-vae` 或用 `--bf16-vae` |

**什么时候必须从 `_small` 换全量 VAE**：本任务贴图里有一圈 1 px 的冷白边缘亮线（甲片、鳍片），如果出图后这圈亮线发虚、四片背甲的接缝粘连成一坨，那就是 small 解码器的细节衰减——换全量 VAE 重出同一张（同 seed，只换 VAE）。

**症状对照**：

| 症状 | 原因 |
|---|---|
| 整体灰蒙、对比度低 | 用了 Flux.1 / SDXL 的 VAE |
| 纯黑图或散点噪点 | VAE 精度不匹配（fp16 跑 bf16 VAE） |
| 构图对了但细节软、细线糊 | `_small` 轻量解码器 → 换全量 |
| 平铺块之间有横向接缝 | 平铺解码 → 降 tile 尺寸、加 overlap，或降分辨率直出 |

---

## 6. 出图后照这张清单打勾（不过关就重出，别后期修）

| # | 检查项 | 不过关时怎么改提示词 |
|---|---|---|
| 1 | **三视图**真的三个、等距、同比例 | 把「三视图」提到提示词**第一句**；SD 把 `(three views:1.3)` 权重提到 1.5；CFG 降到 5.5 |
| 2 | 光环是**细环**、悬空、不与头相连 | 加 `a very thin wire-like halo ring, floating with a visible gap above the head`；负向加 `attached halo, crown` |
| 3 | **尾巴下垂**到尾端环接近最低点 | 加 `the tail droops all the way down, the vent ring hangs at the lowest point of the body` |
| 4 | 背部**能数清 4 片甲片** | 加 `exactly four back plates, seams clearly visible` |
| 5 | 两个气囊在**背部中线偏上** | 加 `two air sacs on the upper back near the spine, not on top of the back` |
| 6 | 没有**地面 / 投影** | 负向补 `ground plane, cast shadow, floor reflection` |
| 7 | 没有文字水印 | 负向补 `text, letters, signature, watermark` |
| 8 | 全身只有**一处橙光** | 加 `the only warm color is inside the tail vent ring`；负向加 `orange glow on body, warm lighting overall` |
| 9 | 不像**少女 / 人形** | 负向权重加到 1.4：`(no humanoid:1.4), girl, anime girl` |
| 10 | 不是**塑料 3D 或写实鲸** | 加 `2d illustration, cel shading, flat color`；负向加 `3d render, octane, photorealistic` |

---

## 7. 一句话总结

主力用 **§1 中文版**（或 **§2 MJ 版**）；三视图崩了就切 **§4 两段式**——先出正面单图，再用它转身，这是这类生物设定稿成功率最高的路径。返回去对 `NEPHELE-ART-BRIEF.md` §4.2 的逐视图清单，缺「光环 / 尾下垂 / 尾环在最低点」三条中的任何一条都直接重出。
