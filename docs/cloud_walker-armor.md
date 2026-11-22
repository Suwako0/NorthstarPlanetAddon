# cloud_walker 云行者（参考图重做：79 部件）

参考图：`docs/previews/cloud_walker-reference.jpg`（冰蓝兜帽斥候 + 半透明膜翼 + 破损长袍）。

生成命令：`python tools/gen-cloud-walker-armor.py`
（同一个数据源产出 Java 模型、128x128 图集、bbmodel 预览与本文档）

坐标是原版 `HumanoidModel` 骨内单位：y 向下、模型正面朝 -Z、模型右侧为 -X。

| 部件 | 父骨 | 槽位 | 视觉类型 | box (x,y,z,w,h,d) | UV |
|---|---|---|---|---|---|
| hood_top | head | head | hood | -5.2,-9,-5.4,10,2,10 | 80,0 |
| hood_top2 | head | head | hood | -4.4,-9.8,-4.6,9,1,9 | 170,0 |
| hood_crest | head | head | hood | -1.5,-10.6,-2.4,3,1,6 | 0,54 |
| hood_side_l | head | head | hood | 3.8,-8.9,-5.2,1,10,10 | 122,0 |
| hood_side_r | head | head | hood | -5.2,-8.9,-5.2,1,10,10 | 146,0 |
| hood_back | head | head | hood | -5.2,-8.8,3.6,10,10,1 | 70,22 |
| hood_cheek_l | head | head | hood | 3,-6.9,-5.2,2,8,2 | 140,54 |
| hood_cheek_r | head | head | hood | -4.9,-6.9,-5.2,2,8,2 | 150,54 |
| hood_chin | head | head | hood | -3,0.2,-5.2,6,1,2 | 68,68 |
| hood_flap_l | head | head | hood | 4.2,-5.4,-3.6,1,8,4 | 20,54 |
| hood_flap_r | head | head | hood | -5.6,-5.4,-3.6,1,8,4 | 32,54 |
| cowl_front | body | chest | cloth | -4.8,-1.2,-3.2,10,4,1 | 116,54 |
| cowl_shoulder_l | body | chest | cloth | 2.6,-1.4,-2.8,4,4,6 | 198,22 |
| cowl_shoulder_r | body | chest | cloth | -6.4,-1.4,-2.8,4,4,6 | 220,22 |
| cowl_fold_l | body | chest | cloth_l | 3.2,-0.2,-3.4,3,2,1 | 68,81 |
| cowl_fold_r | body | chest | cloth_l | -5.8,-0.2,-3.4,3,2,1 | 78,81 |
| pauldron_cap_l | body | chest | plate | 4.2,-1.6,-2.8,4,3,6 | 52,40 |
| pauldron_cap_r | body | chest | plate | -7.8,-1.6,-2.8,4,3,6 | 74,40 |
| pauldron_lame_l | body | chest | plate2 | 4.4,1.6,-2.6,3,2,5 | 44,54 |
| pauldron_lame_r | body | chest | plate2 | -7.4,1.6,-2.6,3,2,5 | 62,54 |
| pauldron_tip_l | body | chest | plate | 4.4,3.4,-2.4,3,2,5 | 80,54 |
| pauldron_tip_r | body | chest | plate | -7.2,3.4,-2.4,3,2,5 | 98,54 |
| pauldron_crest_l | body | chest | trim | 4.4,-2.4,-1.8,3,1,4 | 196,54 |
| pauldron_crest_r | body | chest | trim | -7.2,-2.4,-1.8,3,1,4 | 212,54 |
| chest_center | body | chest | plate_l | -0.4,1.6,-2.9,1,7,1 | 44,81 |
| sash | body | chest | cloth | -4.8,8.8,-2.9,10,2,6 | 0,22 |
| sash_v | body | chest | trim | -3.4,9.8,-3,7,1,1 | 50,81 |
| sash_knot | body | legs | cloth | 3.2,8.9,-3.4,2,2,1 | 152,81 |
| backplate | body | chest | plate2 | -4.2,0.4,2,8,8,1 | 148,40 |
| back_spine | body | chest | trim | -0.5,0.8,2.5,1,8,1 | 10,81 |
| bow_knot | body | chest | trim | -1.4,-0.8,3.4,3,2,2 | 234,68 |
| bow_loop_l | body | chest | cloth | -4,-1.4,3.6,3,2,1 | 88,81 |
| bow_loop_r | body | chest | cloth | 1.2,-1.4,3.6,3,2,1 | 98,81 |
| ribbon_back_l | body | chest | ribbon | -2.5,1,4,2,10,1 | 16,68 |
| ribbon_back_r | body | chest | ribbon | 0.9,1,4.2,2,8,1 | 60,68 |
| wing_hub_l | body | chest | plate | 1.8,-1.2,2.4,2,3,2 | 246,68 |
| wing_hub_r | body | chest | plate | -4.2,-1.2,2.4,2,3,2 | 0,81 |
| wing_membrane_l | body | chest | wing | 2.4,-5.4,3.2,18,13,1 | 0,0 |
| wing_membrane_r | body | chest | wing | -20,-5.4,3.2,18,13,1 | 40,0 |
| wing_spar1_l | body | chest | frame | 2.6,-0.4,2.7,5,1,2 | 122,68 |
| wing_spar1_r | body | chest | frame | -8,-0.4,2.7,5,1,2 | 138,68 |
| wing_spar2_l | body | chest | frame | 7.2,-2.2,2.7,6,1,2 | 86,68 |
| wing_spar2_r | body | chest | frame | -13,-2.2,2.7,6,1,2 | 104,68 |
| wing_spar3_l | body | chest | frame | 12.2,-4.2,2.7,5,1,2 | 154,68 |
| wing_spar3_r | body | chest | frame | -17.6,-4.2,2.7,5,1,2 | 170,68 |
| wing_spar4_l | body | chest | frame | 16.8,-3.2,2.7,4,1,2 | 16,81 |
| wing_spar4_r | body | chest | frame | -20.4,-3.2,2.7,4,1,2 | 30,81 |
| wing_claw_l | body | chest | trim | 19.2,-0.6,2.8,2,2,1 | 160,81 |
| wing_claw_r | body | chest | trim | -21,-0.6,2.8,2,2,1 | 168,81 |
| skirt_front_l | body | legs | skirt | 0.2,9.2,-3.4,5,11,1 | 204,40 |
| skirt_front_r | body | legs | skirt | -5.6,9.2,-3.4,5,11,1 | 218,40 |
| skirt_apron | body | legs | skirt | -3.6,9.4,-3.9,7,8,1 | 232,40 |
| skirt_side_l | body | legs | skirt | 4.8,9.2,-3.2,2,10,6 | 34,22 |
| skirt_side_r | body | legs | skirt | -6.4,9.2,-3.2,2,10,6 | 52,22 |
| skirt_back | body | legs | skirt | -5.2,9.2,2.4,10,12,1 | 208,0 |
| skirt_flare_l | body | legs | skirt | 5,13,-3,2,6,1 | 186,68 |
| skirt_flare_r | body | legs | skirt | -7,13,-3,2,6,1 | 194,68 |
| tasset_l | body | legs | plate | 4.2,9,-3.2,2,4,6 | 168,40 |
| tasset_r | body | legs | plate | -6,9,-3.2,2,4,6 | 186,40 |
| knee_l | left_leg | legs | plate | -2.9,3.6,-3.4,6,2,2 | 24,68 |
| knee_r | right_leg | legs | plate | -2.9,3.6,-3.4,6,2,2 | 42,68 |
| shin_l | left_leg | legs | plate2 | -2.7,6,-3.3,5,3,2 | 228,54 |
| shin_r | right_leg | legs | plate2 | -2.7,6,-3.3,5,3,2 | 0,68 |
| thigh_strap_l | left_leg | legs | leather | -2.6,1,-2.9,5,1,1 | 108,81 |
| thigh_strap_r | right_leg | legs | leather | -2.6,1,-2.9,5,1,1 | 122,81 |
| boot_cuff_l | left_leg | boots | plate | -3,9.4,-3,6,2,6 | 0,40 |
| boot_cuff_r | right_leg | boots | plate | -3,9.4,-3,6,2,6 | 26,40 |
| boot_toe_l | left_leg | boots | plate2 | -2.7,10.6,-4.6,5,2,3 | 160,54 |
| boot_toe_r | right_leg | boots | plate2 | -2.7,10.6,-4.6,5,2,3 | 178,54 |
| boot_heel_l | left_leg | boots | plate2 | -2.4,10.8,2,5,1,2 | 202,68 |
| boot_heel_r | right_leg | boots | plate2 | -2.4,10.8,2,5,1,2 | 218,68 |
| sleeve_l | left_arm | chest | cloth | -2.9,-2.4,-2.9,6,4,6 | 94,22 |
| sleeve_r | right_arm | chest | cloth | -2.9,-2.4,-2.9,6,4,6 | 120,22 |
| elbow_fin_l | left_arm | chest | plate | 1.8,1.4,-1,1,2,2 | 136,81 |
| elbow_fin_r | right_arm | chest | plate | -3.2,1.4,-1,1,2,2 | 144,81 |
| bracer_l | left_arm | chest | plate2 | -2.9,3.8,-2.9,6,4,6 | 146,22 |
| bracer_r | right_arm | chest | plate2 | -2.9,3.8,-2.9,6,4,6 | 172,22 |
| cuff_l | left_arm | chest | trim | -3,7.4,-3,6,1,6 | 96,40 |
| cuff_r | right_arm | chest | trim | -3,7.4,-3,6,1,6 | 122,40 |

## 三套贴图

- `cloud_walker_armor.png`（128x128）：3D 装饰件图集；翼膜形状、破损下摆、飘带缺口都画在 alpha 里
- `cloud_walker_layer_1.png`（64x32）：甲面（兜帽布、胸甲、袖甲、胫甲），**脸 (8,8)-(16,16) 与双手 (y 28-32) 镂空**
- `cloud_walker_layer_2.png`（64x32）：外层描边（兜帽边、肩甲、腰封、膝箍、靴口）

## 配色

| 用途 | 颜色 |
|---|---|
| 甲面亮 / 中 / 暗 | `#DAF5FF` / `#C2E8F6` / `#8FC3D8` |
| 布面 | `#BFE6F5`，暗 `#9ECBDE` |
| 青色强调 | `#86EAF2`，亮 `#D9FDFF`，暗 `#40A8C0` |
| 翼膜 | `#E6F9FE`，荧光边 `#9FF6F8` |
| 描线 | `#2E5C6E` |

## 渲染

`renderToBuffer` 只画装饰件，基础甲面交给 layer 贴图；`setSlot` 按部件所属槽位开关
（裙摆挂在 `body` 骨上，但只在穿护腿时画）。脸和手由 layer 贴图开口保证可见。
