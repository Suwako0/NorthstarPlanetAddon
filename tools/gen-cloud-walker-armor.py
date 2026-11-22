#!/usr/bin/env python3
"""Build the cloud_walker armour set from one data source.

Reference: docs/previews/cloud_walker-reference.jpg (ice-blue hooded scout with a
translucent membrane wing pair, tattered robe skirt and cyan trim).

Outputs
  src/main/java/.../venus/client/model/CloudWalkerArmorModel.java
  assets/northstar_planet_addon/textures/models/armor/cloud_walker_armor.png  (128x128)
  docs/models/cloud_walker_armor.bbmodel                                     (preview)
  docs/cloud_walker-armor.md                                                 (part/UV table)

Coordinates are vanilla HumanoidModel bone-local units: y is DOWN, the model
faces -Z, and the model's right side is -X.  Every part lists the armour slot it
belongs to, so `setSlot` shows only that piece.
"""
import base64
import io
import json
import math
import os
import uuid

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(HERE, "src", "main", "resources", "assets", "northstar_planet_addon")
JAVA = os.path.join(HERE, "src", "main", "java", "com", "suwako", "northstarplanetaddon",
                    "venus", "client", "model", "CloudWalkerArmorModel.java")
ATLAS = os.path.join(ASSETS, "textures", "models", "armor", "cloud_walker_armor.png")
BBMODEL = os.path.join(HERE, "docs", "models", "cloud_walker_armor.bbmodel")
DOC = os.path.join(HERE, "docs", "cloud_walker-armor.md")

AS = 256            # atlas size in pixels (1 texel == 1 model unit in box UV)
PAD = 2

# ------------------------------------------------------------------ palette
C = {
    "ink":        (0x2E, 0x5C, 0x6E, 255),
    "ink_dark":   (0x1B, 0x3B, 0x4A, 255),
    "plate_hi":   (0xF3, 0xFE, 0xFF, 255),
    "plate_l":    (0xDA, 0xF5, 0xFF, 255),
    "plate":      (0xC2, 0xE8, 0xF6, 255),
    "plate_2":    (0xAF, 0xDD, 0xEE, 255),
    "plate_d":    (0x8F, 0xC3, 0xD8, 255),
    "plate_dd":   (0x6E, 0xA3, 0xBA, 255),
    "cloth_hi":   (0xEF, 0xFC, 0xFF, 255),
    "cloth_l":    (0xD8, 0xF2, 0xFC, 255),
    "cloth":      (0xBF, 0xE6, 0xF5, 255),
    "cloth_d":    (0x9E, 0xCB, 0xDE, 255),
    "cloth_dd":   (0x7C, 0xAA, 0xBF, 255),
    "cyan":       (0x86, 0xEA, 0xF2, 255),
    "cyan_l":     (0xD9, 0xFD, 0xFF, 255),
    "cyan_d":     (0x40, 0xA8, 0xC0, 255),
    "membrane":   (0xDF, 0xF6, 0xFD, 255),
    "membrane_d": (0xBD, 0xE2, 0xF1, 255),
    "membrane_g": (0x8F, 0xF0, 0xF5, 255),
    "frame":      (0xD5, 0xF0, 0xFA, 255),
    "frame_l":    (0xF2, 0xFE, 0xFF, 255),
    "frame_d":    (0x8F, 0xC2, 0xD4, 255),
    "leather":    (0x7E, 0xA9, 0xBD, 255),
    "leather_d":  (0x5C, 0x86, 0x99, 255),
    "gem":        (0x9F, 0xF0, 0xFF, 255),
    "gem_l":      (0xE8, 0xFF, 0xFF, 255),
    "clear":      (0, 0, 0, 0),
}

# ------------------------------------------------------------------ parts
# (name, bone, slot, kind, x0,y0,z0, x1,y1,z1, paint flags)
# kind drives both the atlas painting and the part's visual language.
P = []


def snap(box):
    """Box UV needs whole-unit sizes; keep the low corner, round each extent up."""
    x0, y0, z0, x1, y1, z1 = box
    w = max(1, int(round(x1 - x0)))
    h = max(1, int(round(y1 - y0)))
    d = max(1, int(round(z1 - z0)))
    return (x0, y0, z0, x0 + w, y0 + h, z0 + d)


def part(name, bone, slot, kind, box, **kw):
    rec = {"name": name, "bone": bone, "slot": slot, "kind": kind, "box": snap(box)}
    rec.update(kw)
    P.append(rec)
    return rec


def mirror(name, rec, **over):
    """Mirror a part across x=0 (and swap side-sensitive paint)."""
    x0, y0, z0, x1, y1, z1 = rec["box"]
    out = {"name": name, "bone": rec["bone"], "slot": rec["slot"], "kind": rec["kind"],
           "box": (-x1, y0, z0, -x0, y1, z1)}
    for k, v in rec.items():
        if k not in out and k not in ("name", "box"):
            out[k] = v
    out.update(over)
    P.append(out)
    return out


