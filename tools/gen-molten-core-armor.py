#!/usr/bin/env python3
"""Build the molten_core armour set: Java model, 128 atlas, 64x32 layers, icons.

One data source -> every asset, so the model and the paint never drift apart.
Coordinates are vanilla HumanoidModel bone-local units (y is DOWN: the head sits
at -8..0, the torso at 0..12).

Usage:
  python tools/gen-molten-core-armor.py            # write everything
  python tools/gen-molten-core-armor.py --preview  # also print an ASCII front view
"""
import os
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(HERE, "src", "main", "resources", "assets", "northstar_planet_addon")
JAVA = os.path.join(HERE, "src", "main", "java", "com", "suwako", "northstarplanetaddon",
                    "mars", "client", "model", "MoltenCoreArmorModel.java")

# ---------------------------------------------------------------- palette
C = {
    "plate_d": (0x32, 0x2D, 0x2E, 255),
    "plate": (0x4A, 0x45, 0x46, 255),
    "plate_l": (0x5E, 0x58, 0x59, 255),
    "plate_h": (0x7A, 0x72, 0x70, 255),
    "brass_d": (0x5E, 0x45, 0x1C, 255),
    "brass": (0x8A, 0x6A, 0x2A, 255),
    "brass_l": (0xC8, 0xA0, 0x50, 255),
    "glow_d": (0x7A, 0x24, 0x10, 255),
    "glow": (0xE0, 0x5A, 0x1E, 255),
    "glow_l": (0xFF, 0xA0, 0x3C, 255),
    "glow_h": (0xFF, 0xD9, 0xA0, 255),
    "dark": (0x17, 0x14, 0x15, 255),
    "clear": (0, 0, 0, 0),
}

# ---------------------------------------------------------------- atlas regions
# uv is in 0..16 units (texture 128 -> 1 unit = 8 px)
REGION = {
    "plate": (0, 0, 8, 8),      # px (0,0)-(64,64)      主甲面
    "trim": (8, 0, 12, 8),      # px (64,0)-(96,64)     黄铜
    "gem": (12, 0, 16, 4),      # px (96,0)-(128,32)    六边形熔核宝石
    "steel": (12, 4, 16, 8),    # px (96,32)-(128,64)   亮钢
    "glow": (0, 8, 6, 16),      # px (0,64)-(48,128)    纯发光（眼缝/靴底）
    "chain": (6, 8, 9, 16),     # px (48,64)-(72,128)   链节
    "dark": (9, 8, 12, 16),     # px (72,64)-(96,128)   深缝
}

