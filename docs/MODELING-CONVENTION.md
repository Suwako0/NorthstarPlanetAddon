# 建模约定：所有 mod 模型一律走 Blockbench MCP

自本约定起，本仓库的**所有**模型（护甲装饰件、武器、方块、实体）都按下面的流程做，
不再手写几何或让脚本凭空生成几何。

## 唯一来源

- 设计源文件：`build/scratch/models/<name>.bbmodel`（制作期暂存，`build/` 已被 `.gitignore` 忽略；任务结束删除，不入库）
- 贴图：**存在 bbmodel 里**，用 `detail_cubes` / `paint_faces` 绘制
- 游戏内资源：由脚本从 bbmodel **导出**，不手改

## 硬性要求

1. **box UV**：`pack_uv { mode: "box" }`。Java 的 `CubeListBuilder.texOffs(u,v)` 只支持盒式 UV，
   逐面 UV 无法转换。
2. **骨骼名固定**：护甲用 `head / body / right_arm / left_arm / right_leg / left_leg`，
   原点对齐原版人形（颈 24、臂 22、腿 12，脚底 0）。
3. **不用旋转体**：圆柱/齿轮在 Blockbench 是旋转条带，Java 装饰模型只支持轴对齐方块。
   GeckoLib / Bedrock 目标可以用，`HumanoidModel` 不行。
4. **命名带材质前缀**：`plate_ / trim_ / glow_ / chain_ / leather_ / skin_`。
   导出脚本靠前缀分槽位、分材质；`skin_` 开头是预览用参考件，不导出。
5. **一件只管自己的槽位**：胸甲件挂 body/arm 骨，护腿件挂腿骨，靴子件前缀 `boot`。

## 导出与验收

```bash
python tools/bbmodel-export.py            # 贴图 + box-UV 偏移 -> PARTS 与 assets 贴图
python tools/bbmodel-to-java-parts.py     # 部件表 -> Java 模型类（按需再跑）
```

- `runGameTestServer` 通过
- 逐件检查：`python tools/check-armor.py --set <套装名>`
- 与 Blockbench 截图逐件对比：差异只允许来自光照

## 护甲专用的两条硬规则（踩过坑）

### 1. 装饰件必须比原版护甲壳更外扩

vanilla 的护甲层（`layer_1/2`）是用 `CubeDeformation(1.0)` 画的，也就是**比皮肤外扩 1 单位**。
Blockbench 里看不到这层壳，所以按皮肤尺寸建的护甲片进游戏会被它整个包住，
看到的是被盖住后的暗色外壳 —— 表现为「手上有黑方块」「胸甲穿模」「走动时闪」。

- 生成 Java 时，每个装饰件 `addBox(..., deformation)` 必须带上同一 `deformation`；
- 贴身的护腕 / 大腿 / 胫甲 / 战靴还要再**外扩 ≥0.8**，否则只比壳大 0.1–0.4，照样被盖住。

### 2. 左右对称要在导出时统一

Blockbench 的 mirror + 取整是逐件做的，左右两侧会各自取整，
结果左腿到 +3.2、右腿只到 −3.1。导出脚本现在**只取右侧，左侧由镜像生成**：

```python
local_x' = [-x1, -x0]   # 绕骨骼中线镜像
```

### 3. 薄件不要小于 1 单位

box UV 会把 0.8 高的件取整成 0 像素宽的面（`zero_uv`），游戏里采到无效 UV → 黑块。
所有 cube 尺寸对齐整数、最小边 1 单位。

## 交付前的自动闸门

```bash
python tools/check-armor.py --set <套装名>     # 有 error 就不许交付
```

它拦的是已经踩过一次的坑（详细事故记录见 [ARMOR-PITFALLS.md](ARMOR-PITFALLS.md)）：

| 检查 | 对应事故 |
|---|---|
| 穿戴层 64×32 | — |
| **手部底面 4×4 + 底部 3 行必须透明** | 手上黑方块（漏挖手臂"底面"） |
| 装饰件不得完全埋在护甲壳里 | 件被壳吞/穿模 |
| 手臂/腿骨上不得有套筒大件 | 跟着手臂摆的浮动黑块 |
| 左右肢体件必须互为镜像 | 左右不对称 |
| box-UV 面不得采样透明像素 | 薄件零面积 UV |

### 一句话记忆

> 手只占最后 2–3 格，**侧面和"断面"都要挖**；3D 只做轮廓件，别给肢体套盒子。
