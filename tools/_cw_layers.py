# Build 64x32 glyph grids for the cloud_walker armour layers.
HEAD = [
    "mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" + "." * 32,
    "mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" + "." * 32,
]
HEAD += ["ccccccccmmmmmmmmcccccccccccccccc" + "." * 32] * 6
HEAD += ["sscccccc........ccccccssccsccscc" + "." * 32] * 6
HEAD += ["ssssssss........ssssssssssssssss" + "." * 32] * 2

MID = ["....mmmmeeee...." + "...." + "llllllll" + "dddddddd" + "...." + "...." + "mmmm" + "eeee" + "." * 12] * 4

LEGS = [
    "cccccccccccccccc",
    "cscccscccscccscc",
    "cscccscccscccscc",
    "ssssssssssssssss",
    "llllllllllllllll",
    "pppppppppppppppp",
    "yyyyyyyyyyyyyyyy",
    "dddddddddddddddd",
    "yyyyyyyyyyyyyyyy",
    "qqqqqqqqqqqqqqqq",
    "dddddddddddddddd",
    "eeeeeeeeeeeeeeee",
]
BODY = [
    "ddpp" + "llllllll" + "ppdd" + "pppdpppp",
    "ddpp" + "lyllllly" + "ppdd" + "pppdpppp",
    "ddpp" + "llyllyll" + "ppdd" + "pppdpppp",
    "ddpp" + "pllyyllp" + "ppdd" + "pppdpppp",
    "ddpp" + "pplyylpp" + "ppdd" + "pppdpppp",
    "ddpp" + "pppyyppp" + "ppdd" + "pppdpppp",
    "ddpp" + "pppddppp" + "ppdd" + "pppdpppp",
    "ddpp" + "pppddppp" + "ppdd" + "pppdpppp",
    "ddpp" + "dddddddd" + "ppdd" + "pppdpppp",
    "ddpp" + "ppppdppp" + "ppdd" + "pppdpppp",
    "ddpp" + "yyyyyyyy" + "ppdd" + "yyyyyyyy",
    "ddpp" + "zzzzzzzz" + "ppdd" + "zzzzzzzz",
]
ARMS = [
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "mmmmmmmmmmmmmmmm",
    "ssssssssssssssss",
    "llllllllllllllll",
    "pppppppppppppppp",
    "yyyyyyyyyyyyyyyy",
    "pppppppppppppppp",
    "................",
    "................",
    "................",
    "................",
]
BOTTOM = [LEGS[i] + BODY[i] + ARMS[i] + "." * 8 for i in range(12)]
L1 = HEAD + MID + BOTTOM

L2 = ["." * 64] * 32
L2[0] = "mmmmmmmmmmmmmmmmmmmmmmmmmmmmmmmm" + "." * 32
L2[7] = "." * 7 + "y" * 10 + "." * 47
for r in range(8, 15):
    row = list(L2[r])
    row[7] = "y"
    row[16] = "y"
    L2[r] = "".join(row)
L2[15] = "." * 7 + "y" * 10 + "." * 47
for r in (20, 21):
    row = list(L2[r])
    for x in range(16, 20):
        row[x] = "y"
    for x in range(28, 32):
        row[x] = "y"
    L2[r] = "".join(row)
row = list(L2[30])
for x in list(range(20, 28)) + list(range(32, 40)):
    row[x] = "y"
L2[30] = "".join(row)
for r in (26, 28):
    row = list(L2[r])
    for x in list(range(0, 16)) + list(range(40, 56)):
        row[x] = "y"
    L2[r] = "".join(row)
L2[26] = L2[26][:40] + "y" * 16 + L2[26][56:]
L2[28] = L2[28][:40] + "y" * 16 + L2[28][56:]

assert all(len(r) == 64 for r in L1), [len(r) for r in L1]
assert all(len(r) == 64 for r in L2), [len(r) for r in L2]

import json
print(json.dumps(L1, ensure_ascii=False))
print("---")
print(json.dumps(L2, ensure_ascii=False))