# ---------------------------------------------------------------- parts
# (name, bone, x0,y0,z0, x1,y1,z1, region)
PARTS = [
    ("plate_pauldron_lip_left", "body", -12.0, 2.0, -5, -9.0, 6.0, 5, 0, 35),
    ("plate_pauldron_left", "body", -11.05, -1.05, -4.05, -3.95, 4.05, 5.05, 66, 19),
    ("plate_pauldron_spike_b_left", "body", -10.0, -6.0, -1, -8.0, -1.0, 2, 62, 87),
    ("plate_pauldron_spike_a_left", "body", -8.05, -5.05, -2.05, -5.95, -0.95, 2.05, 38, 87),
    ("chain_3_left", "body", -7.1, 7.9, -5.1, -5.9, 10.1, -3.9, 96, 104),
    ("chain_4_left", "body", -7.05, 9.95, -5.05, -5.95, 12.05, -3.95, 106, 104),
    ("chain_1_left", "body", -7.0, 4.0, -5, -6.0, 6.0, -4, 76, 104),
    ("chain_2_left", "body", -7.0, 6.0, -5, -6.0, 8.0, -4, 86, 104),
    ("plate_collar", "body", -6.15, -1.15, -4.15, 6.15, 1.15, 4.15, 0, 76),
    ("plate_tasset_left", "body", -6.05, 11.95, -4.05, -2.95, 15.05, -2.95, 95, 97),
    ("plate_pauldron_spike_c_left", "body", -6.0, -4.0, -1, -4.0, -1.0, 2, 0, 97),
    ("plate_back", "body", -5.1, -0.1, 2.9, 5.1, 10.1, 4.1, 74, 50),
    ("plate_chest", "body", -5.0, 0.0, -4, 5.0, 10.0, 3, 46, 0),
    ("leather_belt", "body", -5.0, 10.0, -4, 6.0, 12.0, 3, 64, 76),
    ("trim_strap_left", "body", -3.2, 11.8, -4.2, -1.8, 15.2, -2.8, 81, 97),
    ("trim_core_ring", "body", -3.1, 1.9, -4.1, 4.1, 7.1, -2.9, 91, 87),
    ("glow_core", "body", -2.15, 2.85, -4.15, 2.15, 6.15, -2.85, 65, 97),
    ("trim_buckle", "body", -2.15, 9.85, -4.15, 2.15, 12.15, -2.85, 60, 104),
    ("plate_tasset_mid", "body", -1.1, 11.9, -4.1, 1.1, 15.1, -2.9, 104, 97),
    ("trim_strap_right", "body", 1.8, 11.8, -4.2, 3.2, 15.2, -2.8, 76, 97),
    ("plate_tasset_right", "body", 2.9, 11.9, -4.1, 6.1, 15.1, -2.9, 86, 97),
    ("plate_pauldron_right", "body", 3.95, -1.05, -4.05, 11.05, 4.05, 5.05, 33, 19),
    ("plate_pauldron_spike_c_right", "body", 5.0, -4.0, -1, 7.0, -1.0, 2, 108, 87),
    ("chain_3_right", "body", 5.9, 7.9, -5.1, 7.1, 10.1, -3.9, 91, 104),
    ("plate_pauldron_spike_a_right", "body", 5.95, -5.05, -2.05, 8.05, -0.95, 2.05, 25, 87),
    ("chain_4_right", "body", 5.95, 9.95, -5.05, 7.05, 12.05, -3.95, 101, 104),
    ("chain_1_right", "body", 6.0, 4.0, -5, 7.0, 6.0, -4, 71, 104),
    ("chain_2_right", "body", 6.0, 6.0, -5, 7.0, 8.0, -4, 81, 104),
    ("plate_pauldron_lip_right", "body", 9.0, 2.0, -5, 12.0, 6.0, 5, 99, 19),
    ("plate_pauldron_spike_b_right", "body", 9.0, -6.0, -1, 11.0, -1.0, 2, 51, 87),
    ("plate_helm_back", "head", -5.2, -8.2, 3.8, 5.2, 0.2, 5.2, 41, 76),
    ("plate_helm_side_left", "head", -5.15, -8.15, -5.15, -3.85, 0.15, 5.15, 23, 0),
    ("plate_crown_out_b_left", "head", -5.1, -12.1, -1.1, -2.9, -10.9, 1.1, 51, 104),
    ("plate_crown_out_a_left", "head", -5.05, -11.05, -1.05, -2.95, -8.95, 1.05, 47, 97),
    ("plate_helm_top", "head", -5.0, -9.0, -5, 5.0, -8.0, 5, 33, 50),
    ("plate_cheek_left", "head", -4.25, -4.25, -5.25, -2.75, -0.75, -1.75, 82, 87),
    ("plate_visor_brow", "head", -4.2, -6.2, -5.2, 4.2, -3.8, -3.8, 0, 104),
    ("plate_helm_chin", "head", -3.2, -1.2, -5.2, 3.2, 0.2, -3.8, 13, 108),
    ("plate_crown_in_a_left", "head", -3.0, -12.0, -1, -1.0, -9.0, 1, 29, 97),
    ("plate_crown_in_b_left", "head", -3.0, -13.0, -1, -1.0, -12.0, 1, 33, 104),
    ("plate_crown_back", "head", -1.1, -11.1, 1.9, 1.1, -8.9, 4.1, 56, 97),
    ("plate_crown_mid_a", "head", -1.05, -12.05, -1.05, 1.05, -8.95, 1.05, 11, 97),
    ("plate_crown_mid_b", "head", -0.1, -14.1, -0.1, 1.1, -11.9, 1.1, 19, 104),
    ("plate_crown_in_a_right", "head", 1.0, -12.0, -1, 3.0, -9.0, 1, 20, 97),
    ("plate_crown_in_b_right", "head", 1.0, -13.0, -1, 3.0, -12.0, 1, 24, 104),
    ("plate_cheek_right", "head", 2.75, -4.25, -5.25, 4.25, -0.75, -1.75, 73, 87),
    ("plate_crown_out_b_right", "head", 2.9, -12.1, -1.1, 5.1, -10.9, 1.1, 42, 104),
    ("plate_crown_out_a_right", "head", 2.95, -11.05, -1.05, 5.05, -8.95, 1.05, 38, 97),
    ("plate_helm_side_right", "head", 3.85, -8.15, -5.15, 5.15, 0.15, 5.15, 0, 0),
    ("plate_bracer_right", "left_arm", -2.45, 1.95, -4.45, 4.45, 8.05, 4.45, 81, 0),
    ("plate_upperarm_right", "left_arm", -2.4, -1.0, -3.4, 4.4, 3.0, 3.4, 97, 50),
    ("plate_boot_right", "left_leg", -4.4, 9.9, -4.5, 4.6, 12.1, 4.5, 29, 64),
    ("plate_thigh_right", "left_leg", -3.35, -0.05, -3.45, 3.55, 7.05, 3.45, 27, 35),
    ("plate_shin_right", "left_leg", -3.3, 6.0, -4.4, 3.5, 10.0, 4.4, 85, 35),
    ("plate_boot_cap_right", "left_leg", -2.1, 9.8, -4.2, 3.3, 12.2, 3.2, 101, 76),
    ("plate_knee_right", "left_leg", -2.05, 4.85, -4.15, 3.25, 7.15, -2.85, 111, 104),
    ("plate_bracer_left", "right_arm", -4.45, 1.95, -4.45, 2.45, 8.05, 4.45, 0, 19),
    ("plate_upperarm_left", "right_arm", -4.4, -1.0, -3.4, 2.4, 3.0, 3.4, 0, 64),
    ("plate_boot_left", "right_leg", -4.5, 10.0, -4.4, 4.3, 12.0, 4.4, 66, 64),
    ("plate_shin_left", "right_leg", -3.55, 5.95, -4.45, 3.35, 10.05, 4.45, 0, 50),
    ("plate_thigh_left", "right_leg", -3.5, 0.0, -3.4, 3.3, 7.0, 3.4, 56, 35),
    ("plate_boot_cap_left", "right_leg", -3.25, 9.85, -4.15, 2.05, 12.15, 3.15, 0, 87),
    ("plate_knee_left", "right_leg", -3.2, 4.9, -4.1, 2.0, 7.1, -2.9, 0, 108),
]

