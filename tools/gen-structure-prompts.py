"""Generate docs/ai-prompts/structure-prompts.{json,txt} from docs/STRUCTURE-AI-PROMPTS.md.

The markdown is the single source of truth: this keeps the machine-readable pack and
the plain-text collection byte-identical to what a human copies out of the doc.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MD = ROOT / "docs" / "STRUCTURE-AI-PROMPTS.md"
OUT_JSON = ROOT / "docs" / "ai-prompts" / "structure-prompts.json"
OUT_TXT = ROOT / "docs" / "ai-prompts" / "structure-prompts.txt"

SECTION_RE = re.compile(r"^### (.+)$", re.M)


def field(block, label):
    """Return the first ```text fenced block that follows a **label** line."""
    m = re.search(r"\*\*%s\*\*[^\n]*\n+```text\n(.*?)\n```" % re.escape(label), block, re.S)
    return " ".join(m.group(1).split()) if m else ""


def first_fence(block):
    """The unlabelled first ```text fence of a section (house style: the long prompt)."""
    m = re.search(r"```text\n(.*?)\n```", block, re.S)
    return " ".join(m.group(1).split()) if m else ""


def inline(block, label):
    m = re.search(r"\*\*%s\*\*\s*([^\n]+)" % re.escape(label), block)
    if not m:
        return ""
    return m.group(1).strip().lstrip("：:").strip()


def split_meta(value):
    """'`id` ｜ **平台** 金星 ｜ ...' -> dict of the pipe-separated key/values."""
    out = {}
    for part in value.split("｜"):
        part = part.strip()
        m = re.match(r"\*\*(.+?)\*\*\s*(.+)", part)
        if m:
            out[m.group(1)] = m.group(2).strip()
        elif part.startswith("`"):
            out["id"] = part.strip("`")
    return out


def main():
    text = MD.read_text(encoding="utf-8")
    starts = [m.start() for m in SECTION_RE.finditer(text)]
    if not starts:
        sys.exit("no '### ' sections found")
    heads = [m.group(1).strip() for m in SECTION_RE.finditer(text)]
    blocks = [text[s:starts[i + 1] if i + 1 < len(starts) else len(text)] for i, s in enumerate(starts)]

    items = []
    for head, block in zip(heads, blocks):
        if "**id**" not in block:
            continue  # explanatory sub-section, not a prompt entry
        title = re.sub(r"^[0-9A-Z]+\s*·\s*", "", head)  # drop the leading ordinal
        name = re.sub(r"\s*·\s*`[^`]*`", "", title)   # drop the trailing `id`
        name = name.split("（")[0].strip()             # drop any parenthetical note
        meta = split_meta(inline(block, "id"))
        item = {
            "no": head.split("·")[0].strip(),
            "name": name,
            "heading": head,
            "id": meta.get("id", ""),
            "planet": meta.get("平台", ""),
            "kind": meta.get("类型", ""),
            "size_blocks": meta.get("体量", ""),
            "aspect": meta.get("画幅", ""),
            "prompt": first_fence(block),
            "prompt_short": field(block, "短版"),
            "prompt_zh": field(block, "中文版"),
            "negative": field(block, "负面词"),
            "params": inline(block, "参数"),
            "palette": inline(block, "调色板"),
        }
        missing = [k for k in ("id", "prompt", "prompt_short", "prompt_zh", "negative") if not item[k]]
        if missing:
            sys.exit("section %r missing: %s" % (head, ", ".join(missing)))
        items.append(item)

    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "note": "本模组特殊建筑 / 祭坛的 AI 绘图指令。生成自 docs/STRUCTURE-AI-PROMPTS.md，勿手改。"
                "配套需求案 docs/STRUCTURE-ART-BRIEF.md。",
        "style_anchor": "stylized Minecraft voxel architecture concept art, blocky one-metre cube construction, "
                        "painted 2D illustration with 3-value cel shading, matte materials, crisp per-block surface "
                        "detail, clean voxel edges, strong readable silhouette, production concept art",
        "color_rules": {
            "cloud_stone": "#586070 / #788090 / #8898a0",
            "tungsten": "#303830 / #485850 / #889890",
            "sulfur_crystal": "#685010 / #a88018 / #f0e078 / #f8e8a8",
            "sulfuric_acid": "#88e048 / #b0e048",
            "cut_copper": "#c16d4f",
            "weathered_cut_copper": "#6a9a88",
            "ares_altar": "#181010 / #201818 / #783818 / #c07838",
            "dreamsleep_altar": "#282040 / #383050 / rune #88e0f8",
        },
        "view_suffixes": {
            "cutaway": "cutaway version: the near-side wall and roof removed, interior floor and chests visible, "
                       "same materials and lighting",
            "plan": "orthographic top-down plan view, grid aligned, no perspective, block layout clearly readable",
            "elevation": "orthographic front elevation, flat and straight on, no perspective, full height fills the frame",
            "silhouette": "pure black silhouette of the same structure on a flat white background, no internal detail",
            "storm": "the same structure under a dark storm sky with distant lightning, cold rim light, same materials",
            "night": "the same structure at night, only its own crystal and lantern light, deep blue ambient, same materials",
            "wide_shot": "wide shot from 60 blocks away, the structure small in frame, silhouette dominant, "
                         "cloud sea filling the lower half",
        },
        "items": items,
    }
    OUT_JSON.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    lines = ["# 特殊建筑提示词纯文本合集（生成自 STRUCTURE-AI-PROMPTS.md，勿手改）", ""]
    for it in items:
        lines += [
            "=" * 78,
            "[%s] %s  (%s / %s / %s / %s)" % (it["no"], it["name"], it["id"], it["planet"], it["kind"], it["size_blocks"]),
            "=" * 78,
            "",
            "-- 长版 --",
            it["prompt"],
            "",
            "-- 短版 --",
            it["prompt_short"],
            "",
            "-- 中文版 --",
            it["prompt_zh"],
            "",
            "-- 负面词 --",
            it["negative"],
            "",
            "-- 参数 --",
            it["params"],
            "",
            "-- 调色板 --",
            it["palette"],
            "",
        ]
    OUT_TXT.write_text("\n".join(lines), encoding="utf-8")

    print("items: %d" % len(items))
    for it in items:
        print("  %-4s %-24s %-22s long=%4d short=%4d zh=%4d neg=%4d"
              % (it["no"], it["id"], it["kind"], len(it["prompt"]), len(it["prompt_short"]),
                 len(it["prompt_zh"]), len(it["negative"])))
    print("json -> %s" % OUT_JSON)
    print("txt  -> %s" % OUT_TXT)


if __name__ == "__main__":
    main()