# ---- head: hood shell around an open face, brim, cheek flaps
part("hood_top", "head", "head", "hood", (-5.2, -9.0, -5.4, 5.2, -6.9, 4.8))
part("hood_top2", "head", "head", "hood", (-4.4, -9.8, -4.6, 4.4, -8.6, 4.2))
part("hood_crest", "head", "head", "hood", (-1.5, -10.6, -2.4, 1.5, -9.2, 3.6))
part("hood_side_l", "head", "head", "hood", (3.8, -8.9, -5.2, 5.2, 0.9, 4.8))
part("hood_side_r", "head", "head", "hood", (-5.2, -8.9, -5.2, -3.8, 0.9, 4.8))
part("hood_back", "head", "head", "hood", (-5.2, -8.8, 3.6, 5.2, 1.3, 4.8))
part("hood_cheek_l", "head", "head", "hood", (3.0, -6.9, -5.2, 4.9, 0.9, -3.6))
part("hood_cheek_r", "head", "head", "hood", (-4.9, -6.9, -5.2, -3.0, 0.9, -3.6))
part("hood_chin", "head", "head", "hood", (-3.0, 0.2, -5.2, 3.0, 1.4, -3.6))
part("hood_flap_l", "head", "head", "hood", (4.2, -5.4, -3.6, 5.6, 2.4, 0.6))
part("hood_flap_r", "head", "head", "hood", (-5.6, -5.4, -3.6, -4.2, 2.4, 0.6))

# ---- body: cowl, layered pauldrons, chest chevron, sash, back plate, bow
part("cowl_front", "body", "chest", "cloth", (-4.8, -1.2, -3.2, 4.8, 2.4, -1.9))
part("cowl_shoulder_l", "body", "chest", "cloth", (2.6, -1.4, -2.8, 6.4, 2.2, 2.8))
part("cowl_shoulder_r", "body", "chest", "cloth", (-6.4, -1.4, -2.8, -2.6, 2.2, 2.8))
part("cowl_fold_l", "body", "chest", "cloth_l", (3.2, -0.2, -3.4, 5.8, 1.4, -2.8))
part("cowl_fold_r", "body", "chest", "cloth_l", (-5.8, -0.2, -3.4, -3.2, 1.4, -2.8))

part("pauldron_cap_l", "body", "chest", "plate", (4.2, -1.6, -2.8, 7.8, 1.8, 2.8), paint={"trim_bottom": True})
part("pauldron_cap_r", "body", "chest", "plate", (-7.8, -1.6, -2.8, -4.2, 1.8, 2.8), paint={"trim_bottom": True})
part("pauldron_lame_l", "body", "chest", "plate2", (4.4, 1.6, -2.6, 7.4, 3.6, 2.6), paint={"trim_bottom": True})
part("pauldron_lame_r", "body", "chest", "plate2", (-7.4, 1.6, -2.6, -4.4, 3.6, 2.6), paint={"trim_bottom": True})
part("pauldron_tip_l", "body", "chest", "plate", (4.4, 3.4, -2.4, 7.2, 5.0, 2.4), paint={"trim_bottom": True})
part("pauldron_tip_r", "body", "chest", "plate", (-7.2, 3.4, -2.4, -4.4, 5.0, 2.4), paint={"trim_bottom": True})
part("pauldron_crest_l", "body", "chest", "trim", (4.4, -2.4, -1.8, 7.2, -1.4, 1.8))
part("pauldron_crest_r", "body", "chest", "trim", (-7.2, -2.4, -1.8, -4.4, -1.4, 1.8))

part("chest_center", "body", "chest", "plate_l", (-0.4, 1.6, -2.9, 0.4, 8.4, -2.2))

part("sash", "body", "chest", "cloth", (-4.8, 8.8, -2.9, 4.8, 10.4, 2.9))
part("sash_v", "body", "chest", "trim", (-3.4, 9.8, -3.0, 3.4, 10.4, -2.1))
part("sash_knot", "body", "legs", "cloth", (3.2, 8.9, -3.4, 5.4, 11.2, -2.0))

part("backplate", "body", "chest", "plate2", (-4.2, 0.4, 2.0, 4.2, 8.6, 2.9))
part("back_spine", "body", "chest", "trim", (-0.5, 0.8, 2.5, 0.5, 8.9, 3.1))

part("bow_knot", "body", "chest", "trim", (-1.4, -0.8, 3.4, 1.4, 1.2, 5.0))
part("bow_loop_l", "body", "chest", "cloth", (-4.0, -1.4, 3.6, -1.2, 1.0, 4.8))
part("bow_loop_r", "body", "chest", "cloth", (1.2, -1.4, 3.6, 4.0, 1.0, 4.8))
part("ribbon_back_l", "body", "chest", "ribbon", (-2.5, 1.0, 4.0, -0.9, 10.6, 5.0))
part("ribbon_back_r", "body", "chest", "ribbon", (0.9, 1.0, 4.2, 2.5, 9.0, 5.2))

# ---- wings: hub + alpha-shaped membrane + stepped leading spar + claw
part("wing_hub_l", "body", "chest", "plate", (1.8, -1.2, 2.4, 4.2, 1.8, 4.4),
     paint={"gem": True})
part("wing_hub_r", "body", "chest", "plate", (-4.2, -1.2, 2.4, -1.8, 1.8, 4.4),
     paint={"gem": True})