BONES = ["head", "body", "right_arm", "left_arm", "right_leg", "left_leg"]
BONE_PIVOT = {  # vanilla HumanoidModel rest pivots, for the preview only
    "head": (0, 0, 0),
    "body": (0, 0, 0),
    "right_arm": (-5, 2, 0),
    "left_arm": (5, 2, 0),
    "right_leg": (-1.9, 12, 0),
    "left_leg": (1.9, 12, 0),
}


def fmt(v):
    if abs(v - round(v)) < 1e-6:
        return str(int(round(v)))
    return f"{round(v, 3)}F"


def java_name(part):
    out = []
    for i, chunk in enumerate(part.split("_")):
        out.append(chunk if i == 0 else chunk.capitalize())
    return "".join(out)


def slot_of(part_name):
    """which armour slot a deco part belongs to (chest / legs / boots / head)"""
    if part_name.startswith(("plate_helm", "plate_spike", "plate_cheek", "plate_brow")):
        return "head"
    if part_name.startswith(("plate_boot", "trim_boot")):
        return "boots"
    if part_name.startswith(("plate_thigh", "plate_knee", "plate_shin", "plate_tasset")):
        return "legs"
    return "chest"


SLOT_FLAG = {"head": "showHead", "chest": "showChest", "legs": "showLegs", "boots": "showBoots"}


# ---------------------------------------------------------------- java
from PIL import Image as _I

_ATLAS = os.path.join(ASSETS, 'textures', 'models', 'armor', 'molten_core_armor.png')
if os.path.exists(_ATLAS):
    _im = _I.open(_ATLAS)
    TEX_W, TEX_H = _im.size
else:
    TEX_W, TEX_H = 128, 128


