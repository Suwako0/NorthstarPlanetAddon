"""把 Blockbench 工程（.bbmodel）转成 GeckoLib 资源：
   1) animations/entity/nephele.animation.json（Bedrock 1.8 动画格式）
   2) textures/entity/*.png（从工程内嵌的 data URL 提取）
   3) 修正 geo 的 identifier 为 geometry.nephele
"""
import base64
import json
import os

ROOT = r"D:\game\mod\NorthstarPlanetAddon"
BB = os.path.join(ROOT, r"build\scratch\models\nephele.bbmodel")
ASSETS = os.path.join(ROOT, r"src\main\resources\assets\northstar_planet_addon")
GEO = os.path.join(ASSETS, r"geo\entity\nephele.geo.json")
ANIM_OUT = os.path.join(ASSETS, r"animations\entity\nephele.animation.json")
TEX_DIR = os.path.join(ASSETS, r"textures\entity")

with open(BB, encoding="utf-8") as fh:
    project = json.load(fh)

# ---------------- 动画 ----------------
# GeckoLib 4.x 的关键帧格式是 {"vector":[x,y,z], "easing":"..."}。
# 不要写 Bedrock 的 {"post":[...], "lerp_mode":"catmullrom"} —— loader 读不到 vector，
# 每个关键帧都会取默认值 0，动画「在播但完全不动」。工程里 hephaestus/ares 两个能正常播的
# 文件都是 vector 格式，照它们写。
def keyframe_value(kf):
    points = kf.get("data_points") or []
    if points and isinstance(points[0], dict):
        point = points[0]
        vector = [float(point.get("x", 0)), float(point.get("y", 0)), float(point.get("z", 0))]
    else:
        flat = kf.get("datapoints") or [0, 0, 0]
        vector = [float(v) for v in flat[:3]]
    easing = "linear" if kf.get("interpolation") == "linear" else "easeInOutSine"
    return {"vector": [round(v, 3) for v in vector], "easing": easing}


group_names = {g["uuid"]: g["name"] for g in project.get("groups", [])}
animations = {}
for anim in project.get("animations", []):
    bones = {}
    for auuid, animator in anim.get("animators", {}).items():
        bone = group_names.get(auuid) or animator.get("name")
        channels = {}
        for kf in animator.get("keyframes", []):
            channel = kf.get("channel", "rotation")
            key = "%g" % round(float(kf["time"]), 3)
            channels.setdefault(channel, {})[key] = keyframe_value(kf)
        if channels:
            bones.setdefault(bone, {}).update(
                {ch: dict(sorted(v.items(), key=lambda kv: float(kv[0]))) for ch, v in channels.items()}
            )
    animations[anim["name"]] = {
        "loop": anim.get("loop") == "loop",
        "animation_length": anim.get("length", 1),
        "bones": bones,
    }

os.makedirs(os.path.dirname(ANIM_OUT), exist_ok=True)
with open(ANIM_OUT, "w", encoding="utf-8") as fh:
    json.dump({"format_version": "1.8.0", "animations": animations}, fh, indent=2)
print("animations ->", ANIM_OUT, [f"{k}({len(v['bones'])} bones)" for k, v in animations.items()])

# ---------------- 贴图 ----------------
os.makedirs(TEX_DIR, exist_ok=True)
written = []
for tex in project.get("textures", []):
    name = tex.get("name", "")
    src = tex.get("source") or ""
    if not name.startswith("nephele") or not src.startswith("data:image"):
        continue
    payload = src.split(",", 1)[1]
    path = os.path.join(TEX_DIR, name + ".png")
    with open(path, "wb") as fh:
        fh.write(base64.b64decode(payload))
    written.append((name, os.path.getsize(path)))
print("textures ->", written)

# ---------------- 修正 geo identifier ----------------
with open(GEO, encoding="utf-8") as fh:
    geo = json.load(fh)
desc = geo["minecraft:geometry"][0]["description"]
desc["identifier"] = "geometry.nephele"
with open(GEO, "w", encoding="utf-8") as fh:
    json.dump(geo, fh, indent=2)
print("geo identifier ->", desc["identifier"], "bones:", len(geo["minecraft:geometry"][0]["bones"]))