part("wing_membrane_l", "body", "chest", "wing", (2.4, -5.4, 3.2, 20.0, 7.4, 4.4),
     paint={"mirror": True})
part("wing_membrane_r", "body", "chest", "wing", (-20.0, -5.4, 3.2, -2.4, 7.4, 4.4),
     paint={"mirror": False})
part("wing_spar1_l", "body", "chest", "frame", (2.6, -0.4, 2.7, 8.0, 0.6, 4.9))
part("wing_spar1_r", "body", "chest", "frame", (-8.0, -0.4, 2.7, -2.6, 0.6, 4.9))
part("wing_spar2_l", "body", "chest", "frame", (7.2, -2.2, 2.7, 13.0, -1.2, 4.9))
part("wing_spar2_r", "body", "chest", "frame", (-13.0, -2.2, 2.7, -7.2, -1.2, 4.9))
part("wing_spar3_l", "body", "chest", "frame", (12.2, -4.2, 2.7, 17.6, -3.2, 4.9))
part("wing_spar3_r", "body", "chest", "frame", (-17.6, -4.2, 2.7, -12.2, -3.2, 4.9))
part("wing_spar4_l", "body", "chest", "frame", (16.8, -3.2, 2.7, 20.4, -2.2, 4.9))
part("wing_spar4_r", "body", "chest", "frame", (-20.4, -3.2, 2.7, -16.8, -2.2, 4.9))
part("wing_claw_l", "body", "chest", "trim", (19.2, -0.6, 2.8, 21.0, 1.8, 4.2))
part("wing_claw_r", "body", "chest", "trim", (-21.0, -0.6, 2.8, -19.2, 1.8, 4.2))

# ---- skirt / tassets (waist-mounted, drawn for the legs slot)
part("skirt_front_l", "body", "legs", "skirt", (0.2, 9.2, -3.4, 5.6, 20.0, -2.3),
     paint={"hem": "front_l"})
part("skirt_front_r", "body", "legs", "skirt", (-5.6, 9.2, -3.4, -0.2, 20.0, -2.3),
     paint={"hem": "front_r"})
part("skirt_apron", "body", "legs", "skirt", (-3.6, 9.4, -3.9, 3.6, 17.2, -3.1),
     paint={"hem": "apron"})
part("skirt_side_l", "body", "legs", "skirt", (4.8, 9.2, -3.2, 6.4, 19.0, 3.2),
     paint={"hem": "side"})
part("skirt_side_r", "body", "legs", "skirt", (-6.4, 9.2, -3.2, -4.8, 19.0, 3.2),
     paint={"hem": "side"})
part("skirt_back", "body", "legs", "skirt", (-5.2, 9.2, 2.4, 5.2, 21.0, 3.4),
     paint={"hem": "back"})
part("skirt_flare_l", "body", "legs", "skirt", (5.0, 13.0, -3.0, 7.0, 19.0, -1.6),
     paint={"hem": "flare"})
part("skirt_flare_r", "body", "legs", "skirt", (-7.0, 13.0, -3.0, -5.0, 19.0, -1.6),
     paint={"hem": "flare"})
part("tasset_l", "body", "legs", "plate", (4.2, 9.0, -3.2, 6.0, 13.2, 3.2))
part("tasset_r", "body", "legs", "plate", (-6.0, 9.0, -3.2, -4.2, 13.2, 3.2))

# ---- legs: knee + shin plates, thigh strap, boots
part("knee_l", "left_leg", "legs", "plate", (-2.9, 3.6, -3.4, 2.9, 5.8, -1.9), paint={"trim_bottom": True})
part("knee_r", "right_leg", "legs", "plate", (-2.9, 3.6, -3.4, 2.9, 5.8, -1.9), paint={"trim_bottom": True})
part("shin_l", "left_leg", "legs", "plate2", (-2.7, 6.0, -3.3, 2.7, 9.4, -1.7), paint={"trim_bottom": True})
part("shin_r", "right_leg", "legs", "plate2", (-2.7, 6.0, -3.3, 2.7, 9.4, -1.7), paint={"trim_bottom": True})
part("thigh_strap_l", "left_leg", "legs", "leather", (-2.6, 1.0, -2.9, 2.6, 1.8, -1.6))
part("thigh_strap_r", "right_leg", "legs", "leather", (-2.6, 1.0, -2.9, 2.6, 1.8, -1.6))
part("boot_cuff_l", "left_leg", "boots", "plate", (-3.0, 9.4, -3.0, 3.0, 11.2, 3.0), paint={"trim_bottom": True})
part("boot_cuff_r", "right_leg", "boots", "plate", (-3.0, 9.4, -3.0, 3.0, 11.2, 3.0), paint={"trim_bottom": True})
part("boot_toe_l", "left_leg", "boots", "plate2", (-2.7, 10.6, -4.6, 2.7, 12.3, -1.6), paint={"trim_bottom": True})
part("boot_toe_r", "right_leg", "boots", "plate2", (-2.7, 10.6, -4.6, 2.7, 12.3, -1.6), paint={"trim_bottom": True})
part("boot_heel_l", "left_leg", "boots", "plate2", (-2.4, 10.8, 2.0, 2.4, 12.2, 3.6))
part("boot_heel_r", "right_leg", "boots", "plate2", (-2.4, 10.8, 2.0, 2.4, 12.2, 3.6))