def emit_java():
    fields = "\n".join(f"    private final ModelPart {java_name(p[0])};" for p in PARTS)
    ctor_lines = []
    for bone in BONES:
        ctor_lines.append(f'        ModelPart {java_name(bone)}Part = root.getChild("{bone}");')
    for p in PARTS:
        ctor_lines.append(f'        this.{java_name(p[0])} = {java_name(p[1])}Part.getChild("{p[0]}");')

    layer = []
    for bone in BONES:
        layer.append(f'        PartDefinition {bone} = root.getChild("{bone}");')
    for name, bone, x0, y0, z0, x1, y1, z1, u, v in PARTS:
        w, h, d = x1 - x0, y1 - y0, z1 - z0
        layer.append(
            f'        {bone}.addOrReplaceChild("{name}",\n'
            f'                CubeListBuilder.create().texOffs({u}, {v})'
            f'.addBox({fmt(x0)}, {fmt(y0)}, {fmt(z0)}, {fmt(w)}, {fmt(h)}, {fmt(d)}),\n'
            f'                PartPose.offset(0, 0, 0));')

    render = []
    for bone in BONES:
        mine = [p for p in PARTS if p[1] == bone]
        if not mine:
            continue
        render.append("        poseStack.pushPose();")
        render.append(f"        this.{java_name(bone)}.translateAndRotate(poseStack);")
        for p in mine:
            flag = SLOT_FLAG[slot_of(p[0])]
            render.append(f"        if ({flag}) {{")
            render.append(f"            this.{java_name(p[0])}.render(poseStack, buffer, packedLight, packedOverlay, color);")
            render.append("        }")
        render.append("        poseStack.popPose();")

    java = f'''package com.suwako.northstarplanetaddon.mars.client.model;

import com.suwako.northstarplanetaddon.client.render.ArmorDecoModel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;

/**
 * MoltenCoreArmorModel — 熔核套的立体装饰件（{len(PARTS)} 部件）。
 *
 * <p>对着参考图重做：五尖冠头盔 + 发光眼缝、塔式肩甲 + 上翘尖、胸口熔核环、
 * 铜腰带 + 方扣、链饰、膝甲、赤热靴底。</p>
 *
 * <p>贴图：assets/northstar_planet_addon/textures/models/armor/molten_core_armor.png（128x128）。
 * 只画装饰件，身体本体交给 molten_core_layer_1/2.png；手部窗口仍然镂空。</p>
 *
 * <p>生成脚本：tools/gen-molten-core-armor.py（改形制请改脚本，不要手改本文件）。</p>
 */
public class MoltenCoreArmorModel extends HumanoidModel<LivingEntity> implements ArmorDecoModel {{

{fields}

    private boolean showHead = true;
    private boolean showChest = true;
    private boolean showLegs = true;
    private boolean showBoots = true;

    /**
     * 按「部件所属槽位」开关，而不是按骨骼：腿骨上只有膝/胫属于护腿，
     * 靴子属于靴子槽；肩上和手臂上的件属于胸甲槽。
     */
    public void setSlot(ArmorItem.Type type) {{
        if (type == ArmorItem.Type.BODY) {{
            return;                       // 全套：全部都画
        }}
        this.showHead = type == ArmorItem.Type.HELMET;
        this.showChest = type == ArmorItem.Type.CHESTPLATE;
        this.showLegs = type == ArmorItem.Type.LEGGINGS;
        this.showBoots = type == ArmorItem.Type.BOOTS;
    }}

    public MoltenCoreArmorModel(ModelPart root) {{
        super(root);
{chr(10).join(ctor_lines)}
    }}

    public static LayerDefinition createArmorLayer(CubeDeformation deformation) {{
        MeshDefinition mesh = HumanoidModel.createMesh(deformation, 0.0F);
        PartDefinition root = mesh.getRoot();
{chr(10).join(layer)}
        return LayerDefinition.create(mesh, {TEX_W}, {TEX_H});
    }}

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {{
        // 只画装饰件；身体由 layer 贴图负责，戴头盔能看见手
{chr(10).join(render)}
    }}
}}
'''
    with open(JAVA, "w", encoding="utf-8", newline="\n") as f:
        f.write(java)
    print("wrote", JAVA, len(PARTS), "parts")


