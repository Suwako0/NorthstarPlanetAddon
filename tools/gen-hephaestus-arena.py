#!/usr/bin/env python3
"""Generate the GameTest arena template used by the Hephaestus movement test.

The stock template (`dreamsleep_alter`) is 7x3x7 — too small for a 11-block-wide
colossus that needs to walk a few blocks.  This writes
`northstarplanetaddongametests.hephaestus_arena.nbt`: a 21x5x21 stone platform.

Usage: python tools/gen-hephaestus-arena.py
"""
import gzip
import os
import struct

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_DIR = os.path.join(HERE, "src", "main", "resources", "data", "northstar_planet_addon", "structure")
NAMES = ["hephaestus_arena.nbt", "northstarplanetaddongametests.hephaestus_arena.nbt"]
DATA_VERSION = 3955  # 1.21.1

SX, SY, SZ = 21, 16, 21
FLOOR_LAYERS = 2      # y = 0..1 are stone, above is air (16 层高：11 格宽的巨像有 9.9 格高)

TAG_END, TAG_BYTE, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 1, 3, 8, 9, 10


def s(value):
    raw = value.encode("utf-8")
    return struct.pack(">H", len(raw)) + raw


def named(tag_id, name, payload):
    return struct.pack(">b", tag_id) + s(name) + payload


def int_list(values):
    return struct.pack(">b", TAG_INT) + struct.pack(">i", len(values)) + b"".join(
        struct.pack(">i", v) for v in values)


def list_compound(payloads):
    return struct.pack(">b", TAG_COMPOUND) + struct.pack(">i", len(payloads)) + b"".join(payloads)


def build():
    # every compound payload must be terminated by TAG_End
    palette = [
        named(TAG_STRING, "Name", s("minecraft:stone")) + struct.pack(">b", TAG_END),
        named(TAG_STRING, "Name", s("minecraft:air")) + struct.pack(">b", TAG_END),
    ]
    blocks = []
    for x in range(SX):
        for y in range(SY):
            for z in range(SZ):
                state = 0 if y < FLOOR_LAYERS else 1
                body = named(TAG_LIST, "pos", int_list([x, y, z])) + named(
                    TAG_INT, "state", struct.pack(">i", state))
                blocks.append(body + struct.pack(">b", TAG_END))

    root = b"".join([
        named(TAG_LIST, "size", int_list([SX, SY, SZ])),
        named(TAG_LIST, "palette", list_compound(palette)),
        named(TAG_LIST, "blocks", list_compound(blocks)),
        named(TAG_LIST, "entities", list_compound([])),
        named(TAG_INT, "DataVersion", struct.pack(">i", DATA_VERSION)),
        struct.pack(">b", TAG_END),
    ])
    return struct.pack(">b", TAG_COMPOUND) + s("") + root


def main():
    payload = gzip.compress(build(), 9)
    for name in NAMES:
        path = os.path.join(OUT_DIR, name)
        with open(path, "wb") as fh:
            fh.write(payload)
        print(f"wrote {os.path.relpath(path, HERE)} ({len(payload)} bytes)  size={SX}x{SY}x{SZ}")


if __name__ == "__main__":
    main()