# ---- arms: cloth sleeve, elbow fin, bracer, cyan cuff (hand stays clear)
part("sleeve_l", "left_arm", "chest", "cloth", (-2.9, -2.4, -2.9, 2.9, 1.6, 2.9))
part("sleeve_r", "right_arm", "chest", "cloth", (-2.9, -2.4, -2.9, 2.9, 1.6, 2.9))
part("elbow_fin_l", "left_arm", "chest", "plate", (1.8, 1.4, -1.0, 3.2, 3.8, 1.0))
part("elbow_fin_r", "right_arm", "chest", "plate", (-3.2, 1.4, -1.0, -1.8, 3.8, 1.0))
part("bracer_l", "left_arm", "chest", "plate2", (-2.9, 3.8, -2.9, 2.9, 8.2, 2.9))
part("bracer_r", "right_arm", "chest", "plate2", (-2.9, 3.8, -2.9, 2.9, 8.2, 2.9))
part("cuff_l", "left_arm", "chest", "trim", (-3.0, 7.4, -3.0, 3.0, 8.2, 3.0))
part("cuff_r", "right_arm", "chest", "trim", (-3.0, 7.4, -3.0, 3.0, 8.2, 3.0))

# ------------------------------------------------------------------ UV pack
for rec in P:
    x0, y0, z0, x1, y1, z1 = rec["box"]
    rec["size"] = (x1 - x0, y1 - y0, z1 - z0)
    w, h, d = rec["size"]
    rec["footprint"] = (int(math.ceil(2 * (w + d))), int(math.ceil(h + d)))

order = sorted(range(len(P)), key=lambda i: P[i]["footprint"][0] * P[i]["footprint"][1],
               reverse=True)
cx = cy = row_h = 0
for i in order:
    rec = P[i]
    fw, fh = rec["footprint"]
    if cx + fw > AS:
        cx = 0
        cy += row_h + PAD
        row_h = 0
    rec["uv"] = (cx, cy)
    cx += fw + PAD
    row_h = max(row_h, fh)
used_h = cy + row_h
if used_h > AS:
    raise SystemExit(f"atlas overflow: needs {used_h}px of {AS}px")