# ---------------------------------------------------------------- deco atlas
def emit_atlas():
    img = Image.new("RGBA", (128, 128), C["clear"])
    d = ImageDraw.Draw(img)

    def bevel_plate(box, base, lit, dark, hi):
        """Flat plate with a 1 px lit top / dark bottom and a few chunky rivets."""
        x0, y0, x1, y1 = box
        d.rectangle([x0, y0, x1, y1], fill=base)
        d.rectangle([x0, y0, x1, y0 + 2], fill=lit)
        d.rectangle([x0, y1 - 2, x1, y1], fill=dark)
        d.rectangle([x0, y0, x0 + 1, y1], fill=lit)
        d.rectangle([x1 - 1, y0, x1, y1], fill=dark)
        for rx, ry in ((x0 + 6, y0 + 10), (x1 - 10, y0 + 22),
                       (x0 + 14, y1 - 14), (x1 - 20, y1 - 26)):
            d.rectangle([rx, ry, rx + 3, ry + 3], fill=dark)
            d.point((rx, ry), fill=hi)

    # plate quadrant
    bevel_plate((0, 0, 63, 63), C["plate"], C["plate_l"], C["plate_d"], C["plate_h"])
    for (x0, y0, x1, y1) in ((8, 16, 30, 19), (36, 30, 58, 33), (12, 44, 28, 47)):
        d.rectangle([x0, y0, x1, y1], fill=C["plate_l"])
    for (x0, y0, x1, y1) in ((18, 26, 22, 40), (44, 20, 47, 28), (30, 50, 52, 53)):
        d.rectangle([x0, y0, x1, y1], fill=C["plate_d"])

    # trim quadrant
    bevel_plate((64, 0, 95, 63), C["brass"], C["brass_l"], C["brass_d"], C["brass_l"])
    d.rectangle([70, 20, 90, 23], fill=C["brass_l"])
    d.rectangle([74, 40, 86, 42], fill=C["brass_d"])

    # gem quadrant: a single hexagonal badge (brass rim + recess + gem facets),
    # so the emblem is one shape instead of a square plate with a hex stuck on it
    d.rectangle([96, 0, 127, 31], fill=C["plate_d"])
    cx, cy = 112, 16

    def hexagon(radius, rot=30):
        import math
        return [(cx + radius * math.cos(math.radians(60 * i + rot)),
                 cy + radius * math.sin(math.radians(60 * i + rot))) for i in range(6)]

    d.polygon(hexagon(15.5), fill=C["brass_d"])
    d.polygon(hexagon(14.0), fill=C["brass"])
    d.polygon(hexagon(12.0), fill=C["brass_l"])
    d.polygon(hexagon(11.0), fill=C["plate_d"])      # recess shadow
    d.polygon(hexagon(10.0), fill=C["glow"])
    rim = hexagon(10.0)
    d.polygon([rim[2], rim[3], rim[4], (cx, cy)], fill=C["glow_l"])   # lit facet
    d.polygon([rim[0], rim[4], rim[5], (cx, cy)], fill=C["glow_d"])   # shaded facet
    d.polygon(hexagon(3.4), fill=C["glow_h"])
    d.line([cx - 4, cy - 5, cx + 1, cy - 7], fill=C["glow_h"])

    # steel quadrant
    bevel_plate((96, 32, 127, 63), C["plate_l"], C["plate_h"], C["plate"], C["plate_h"])

    # glow quadrant
    d.rectangle([0, 64, 47, 127], fill=C["glow"])
    d.rectangle([8, 72, 40, 120], fill=C["glow_l"])
    d.rectangle([16, 84, 32, 108], fill=C["glow_h"])

    # chain quadrant
    d.rectangle([48, 64, 71, 127], fill=C["plate_d"])
    for y in range(64, 128, 8):
        d.rectangle([50, y + 1, 69, y + 3], fill=C["plate_l"])
        d.rectangle([57, y + 4, 62, y + 6], fill=C["plate_h"])

    # dark quadrant
    d.rectangle([72, 64, 95, 127], fill=C["dark"])
    d.rectangle([76, 70, 92, 74], fill=C["plate_d"])

    out = os.path.join(ASSETS, "textures", "models", "armor", "molten_core_armor.png")
    img.save(out, "PNG", optimize=True)
    print("wrote", out, os.path.getsize(out), "bytes")


