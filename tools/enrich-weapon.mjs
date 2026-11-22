#!/usr/bin/env node
/**
 * enrich-weapon.mjs — 给生成器产出的 3D 武器模型补装饰件，把 element 数抬到目标下限。
 *
 * weapon3d.mjs / gen-tool3d.mjs 的部件数是固定的（35~47 之间）；这套附加装饰不新增图集区域，
 * 全部复用已生成的 accent / gem / pommel / flat 区域，所以贴图不用改。
 *
 * 位置全部锚在模型包围盒的垂直比例上（头部 80~95%、中部 35~60%、配重 5%），x 以 8 为中心，
 * 与两个生成器的几何约定一致（x 中心 8、厚度 z 6.4~9.6）。
 *
 * 用法：node enrich-weapon.mjs <model.json> --min 45
 */
import { readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'

const file = resolve(process.argv[2] ?? '')
if (!process.argv[2]) {
  console.error('用法: node enrich-weapon.mjs <model.json> --min 45')
  process.exit(1)
}
const minIdx = process.argv.indexOf('--min')
const MIN = minIdx >= 0 ? Number(process.argv[minIdx + 1]) : 45

/* 与生成器一致的图集区域 → uv（0..16 空间，texture_size 改变时自动跟随） */
const regions = {
  guard:  { x: 16, y: 0,  w: 16, h: 8 },
  wrap:   { x: 16, y: 8,  w: 16, h: 16 },
  pommel: { x: 16, y: 24, w: 16, h: 8 },
  gem:    { x: 16, y: 32, w: 8,  h: 8 },
  accent: { x: 32, y: 0,  w: 32, h: 16 },
  flat:   { x: 32, y: 16, w: 16, h: 16 },
}
const uvOf = (r) => [r.x / 4, r.y / 4, (r.x + r.w) / 4, (r.y + r.h) / 4]
const FACES = ['down', 'up', 'north', 'south', 'west', 'east']

const model = JSON.parse(readFileSync(file, 'utf8'))
const els = model.elements
if (!Array.isArray(els) || els.length === 0) {
  console.error('没有 elements，不是 3D 模型：' + file)
  process.exit(1)
}

const ys = els.flatMap((e) => [e.from[1], e.to[1]])
const minY = Math.min(...ys)
const maxY = Math.max(...ys)
const h = maxY - minY
const at = (frac) => Math.round((minY + h * frac) * 100) / 100

function box(name, half, y, thickness, region) {
  const uv = uvOf(regions[region])
  const faces = {}
  for (const f of FACES) faces[f] = { uv, texture: '#all' }
  els.push({
    name,
    from: [Math.round((8 - half) * 100) / 100, y, Math.round((8 - thickness) * 100) / 100],
    to: [Math.round((8 + half) * 100) / 100, Math.round((y + 0.45) * 100) / 100, Math.round((8 + thickness) * 100) / 100],
    faces,
  })
}

/* 装饰阶梯：从头部往下、再配重，逐条追加，够数就停 */
const LADDER = [
  () => box('enrich_head_ring_low', 1.35, at(0.955), 1.35, 'accent'),
  () => box('enrich_head_ring_high', 1.1, at(0.905), 1.1, 'accent'),
  () => box('enrich_head_gem_left', 0.5, at(0.88), 0.62, 'gem'),
  () => box('enrich_head_gem_right', 0.5, at(0.86), 0.62, 'gem'),
  () => box('enrich_collar_ring', 1.0, at(0.74), 1.0, 'guard'),
  () => box('enrich_stud_1', 0.28, at(0.66), 1.12, 'pommel'),
  () => box('enrich_stud_2', 0.28, at(0.58), 1.12, 'pommel'),
  () => box('enrich_stud_3', 0.28, at(0.50), 1.12, 'pommel'),
  () => box('enrich_stud_4', 0.28, at(0.42), 1.12, 'pommel'),
  () => box('enrich_slot_1', 0.62, at(0.38), 0.30, 'flat'),
  () => box('enrich_slot_2', 0.62, at(0.33), 0.30, 'flat'),
  () => box('enrich_butt_ring', 1.05, at(0.06), 1.05, 'accent'),
  () => box('enrich_butt_gem', 0.45, at(0.02), 0.55, 'gem'),
  () => box('enrich_wrap_band', 0.95, at(0.24), 0.95, 'accent'),
]

let i = 0
while (els.length < MIN && i < LADDER.length) {
  LADDER[i]()
  i += 1
}
if (els.length < MIN) {
  console.error(`补完阶梯后仍只有 ${els.length} 个 element（目标 ${MIN}），需要再加装饰模板`)
  process.exit(2)
}

writeFileSync(file, JSON.stringify(model, null, 2) + '\n')
const ys2 = els.flatMap((e) => [e.from[1], e.to[1]])
console.log(JSON.stringify({
  file: file.split(/[\\/]/).pop(),
  elements: els.length,
  added: i,
  lengthUnits: Math.round((Math.max(...ys2) - Math.min(...ys2)) * 10) / 10,
}, null, 2))