def face_rects(rec):
    """Box-UV regions (texture pixels) for every face of a cube."""
    u, v = rec["uv"]
    w, h, d = rec["size"]
    d = int(math.ceil(d))
    w = int(math.ceil(w))
    h = int(math.ceil(h))
    return {
        "east":  (u, v + d, u + d, v + d + h),
        "north": (u + d, v + d, u + d + w, v + d + h),
        "west":  (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "south": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
        "up":    (u + d, v, u + d + w, v + d),
        "down":  (u + d + w, v, u + 2 * d + w, v + d),
    }


# ------------------------------------------------------------------ atlas
img = Image.new("RGBA", (AS, AS), C["clear"])
dr = ImageDraw.Draw(img)


def shade(col, k):
    r, g, b = col[:3]
    return (max(0, min(255, int(r * k))), max(0, min(255, int(g * k))),
            max(0, min(255, int(b * k))), 255)


def fill_rect(rect, col):
    x0, y0, x1, y1 = rect
    if x1 > x0 and y1 > y0:
        dr.rectangle([x0, y0, x1 - 1, y1 - 1], fill=col)


def outline_rect(rect, col):
    x0, y0, x1, y1 = rect
    if x1 <= x0 or y1 <= y0:
        return
    dr.rectangle([x0, y0, x1 - 1, y1 - 1], outline=col)


def plate(ctx, base, hi, mid, sh, edge, rivets=True):
    """Bevel + top light row + bottom shade + chunky rivets."""
    x0, y0, x1, y1 = ctx
    w, h = x1 - x0, y1 - y0
    if w <= 0 or h <= 0:
        return
    fill_rect(ctx, base)
    if h >= 3:
        dr.rectangle([x0, y0, x1 - 1, y0 + 0], fill=hi)
        dr.rectangle([x0, y1 - 1, x1 - 1, y1 - 1], fill=sh)
    if w >= 3:
        dr.rectangle([x0, y0, x0 + 0, y1 - 1], fill=hi)
        dr.rectangle([x1 - 1, y0, x1 - 1, y1 - 1], fill=shade(base, 0.85))
    outline_rect(ctx, shade(base, 0.62))
    if rivets and w >= 6 and h >= 6:
        for rx, ry in ((x0 + 2, y0 + 2), (x1 - 3, y0 + 2),
                       (x0 + 2, y1 - 3), (x1 - 3, y1 - 3)):
            dr.point((rx, ry), fill=hi)
            dr.point((rx + 1, ry + 1), fill=shade(base, 0.7))


def cloth(ctx, base, hi, mid, sh, folds=2):
    x0, y0, x1, y1 = ctx
    w, h = x1 - x0, y1 - y0
    if w <= 0 or h <= 0:
        return
    fill_rect(ctx, base)
    if h > 2:
        dr.rectangle([x0, y0, x1 - 1, y0], fill=hi)
        dr.rectangle([x0, y1 - 1, x1 - 1, y1 - 1], fill=sh)
    step = max(3, w // (folds + 1))
    x = x0 + step
    for _ in range(folds):
        if x < x1 - 1:
            dr.line([x, y0 + 1, x, y1 - 2], fill=mid)
            if x + 1 < x1:
                dr.point((x + 1, y0 + 1), fill=hi)
        x += step
    outline_rect(ctx, shade(base, 0.62))


def wing_art(ctx, mirror):
    """Membrane shape, painted ribs and a glowing trailing edge."""
    x0, y0, x1, y1 = ctx
    w, h = x1 - x0, y1 - y0
    if w < 8 or h < 6:
        fill_rect(ctx, C["membrane"])
        return

    def X(v):
        return x0 + (w - 1 - v) if mirror else x0 + v

    top = [(0, 5), (3, 3), (6, 1), (9, 0), (12, 1), (15, 3), (17, 5)]
    bottom = [(16, 7), (14, 6), (13, 9), (11, 7), (10, 10), (8, 8),
              (6, 11), (4, 9), (2, 11), (0, 9)]
    poly = [(X(px), y0 + py) for px, py in top + bottom]
    dr.polygon(poly, fill=C["membrane"])

    # lower membrane is a touch cooler, like the reference's translucency
    for yy in range(y0 + 6, y1):
        for xx in range(x0, x1):
            if img.getpixel((xx, yy))[:3] == C["membrane"][:3]:
                img.putpixel((xx, yy), C["membrane_d"])

    # bright leading edge
    dr.line([(X(px), y0 + py) for px, py in top], fill=C["frame_l"])
    # ribs from the root towards the finger tips
    dr.line([(X(0), y0 + 9), (X(9), y0 + 0)], fill=C["frame_l"])
    dr.line([(X(9), y0 + 0), (X(16), y0 + 7)], fill=C["frame_l"])
    dr.line([(X(3), y0 + 5), (X(13), y0 + 9)], fill=C["frame"])
    # glowing trailing edge
    for i in range(len(bottom) - 1):
        dr.line([(X(bottom[i][0]), y0 + bottom[i][1]),
                 (X(bottom[i + 1][0]), y0 + bottom[i + 1][1])], fill=C["membrane_g"])
    # root shading
    dr.polygon([(X(0), y0 + 9), (X(2), y0 + 4), (X(0), y0 + 4)], fill=C["membrane_d"])


def torn_panel(ctx, base=C["cloth"], edge=C["cyan"], depth=3, folds=3, trim_right=False):
    """Cloth panel whose silhouette is cut by a jagged hem in the alpha channel."""
    x0, y0, x1, y1 = ctx
    w, h = x1 - x0, y1 - y0
    if w <= 0 or h <= 0:
        return
    pattern = [depth, depth - 1, depth + 1, depth - 1, depth - 2, depth]
    for xx in range(w):
        cut = max(1, min(h - 2, pattern[xx % len(pattern)]))
        dr.rectangle([x0 + xx, y0, x0 + xx, y1 - 1 - cut], fill=base)
    # top light row and vertical folds
    dr.rectangle([x0, y0, x1 - 1, y0], fill=C["cloth_hi"])
    step = max(3, w // (folds + 1))
    for i in range(1, folds + 1):
        xx = x0 + i * step
        if x0 < xx < x1 - 1:
            dr.line([xx, y0 + 1, xx, y1 - 4], fill=C["cloth_d"])
            if xx + 1 < x1 - 1:
                dr.point((xx + 1, y0 + 1), fill=C["cloth_hi"])
    # side edges + cyan slit trim
    for yy in range(h):
        if img.getpixel((x0, y0 + yy))[3] > 0:
            dr.point((x0, y0 + yy), fill=C["cloth_dd"])
        if img.getpixel((x1 - 1, y0 + yy))[3] > 0:
            dr.point((x1 - 1, y0 + yy), fill=C["cloth_dd"])
    if trim_right:
        for yy in range(h):
            if img.getpixel((x1 - 1, y0 + yy))[3] > 0:
                dr.point((x1 - 1, y0 + yy), fill=edge)
    else:
        for yy in range(h):
            if img.getpixel((x0, y0 + yy))[3] > 0:
                dr.point((x0, y0 + yy), fill=edge)
    # darker tatter tips
    for xx in range(w):
        for yy in range(y1 - 1, max(y0, y1 - depth - 3), -1):
            if img.getpixel((x0 + xx, yy))[3] > 0:
                img.putpixel((x0 + xx, yy), C["cloth_d"])
                break


def paint_part(rec):
    faces = face_rects(rec)
    kind = rec["kind"]
    paint = rec.get("paint", {})
    n = faces["north"]
    s = faces["south"]
    side = {
        "plate": (C["plate_2"], C["plate_l"], C["plate_d"]),
        "plate2": (C["plate"], C["plate_l"], C["plate_d"]),
        "plate_l": (C["plate"], C["plate_hi"], C["plate_d"]),
        "trim": (C["cyan"], C["cyan_l"], C["cyan_d"]),
        "frame": (C["frame"], C["frame_l"], C["frame_d"]),
        "cloth": (C["cloth"], C["cloth_l"], C["cloth_d"]),
        "cloth_l": (C["cloth_l"], C["cloth_hi"], C["cloth_d"]),
        "hood": (C["cloth"], C["cloth_hi"], C["cloth_d"]),
        "hood_l": (C["cloth_l"], C["cloth_hi"], C["cloth_d"]),
        "skirt": (C["cloth"], C["cloth_l"], C["cloth_d"]),
        "ribbon": (C["cloth_l"], C["cloth_hi"], C["cloth"]),
        "leather": (C["leather"], C["leather"], C["leather_d"]),
        "wing": (C["frame"], C["frame_l"], C["frame_d"]),
    }[kind]
    alpha_kind = kind in ("wing", "skirt", "ribbon")
    fill_rect(faces["east"], side[0])
    fill_rect(faces["west"], side[0])
    fill_rect(faces["up"], side[1])
    fill_rect(faces["down"], side[2])
    if not alpha_kind:
        fill_rect(n, C["plate"])
        fill_rect(s, C["plate"])

    if kind == "wing":
        wing_art(n, paint.get("mirror", False))
        wing_art(s, not paint.get("mirror", False))
        for key in ("east", "west", "up", "down"):
            fill_rect(faces[key], C["frame_d"])
        return
    if kind in ("hood", "hood_l"):
        for ctx in (n, s):
            cloth(ctx, C["cloth"], C["cloth_hi"], C["cloth_d"], C["cloth_dd"], folds=2)
            x0, y0, x1, y1 = ctx
            fill_rect((x0, y1 - 2, x1, y1), C["cloth_dd"])
        if kind == "hood_l":
            for ctx in (n, s):
                fill_rect((ctx[0], ctx[1], ctx[2], ctx[1] + 1), C["plate_hi"])
        return
    if kind in ("cloth", "cloth_l"):
        for ctx in (n, s):
            cloth(ctx, C["cloth"], C["cloth_hi"], C["cloth_d"], C["cloth_dd"], folds=2)
        if kind == "cloth_l":
            fill_rect((n[0], n[1], n[2], n[1] + 1), C["cloth_hi"])
            fill_rect((s[0], s[1], s[2], s[1] + 1), C["cloth_hi"])
        return
    if kind == "ribbon":
        for ctx in (n, s):
            torn_panel(ctx, depth=2, folds=1, trim_right=False,
                       edge=C["cloth_hi"])
        return
    if kind == "skirt":
        hem = paint.get("hem")
        for ctx in (n, s):
            torn_panel(ctx, depth=3, folds=3, trim_right=(hem == "front_l"),
                       edge=C["cyan"] if hem in ("front_l", "front_r") else C["cloth_hi"])
        return
    if kind == "trim":
        for ctx in (n, s):
            x0, y0, x1, y1 = ctx
            fill_rect(ctx, C["cyan"])
            dr.rectangle([x0, y0, x1 - 1, y0], fill=C["cyan_l"])
            dr.rectangle([x0, y1 - 1, x1 - 1, y1 - 1], fill=C["cyan_d"])
            if y1 - y0 > 3:
                yy = (y0 + y1) // 2
                dr.line([x0 + 1, yy, x1 - 2, yy], fill=C["cyan_l"])
        return
    if kind == "frame":
        for ctx in (n, s):
            plate(ctx, C["frame"], C["frame_l"], C["plate_l"], C["frame_d"], C["ink"], rivets=False)
            dr.line([ctx[0], (ctx[1] + ctx[3]) // 2, ctx[2] - 1, (ctx[1] + ctx[3]) // 2],
                    fill=C["cyan"])
        return
    if kind == "leather":
        for ctx in (n, s):
            x0, y0, x1, y1 = ctx
            fill_rect(ctx, C["leather"])
            dr.rectangle([x0, y0, x1 - 1, y0], fill=C["leather_d"])
            step = max(3, (x1 - x0) // 3)
            for xx in range(x0 + 1, x1 - 1, step):
                dr.line([xx, y0 + 1, xx, y1 - 2], fill=C["ink"])
        return
    if kind == "plate_l":
        for ctx in (n, s):
            plate(ctx, C["plate_l"], C["plate_hi"], C["plate"], C["plate_d"], C["ink"])
            if paint.get("trim_bottom"):
                dr.rectangle([ctx[0], ctx[3] - 1, ctx[2] - 1, ctx[3] - 1], fill=C["cyan"])
        return
    if kind == "plate2":
        for ctx in (n, s):
            plate(ctx, C["plate_2"], C["plate_l"], C["plate"], C["plate_d"], C["ink"])
            if paint.get("trim_bottom"):
                dr.rectangle([ctx[0], ctx[3] - 1, ctx[2] - 1, ctx[3] - 1], fill=C["cyan"])
        return
    # default plate
    for ctx in (n, s):
        plate(ctx, C["plate"], C["plate_l"], C["plate_2"], C["plate_d"], C["ink"])
        if paint.get("trim_bottom"):
            dr.rectangle([ctx[0], ctx[3] - 1, ctx[2] - 1, ctx[3] - 1], fill=C["cyan"])
        if paint.get("gem"):
            cx = (ctx[0] + ctx[2]) // 2
            cy = (ctx[1] + ctx[3]) // 2
            dr.rectangle([cx - 1, cy - 1, cx + 1, cy + 1], fill=C["cyan_d"])
            dr.rectangle([cx - 1, cy - 1, cx, cy], fill=C["cyan_l"])
            dr.point((cx, cy), fill=C["gem_l"])


for rec in P:
    paint_part(rec)

os.makedirs(os.path.dirname(ATLAS), exist_ok=True)
img.save(ATLAS, "PNG", optimize=True)
print("wrote", ATLAS, img.size, os.path.getsize(ATLAS), "bytes", "used height", used_h)

# ------------------------------------------------------------------ java
BONE_BASE = {"head": "head", "body": "body", "left_arm": "leftArm", "right_arm": "rightArm",
             "left_leg": "leftLeg", "right_leg": "rightLeg"}
SLOT_FLAG = {"head": "showHead", "chest": "showChest", "legs": "showLegs", "boots": "showBoots"}


def jname(name):
    out = []
    for i, chunk in enumerate(name.split("_")):
        out.append(chunk if i == 0 else chunk.capitalize())
    return "".join(out)


def num(v):
    if abs(v - round(v)) < 1e-6:
        return str(int(round(v)))
    return f"{round(v, 3)}F"


fields = "\n".join(f"    private final ModelPart {jname(p['name'])};" for p in P)
ctor = []
for bone in BONE_BASE:
    ctor.append(f'        ModelPart {jname(bone)}Part = root.getChild("{bone}");')
for p in P:
    ctor.append(f'        this.{jname(p["name"])} = {jname(p["bone"])}Part.getChild("{p["name"]}");')

layer = [f'        PartDefinition {bone} = root.getChild("{bone}");' for bone in BONE_BASE]
for p in P:
    x0, y0, z0, x1, y1, z1 = p["box"]
    w, h, d = x1 - x0, y1 - y0, z1 - z0
    u, v = p["uv"]
    layer.append(
        f'        {p["bone"]}.addOrReplaceChild("{p["name"]}",\n'
        f'                CubeListBuilder.create().texOffs({u}, {v})'
        f'.addBox({num(x0)}, {num(y0)}, {num(z0)}, {num(w)}, {num(h)}, {num(d)}),\n'
        f'                PartPose.offset(0, 0, 0));')

render = []
for bone in BONE_BASE:
    mine = [p for p in P if p["bone"] == bone]
    if not mine:
        continue
    render.append("        poseStack.pushPose();")
    render.append(f"        this.{jname(bone)}.translateAndRotate(poseStack);")
    for p in mine:
        render.append(f"        if ({SLOT_FLAG[p['slot']]}) {{")
        render.append(f"            this.{jname(p['name'])}.render(poseStack, buffer, packedLight, packedOverlay, color);")
        render.append("        }")
    render.append("        poseStack.popPose();")

java = f'''package com.suwako.northstarplanetaddon.venus.client.model;

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
 * CloudWalkerArmorModel — 云行者套装的 3D 装饰件（{len(P)} 部件）。
 *
 * <p>按参考图 docs/previews/cloud_walker-reference.jpg 重做：兜帽 + 颈巾 + 分层肩甲 +
 * 胸口人字甲 + 腰封、背后蝶结与长飘带、膜翼（翼膜形状画在贴图 alpha 里）、
 * 破损长袍下摆、护膝 / 胫甲 / 战靴。</p>
 *
 * <p>贴图：assets/northstar_planet_addon/textures/models/armor/cloud_walker_armor.png（{AS}x{AS}）。</p>
 * <p>只画装饰件，基础甲面交给 cloud_walker_layer_1/2.png；脸和手由 layer 贴图开口保证可见。</p>
 *
 * <p>生成脚本：tools/gen-cloud-walker-armor.py（改形制请改脚本，不要手改本文件）。</p>
 */
public class CloudWalkerArmorModel extends HumanoidModel<LivingEntity> implements ArmorDecoModel {{

{fields}

    private boolean showHead = true;
    private boolean showChest = true;
    private boolean showLegs = true;
    private boolean showBoots = true;

    /** 按部位所属槽位显示：裙摆挂在 body 骨上但只在穿护腿时画。 */
    public void setSlot(ArmorItem.Type type) {{
        if (type == ArmorItem.Type.BODY) {{
            this.showHead = true;
            this.showChest = true;
            this.showLegs = true;
            this.showBoots = true;
            return;
        }}
        this.showHead = type == ArmorItem.Type.HELMET;
        this.showChest = type == ArmorItem.Type.CHESTPLATE;
        this.showLegs = type == ArmorItem.Type.LEGGINGS;
        this.showBoots = type == ArmorItem.Type.BOOTS;
    }}

    public CloudWalkerArmorModel(ModelPart root) {{
        super(root);
{chr(10).join(ctor)}
    }}

    public static LayerDefinition createArmorLayer(CubeDeformation deformation) {{
        MeshDefinition mesh = HumanoidModel.createMesh(deformation, 0.0F);
        PartDefinition root = mesh.getRoot();
{chr(10).join(layer)}
        return LayerDefinition.create(mesh, {AS}, {AS});
    }}

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {{
        // 只画装饰件；基础身体由 layer 贴图负责，脸和手不会被甲片盖住
{chr(10).join(render)}
    }}
}}
'''
with open(JAVA, "w", encoding="utf-8", newline="\n") as f:
    f.write(java)
print("wrote", JAVA, len(P), "parts")

# ------------------------------------------------------------------ bbmodel preview
# Blockbench: y up, faces -Z, model right = +X.  Java model-local y is down and
# the model right is -X, so x is negated for the preview only.
PIVOT_Y = {"head": 24, "body": 24, "right_arm": 22, "left_arm": 22,
           "right_leg": 12, "left_leg": 12}
PIVOT_X = {"head": 0, "body": 0, "right_arm": -5, "left_arm": 5,
           "right_leg": -1.9, "left_leg": 1.9}

groups = []
outliner = []
for bone in BONE_BASE:
    gu = str(uuid.uuid4())
    groups.append({"name": bone, "uuid": gu, "export": True, "locked": False, "scope": 0,
                   "selected": False, "visibility": True, "origin": [0, PIVOT_Y[bone], 0],
                   "rotation": [0, 0, 0], "color": 0, "children": [],
                   "reset": False, "shade": True, "mirror_uv": False, "autouv": 0,
                   "isOpen": False, "primary_selected": False})
    outliner.append({"name": bone, "uuid": gu, "export": True, "locked": False,
                     "scope": 0, "selected": False, "visibility": True,
                     "origin": [0, PIVOT_Y[bone], 0], "rotation": [0, 0, 0],
                     "color": 0, "children": []})
gidx = {g["name"]: g for g in groups}
oidx = {g["name"]: g for g in outliner}

elements = []
for p in P:
    x0, y0, z0, x1, y1, z1 = p["box"]
    px = PIVOT_X[p["bone"]]
    py = PIVOT_Y[p["bone"]]
    wx0, wx1 = px + x0, px + x1
    wy0, wy1 = py - y1, py - y0          # java y down -> world y up
    wz0, wz1 = z0, z1
    bx0, bx1 = -wx1, -wx0                # java right -X -> blockbench right +X
    f = face_rects(p)
    faces = {}
    for name, rect in f.items():
        faces[name] = {"uv": list(rect), "texture": 0}
    eu = str(uuid.uuid4())
    el = {"name": p["name"], "box_uv": True, "render_order": "default", "locked": False,
          "export": True, "scope": 0, "allow_mirror_modeling": True,
          "from": [round(bx0, 3), round(wy0, 3), round(wz0, 3)],
          "to": [round(bx1, 3), round(wy1, 3), round(wz1, 3)],
          "autouv": 0, "color": 0, "origin": [0, 0, 0],
          "uv_offset": list(p["uv"]), "faces": faces, "type": "cube", "uuid": eu}
    elements.append(el)
    gidx[p["bone"]]["children"].append(eu)
    oidx[p["bone"]]["children"].append(eu)

buf = io.BytesIO()
img.save(buf, "PNG")
data_url = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("ascii")
bb = {"meta": {"format_version": "5.0", "model_format": "free", "box_uv": True},
      "name": "cloud_walker_armor", "model_identifier": "cloud_walker_armor",
      "visible_box": [0, 0, 0],
      "resolution": {"width": AS, "height": AS},
      "elements": elements, "groups": groups, "outliner": outliner,
      "textures": [{"name": "cloud_walker_armor.png", "id": "0", "uuid": str(uuid.uuid4()),
                    "uv_width": AS, "uv_height": AS, "source": data_url,
                    "particle": False, "render_mode": "default", "render_sides": "auto",
                    "width": AS, "height": AS}],
      "animations": [], "texture_groups": [], "reference_images": [],
      "unhandled_root_fields": {}}
os.makedirs(os.path.dirname(BBMODEL), exist_ok=True)
with open(BBMODEL, "w", encoding="utf-8", newline="\n") as f:
    json.dump(bb, f, ensure_ascii=False)
print("wrote", BBMODEL)

# ------------------------------------------------------------------ docs
rows = "\n".join(
    f"| {p['name']} | {p['bone']} | {p['slot']} | {p['kind']} | "
    f"{p['box'][0]:g},{p['box'][1]:g},{p['box'][2]:g},{p['box'][3] - p['box'][0]:g},"
    f"{p['box'][4] - p['box'][1]:g},{p['box'][5] - p['box'][2]:g} | "
    f"{p['uv'][0]},{p['uv'][1]} |" for p in P)
doc = f"""# cloud_walker 云行者（参考图重做：{len(P)} 部件）

参考图：`docs/previews/cloud_walker-reference.jpg`（冰蓝兜帽斥候 + 半透明膜翼 + 破损长袍）。

生成命令：`python tools/gen-cloud-walker-armor.py`
（同一个数据源产出 Java 模型、128x128 图集、bbmodel 预览与本文档）

坐标是原版 `HumanoidModel` 骨内单位：y 向下、模型正面朝 -Z、模型右侧为 -X。

| 部件 | 父骨 | 槽位 | 视觉类型 | box (x,y,z,w,h,d) | UV |
|---|---|---|---|---|---|
{rows}

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
"""
with open(DOC, "w", encoding="utf-8", newline="\n") as f:
    f.write(doc)
print("wrote", DOC)