# ---------------------------------------------------------------- layer textures
def emit_layers():
    """layer_1 = head/torso/arms/boots, layer_2 = leggings. Face and hands stay open."""
    l1 = Image.new("RGBA", (64, 32), C["clear"])
    d = ImageDraw.Draw(l1)

    # ---- head: painted around the face window, brass rim, cheek plates
    d.rectangle([0, 0, 31, 15], fill=C["plate"])
    d.rectangle([0, 0, 31, 2], fill=C["plate_l"])
    d.rectangle([0, 3, 31, 4], fill=C["plate"])
    d.rectangle([0, 13, 31, 15], fill=C["plate_d"])
    d.rectangle([0, 8, 7, 15], fill=C["plate_d"])       # left cheek
    d.rectangle([16, 8, 31, 15], fill=C["plate_d"])     # right cheek / back
    d.rectangle([7, 7, 16, 8], fill=C["brass_l"])       # brow band
    d.rectangle([7, 7, 16, 7], fill=C["brass"])
    d.rectangle([7, 15, 16, 15], fill=C["brass_d"])     # chin band
    d.rectangle([8, 9, 15, 14], fill=C["clear"])        # << face window
    d.rectangle([8, 8, 15, 8], fill=C["brass"])         # rim above the face
    d.rectangle([8, 15, 15, 15], fill=C["brass_d"])     # rim below the face

    # ---- torso: layered plates, brass belt, glowing core
    d.rectangle([16, 16, 39, 31], fill=C["plate"])
    d.rectangle([16, 16, 39, 18], fill=C["plate_l"])
    d.rectangle([16, 19, 39, 20], fill=C["plate_d"])
    d.rectangle([16, 20, 27, 26], fill=C["plate_l"])    # left chest plate
    d.rectangle([28, 20, 39, 26], fill=C["plate_d"])    # right chest plate
    d.rectangle([22, 21, 26, 25], fill=C["glow"])       # core glow patch
    d.rectangle([23, 22, 25, 24], fill=C["glow_h"])
    d.rectangle([16, 26, 39, 29], fill=C["brass"])
    d.rectangle([16, 26, 39, 27], fill=C["brass_l"])
    d.rectangle([26, 26, 30, 29], fill=C["brass_l"])    # buckle
    d.rectangle([30, 30, 39, 31], fill=C["plate_d"])

    # ---- arms: forearm plate, brass cuff line, hand window stays clear
    #      arm box at (40,16): right(40,20) front(44,20) left(48,20) back(52,20)
    #                          top(44,16) bottom(48,16)
    d.rectangle([40, 16, 55, 31], fill=C["plate"])
    d.rectangle([40, 16, 55, 17], fill=C["plate_l"])
    d.rectangle([40, 18, 55, 19], fill=C["plate_d"])
    d.rectangle([40, 24, 55, 27], fill=C["brass"])
    d.rectangle([40, 24, 55, 25], fill=C["brass_l"])
    # hand = bottom 3 units of the four side faces ...
    d.rectangle([40, 29, 55, 31], fill=C["clear"])
    # ... plus the END face of the arm (the 4x4 "bottom" panel) — this is what
    # showed as a black block under the hand
    d.rectangle([48, 16, 51, 19], fill=C["clear"])

    # ---- boots (leg region of layer_1): hot sole
    d.rectangle([0, 16, 15, 31], fill=C["plate"])
    d.rectangle([0, 16, 15, 17], fill=C["plate_l"])
    d.rectangle([0, 18, 15, 19], fill=C["plate_d"])
    d.rectangle([0, 26, 15, 28], fill=C["brass_d"])
    d.rectangle([0, 29, 15, 31], fill=C["glow"])
    d.rectangle([2, 30, 13, 31], fill=C["glow_h"])
    l1.save(os.path.join(ASSETS, "textures", "models", "armor", "molten_core_layer_1.png"), "PNG", optimize=True)

    # ---- layer_2: leggings
    l2 = Image.new("RGBA", (64, 32), C["clear"])
    d = ImageDraw.Draw(l2)
    d.rectangle([16, 16, 39, 31], fill=C["plate"])
    d.rectangle([16, 16, 39, 20], fill=C["brass"])
    d.rectangle([16, 16, 39, 18], fill=C["brass_l"])
    d.rectangle([16, 20, 39, 22], fill=C["plate_l"])
    d.rectangle([16, 23, 39, 31], fill=C["plate_d"])
    d.rectangle([0, 16, 15, 31], fill=C["plate"])
    d.rectangle([0, 16, 15, 17], fill=C["brass"])
    d.rectangle([0, 18, 15, 20], fill=C["plate_l"])     # thigh plate
    d.rectangle([0, 23, 15, 24], fill=C["plate_d"])     # knee seam
    d.rectangle([0, 25, 15, 27], fill=C["plate_l"])     # knee cap
    d.rectangle([0, 29, 15, 31], fill=C["plate_d"])
    if True:   # 3D-only set: the vanilla layer must stay invisible
        # diagnostic: hide the vanilla armour shell so only the 3D deco shows
        l1 = Image.new("RGBA", (64, 32), C["clear"])
        l2 = Image.new("RGBA", (64, 32), C["clear"])
    l1.save(os.path.join(ASSETS, "textures", "models", "armor", "molten_core_layer_1.png"), "PNG", optimize=True)
    l2.save(os.path.join(ASSETS, "textures", "models", "armor", "molten_core_layer_2.png"), "PNG", optimize=True)
    print("wrote layer_1 / layer_2" + ("  (DECO-ONLY DIAGNOSTIC)" if "--no-layer" in sys.argv else ""))


