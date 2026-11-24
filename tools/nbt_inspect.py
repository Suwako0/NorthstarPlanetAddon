"""Minimal NBT reader: prints size, palette and block counts of a structure NBT."""
import gzip
import struct
import sys
from collections import Counter


class Reader:
    def __init__(self, data):
        self.d = data
        self.i = 0

    def u1(self):
        v = self.d[self.i]
        self.i += 1
        return v

    def u2(self):
        v = struct.unpack_from(">H", self.d, self.i)[0]
        self.i += 2
        return v

    def i4(self):
        v = struct.unpack_from(">i", self.d, self.i)[0]
        self.i += 4
        return v

    def i8(self):
        v = struct.unpack_from(">q", self.d, self.i)[0]
        self.i += 8
        return v

    def f4(self):
        v = struct.unpack_from(">f", self.d, self.i)[0]
        self.i += 4
        return v

    def f8(self):
        v = struct.unpack_from(">d", self.d, self.i)[0]
        self.i += 8
        return v

    def string(self):
        n = self.u2()
        s = self.d[self.i:self.i + n].decode("utf-8", "replace")
        self.i += n
        return s


def read_payload(r, tag):
    if tag == 0:
        return None
    if tag == 1:
        v = r.u1()
        return v - 256 if v > 127 else v
    if tag == 2:
        v = r.u2()
        return v - 65536 if v > 32767 else v
    if tag == 3:
        return r.i4()
    if tag == 4:
        return r.i8()
    if tag == 5:
        return r.f4()
    if tag == 6:
        return r.f8()
    if tag == 7:
        n = r.i4()
        v = r.d[r.i:r.i + n]
        r.i += n
        return list(v)
    if tag == 8:
        return r.string()
    if tag == 9:
        itype = r.u1()
        n = r.i4()
        return [read_payload(r, itype) for _ in range(n)]
    if tag == 10:
        out = {}
        while True:
            t = r.u1()
            if t == 0:
                return out
            name = r.string()
            out[name] = read_payload(r, t)
    if tag == 11:
        n = r.i4()
        v = struct.unpack_from(">%di" % n, r.d, r.i)
        r.i += 4 * n
        return list(v)
    if tag == 12:
        n = r.i4()
        v = struct.unpack_from(">%dq" % n, r.d, r.i)
        r.i += 8 * n
        return list(v)
    raise ValueError("bad tag %d" % tag)


def load(path):
    with open(path, "rb") as f:
        raw = f.read()
    if raw[:2] == b"\x1f\x8b":
        raw = gzip.decompress(raw)
    r = Reader(raw)
    t = r.u1()
    assert t == 10, t
    r.string()
    return read_payload(r, 10)


def main(path):
    root = load(path)
    print("== %s" % path)
    for k, v in root.items():
        if k == "palette" and isinstance(v, list):
            print("palette: %d entries" % len(v))
            for e in v:
                print("   ", e.get("Name"), {kk: vv for kk, vv in e.items() if kk != "Name"} or "")
        elif k == "blocks" and isinstance(v, list):
            print("blocks: %d" % len(v))
            counts = Counter()
            states = {}
            for b in v:
                st = b.get("state", 0)
                counts[st] += 1
                for e in ("nbt",):
                    if e in b:
                        states[st] = b[e]
            pal = root.get("palette", [])
            print("-- counts by palette entry (top 40):")
            for st, c in counts.most_common(40):
                name = pal[st].get("Name") if st < len(pal) else "?"
                props = {kk: vv for kk, vv in (pal[st] if st < len(pal) else {}).items() if kk != "Name"}
                print("   %6d  %s %s" % (c, name, props or ""))
            extra = [b for b in v if "nbt" in b]
            if extra:
                print("-- blocks with NBT: %d, sample:" % len(extra))
                for b in extra[:8]:
                    print("   ", b.get("pos"), b.get("nbt"))
        elif k in ("size", "entities", "DataVersion", "author"):
            if k == "entities":
                print("entities: %d" % len(v))
                for e in v[:10]:
                    print("   ", e.get("pos"), (e.get("nbt") or {}).get("id"))
            else:
                print("%s: %s" % (k, v))
        else:
            print("%s: %r" % (k, v if not isinstance(v, list) else "list[%d]" % len(v)))


if __name__ == "__main__":
    for p in sys.argv[1:]:
        main(p)
        print()
