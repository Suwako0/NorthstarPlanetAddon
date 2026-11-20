# molten_core 盔甲模型（17 个部件 / 8 个装饰件）

实测 12 个自定义盔甲模型：平均 9.5 个部件、3.3 个装饰件 → 本模型高于平均线。

| 部件 | 父骨 | box(x,y,z,w,h,d) | UV | 占位 | 装饰 |
|---|---|---|---|---|---|
| crown_left | head | -4,-4,-1,1,4,2 | 0,0 | 6x6 | 是 |
| crown_mid | head | -1,-6,-1,2,6,2 | 8,0 | 8x8 | 是 |
| crown_right | head | 3,-4,-1,1,4,2 | 18,0 | 6x6 | 是 |
| horn_left | head | -6,-3,-1,2,3,2 | 26,0 | 8x5 | 是 |
| horn_right | head | 4,-3,-1,2,3,2 | 36,0 | 8x5 | 是 |
| chest_plate | body | -4.5,0,-2.5,9,6,5 | 46,0 | 28x11 | 甲片 |
| core_gem | body | -1,2,-2.8,2,2,1 | 76,0 | 6x3 | 是 |
| pauldron_left | body | -6,-1,-2.5,3,3,5 | 84,0 | 16x8 | 甲片 |
| pauldron_right | body | 3,-1,-2.5,3,3,5 | 102,0 | 16x8 | 甲片 |
| cape | body | -4,1,2.2,8,10,0.6 | 0,13 | 18x11 | 是 |
| belt | body | -4.2,5,-2.2,8.4,1.5,4.4 | 20,13 | 26x6 | 是 |
| bracer_right | right_arm | -2.2,4,-2.2,4.4,3,4.4 | 48,13 | 18x8 | 甲片 |
| bracer_left | left_arm | -2.2,4,-2.2,4.4,3,4.4 | 68,13 | 18x8 | 甲片 |
| tasset_right | right_leg | -2.2,0,-2.2,4.4,2,4.4 | 88,13 | 18x7 | 甲片 |
| tasset_left | left_leg | -2.2,0,-2.2,4.4,2,4.4 | 108,13 | 18x7 | 甲片 |
| greave_right | right_leg | -2.2,6,-2.2,4.4,5,4.4 | 0,26 | 18x10 | 甲片 |
| greave_left | left_leg | -2.2,6,-2.2,4.4,5,4.4 | 20,26 | 18x10 | 甲片 |

## 渲染规则（脸和手必须可见）

- `renderToBuffer` **只画上表这些装饰件**，不画基础头/身/臂/腿。
- 基础覆盖交给 `molten_core_layer_1/2.png`（原版盔甲层），所以脸、手、腿不会被模型整块盖住。
- 每个装饰件按父骨变换渲染（`head/body/rightArm/...` 的 `translateAndRotate`），会跟随头部转动和摆臂。
- 渲染前调用 `model.setSlot(ArmorItem.Type.HELMET/CHESTPLATE/LEGGINGS/BOOTS)`：只画该槽位对应的装饰件，
  否则头盔渲染时会把披风、护腿、护臂一起画出来。
- 如果非要把甲片做成 3D 并覆盖身体，那也必须保证 layer 贴图在脸部/手部是透明的，否则一定"看不见脸"。

## 贴图分两层

- 模型贴图：`textures/models/armor/molten_core_armor.png`（128x128，上表 UV 对应）
- 原版穿戴层：`textures/models/armor/molten_core_layer_1.png` / `_layer_2.png`（64x32，皮肤式图集）
- 物品图标：`textures/item/molten_core_{helmet,chestplate,leggings,boots}.png`（16x16，4 张）