# ---------------------------------------------------------------- icons
def emit_icons():
    # icons are painted with shapes instead of glyphs to stay readable

    def icon(name, painter):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        dd = ImageDraw.Draw(img)
        painter(dd)
        img.save(os.path.join(ASSETS, "textures", "item", f"{name}.png"), "PNG", optimize=True)

    def outline(dd, box):
        dd.rectangle(box, outline=C["plate_d"])

    def helmet(dd):
        # five-spike crown (tapered), dark faceplate, hot eye slit, brass cuff
        for cx, top in ((4, 2), (7, 0), (10, 2)):
            dd.rectangle([cx, top, cx + 2, top + 3], fill=C["plate_l"])
            dd.rectangle([cx, top, cx, top + 3], fill=C["plate_h"])
            dd.rectangle([cx + 2, top, cx + 2, top + 3], fill=C["plate"])
        dd.rectangle([3, 3, 12, 5], fill=C["plate_l"])
        dd.rectangle([3, 5, 12, 12], fill=C["plate"])
        dd.rectangle([3, 5, 12, 6], fill=C["plate_h"])
        dd.rectangle([4, 8, 11, 9], fill=C["glow"])
        dd.rectangle([5, 8, 10, 8], fill=C["glow_h"])
        dd.rectangle([4, 10, 11, 11], fill=C["plate_d"])
        dd.rectangle([4, 12, 11, 13], fill=C["brass"])
        dd.rectangle([6, 12, 9, 13], fill=C["brass_l"])

    def chestplate(dd):
        dd.rectangle([3, 4, 12, 11], fill=C["plate"])
        dd.rectangle([1, 3, 3, 7], fill=C["plate_l"])
        dd.rectangle([12, 3, 14, 7], fill=C["plate_l"])
        dd.rectangle([1, 3, 1, 7], fill=C["plate_h"])
        dd.rectangle([14, 3, 14, 7], fill=C["plate_h"])
        dd.polygon([(7, 4), (10, 5), (10, 9), (7, 10), (5, 8), (5, 6)], fill=C["glow"])
        dd.polygon([(7, 5), (9, 6), (9, 8), (7, 9), (6, 7), (6, 7)], fill=C["glow_l"])
        dd.rectangle([7, 6, 8, 8], fill=C["glow_h"])
        dd.rectangle([2, 11, 13, 13], fill=C["brass"])
        dd.rectangle([7, 11, 8, 13], fill=C["brass_l"])
        dd.rectangle([2, 14, 4, 15], fill=C["plate_d"])
        dd.rectangle([11, 14, 13, 15], fill=C["plate_d"])

    def leggings(dd):
        dd.rectangle([2, 2, 13, 4], fill=C["brass"])
        dd.rectangle([3, 2, 6, 4], fill=C["brass_l"])
        dd.rectangle([3, 4, 6, 13], fill=C["plate"])
        dd.rectangle([9, 4, 12, 13], fill=C["plate"])
        dd.rectangle([3, 4, 6, 5], fill=C["plate_h"])
        dd.rectangle([9, 4, 12, 5], fill=C["plate_h"])
        dd.rectangle([2, 7, 7, 9], fill=C["plate_l"])
        dd.rectangle([8, 7, 13, 9], fill=C["plate_l"])
        dd.rectangle([3, 11, 6, 13], fill=C["plate_d"])
        dd.rectangle([9, 11, 12, 13], fill=C["plate_d"])

    def boots(dd):
        dd.rectangle([1, 4, 6, 12], fill=C["plate"])
        dd.rectangle([9, 4, 14, 12], fill=C["plate"])
        dd.rectangle([1, 4, 6, 5], fill=C["plate_h"])
        dd.rectangle([9, 4, 14, 5], fill=C["plate_h"])
        dd.rectangle([1, 9, 6, 10], fill=C["plate_l"])
        dd.rectangle([9, 9, 14, 10], fill=C["plate_l"])
        dd.rectangle([0, 12, 6, 14], fill=C["plate_d"])
        dd.rectangle([9, 12, 15, 14], fill=C["plate_d"])
        dd.rectangle([0, 14, 6, 15], fill=C["glow"])
        dd.rectangle([9, 14, 15, 15], fill=C["glow"])
        dd.rectangle([1, 14, 4, 14], fill=C["glow_h"])
        dd.rectangle([11, 14, 14, 14], fill=C["glow_h"])

    icon("molten_core_helmet", helmet)
    icon("molten_core_chestplate", chestplate)
    icon("molten_core_leggings", leggings)
    icon("molten_core_boots", boots)
    print("wrote 4 icons")


