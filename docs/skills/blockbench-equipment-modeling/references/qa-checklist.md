# 交付前清单（装备）

按顺序过，任何一项不过就不算完成。

## A. Blockbench 项目内

- [ ] `check_model`：`coplanar_overlap` = 0、`hidden_cube` = 0、`zero_uv` = 0、`floating_part` = 0（有意悬空件除外）
- [ ] 所有 cube 尺寸为整数、每条边 ≥ 1
- [ ] 外扩（clearance）已在 `pack_uv` **之前**完成
- [ ] 左右件由单侧镜像，位置完全对称
- [ ] 贴图在项目内（不是外部 PNG 链接），`detail_cubes` 上色，无纯色填充块
- [ ] `save_project` 到 `docs/models/<name>.bbmodel`
- [ ] 截图两张：正视图 + 四分之三视图（留档用）

## B. 装备类型专项

**穿戴件（护甲 / 饰品）**

- [ ] 骨架用原版人形六骨，原点与原版一致
- [ ] 走分层：脸、手、**手部断面**窗口 alpha = 0；脚底断面按设计取舍
- [ ] 走纯 3D：`layer_1/2` 全透明，且 3D 件覆盖全身（手/脸按设计留空）
- [ ] 槽位可见性按**部件**判断，不按骨骼
- [ ] 部件数 ≥ 35，装饰件占比 ≥ 15%

**手持件（武器 / 工具）**

- [ ] 握把中心对齐模型原点
- [ ] display 用对预设（短刃 `handheld` / 长武器 `big_handheld`）
- [ ] 长度按原版量级，没有靠 scale 硬撑
- [ ] 刃/头分段收窄，不是单块方块
- [ ] element 数 ≥ 15（低于这个数就是"棍子 + 刀片"）
- [ ] 若需要 GUI 图标，另出 2D `_gui.png`

## C. 导出链

- [ ] 导出脚本从 `.bbmodel` 读写：贴图 PNG 原样落到 `assets/`，`uv_offset` 落到几何
- [ ] 图集尺寸与运行时代码里的尺寸常量一致（`texture_size`、`LayerDefinition.create(mesh, W, H)`）
- [ ] `inflate` 被烘进导出坐标
- [ ] 资源路径与注册名一致：`<modid>:<name>` → `textures/.../<name>.png` + `models/.../<name>.json` + `lang` 条目

## D. 验证

```bash
# 工程自带校验（有就一定要跑）
python tools/check-armor.py --set <套装>        # 穿戴件
node  <skill>/scripts/check-assets.mjs <工程根>  # 资源齐套

# 编译
.\gradlew.bat build

# 游戏内（唯一可信的验收）
.\gradlew.bat runGameTestServer   # 日志出现 "All N required tests passed :)"
```

- [ ] `build` 通过
- [ ] `GameTestServer` 通过（Gradle 退出码不可信，只看日志那行）
- [ ] 已提醒用户**重启客户端**（客户端模型与贴图不热重载）

## E. 汇报

- [ ] 列出改动的文件路径
- [ ] 贴出验证命令的实际输出（不要写"应该能跑"）
- [ ] 贴 Blockbench 截图 + 游戏内截图（游戏内截图由用户提供）
- [ ] 未解决项要明说，不要模糊成"已优化"

## Token 预算（经验值）

| 做法 | 影响 |
|---|---|
| 一次调用塞 40+ 个 cube | 单次返回可占数万 token，改用 `execute_script` 只回计数 |
| 每步都截图 | 每张图约 1–2k token，只在需要判断时拍 |
| 反复读整份生成的 Java | 用 `grep`/行号定位 |
| 盲改 | 最贵：一次盲改 = 全量重打包 + 重上色 + 重编译，改三轮就是几十万 token |

**结论：宁可在建模前多问一句，也不要在游戏里试错。**