# ---------------------------------------------------------------- preview
def emit_preview():
    from PIL import Image as I
    S = 10
    w = int(30 * S)
    h = int(40 * S)
    img = I.new("RGBA", (w, h), (0x8A, 0x86, 0x80, 255))
    px = img.load()

    def put(sx, sy, c):
        if 0 <= sx < w and 0 <= sy < h:
            px[sx, sy] = c

    def proj(x, y):
        """world space: feet y=0, up is +y; screen y grows downward"""
        return ((x + 15) * S, (38 - y) * S)

    def draw_tex(src, tu0, tv0, tu1, tv1, mx0, my0, mx1, my1, flip=False):
        x0, y0 = proj(mx0, my1)
        x1, y1 = proj(mx1, my0)
        for sy in range(int(y0), int(y1)):
            fy = (sy - y0) / max(1e-6, (y1 - y0))
            for sx in range(int(x0), int(x1)):
                fx = (sx - x0) / max(1e-6, (x1 - x0))
                u = (1 - fx) if flip else fx
                c = src[int(tu0 + u * (tu1 - tu0 - 1)), int(tv0 + fy * (tv1 - tv0 - 1))]
                if c[3] > 8:
                    put(sx, sy, c)

    l1 = I.open(os.path.join(ASSETS, "textures", "models", "armor", "molten_core_layer_1.png")).convert("RGBA").load()
    l2 = I.open(os.path.join(ASSETS, "textures", "models", "armor", "molten_core_layer_2.png")).convert("RGBA").load()

    draw_tex(l1, 8, 8, 16, 16, -4, 24, 4, 32)            # head front
    draw_tex(l1, 20, 20, 28, 32, -4, 12, 4, 24)          # torso front
    draw_tex(l1, 44, 20, 48, 32, -8, 12, -4, 24)         # right arm
    draw_tex(l1, 44, 20, 48, 32, 4, 12, 8, 24, True)     # left arm (mirrored)
    draw_tex(l2, 4, 20, 8, 32, -3.9, 0, 0.1, 12)         # right leg
    draw_tex(l2, 4, 20, 8, 32, -0.1, 0, 3.9, 12, True)   # left leg

    atlas = I.open(os.path.join(ASSETS, "textures", "models", "armor", "molten_core_armor.png")).convert("RGBA").load()
    for name, bone, x0, y0, z0, x1, y1, z1, upx, vpx in sorted(PARTS, key=lambda p: -p[4]):
        bx, by, bz = BONE_PIVOT[bone]
        # box-UV front face sits at (u + depth, v + depth) and is w x h
        pw, ph, pd = x1 - x0, y1 - y0, z1 - z0
        tu0, tv0 = upx + pd, vpx + pd
        # parts are in HumanoidModel space (y DOWN from the neck at 24); the
        # preview draws y UP, so flip around the neck line and swap the range
        wy0 = 24.0 - (by + y1)
        wy1 = 24.0 - (by + y0)
        draw_tex(atlas, tu0, tv0, tu0 + pw, tv0 + ph,
                 x0 + bx, wy0, x1 + bx, wy1)

    # face placeholder drawn last so it shows through the open visor
    # (in game this is the player's own skin)
    fx0, fy0 = proj(-3.5, 30.1)
    fx1, fy1 = proj(3.5, 24.9)
    for yy in range(int(fy0), int(fy1)):
        for xx in range(int(fx0), int(fx1)):
            put(xx, yy, (0xC8, 0x9A, 0x78, 255))

    out = os.path.join(HERE, "build", f"preview-molten_core_armor{'-' + ONLY if ONLY else ''}.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out)
    print("wrote", out, img.size)

    if "--ascii" in sys.argv:
        ramp = ".,:;+=xX#@"
        step = 5
        for y in range(0, h, step * 2):
            row = ""
            for x in range(0, w, step):
                r, g, b, a = px[x, y]
                if (r, g, b) == (0x8A, 0x86, 0x80):
                    row += " "
                    continue
                lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
                row += ramp[min(9, max(1, int(lum / 0.45 * 9)))]
            print("   " + row)


ONLY = ""


def main():
    global ONLY, PARTS
    # 图集不再由本脚本绘制：直接用 Blockbench 项目里那张贴图（tools/bbmodel-export.py 导出），
    # 这样游戏内外是同一张图、同一套 UV。
    if "--only" in sys.argv:
        ONLY = sys.argv[sys.argv.index("--only") + 1]
        keep = [p for p in PARTS if slot_of(p[0]) == ONLY]
        emit_layers()
        emit_icons()
        saved, PARTS = PARTS, keep
        emit_preview()
        PARTS = saved
        return

    emit_java()
    emit_layers()
    emit_icons()
    emit_preview()


if __name__ == "__main__":
    main()
