#!/usr/bin/env node
/**
 * gen-tool3d.mjs — 给 weapon3d.mjs 没覆盖的「器械类」装备生成 3D 物品模型。
 *
 * weapon3d.mjs 的 kind 只有剑类 / 长柄类 / 锤 / 斧 / 法杖 / 三叉戟，
 * 十字弩和钻机套不进任何一种形制，硬套会做出「带弩臂的剑」。
 * 这里沿用它的全部产出约定，只补两种新形制：
 *
 *   kind = "crossbow"  弩身 + 弩臂 + 弦 + 弩矢 + 扳机 + 握把 + 背带
 *   kind = "drill"     枪体 + 三道箍 + 卡盘 + 四段钻头 + 螺旋排屑槽 + 压力表 + 手枪握把
 *
 * 产出：models/item/<name>.json（elements + big_handheld + gui）、
 *       textures/item/<name>.png（64x64 图集，UV 区域与 weapon3d 完全一致）、
 *       docs/<name>-atlas.md。
 *
 * 用法：node gen-tool3d.mjs <spec.json> --out <projectRoot>
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { join, resolve, dirname } from 'node:path'
import { createRequire } from 'node:module'
import * as path from 'node:path'
import os from 'node:os'
import { fileURLToPath } from 'node:url'

const HERE = path.dirname(fileURLToPath(import.meta.url))

function loadSharp() {
  const home = process.env.DSH_HOME || path.join(os.homedir(), '.dsh')
  const candidates = [
    process.env.DSH_SHARP_PKG,
    path.join(home, 'plugins', 'dsh-pixel-art', 'node_modules', 'sharp', 'package.json'),
    path.join(home, 'profiles', 'node_modules', 'sharp', 'package.json'),
    path.join(home, 'profiles', 'web', 'package.json'),
  ].filter(Boolean)
  for (const c of candidates) {
    try { return createRequire(c)('sharp') } catch {}
  }
  try { return createRequire(path.join(home, 'profiles', 'web', 'package.json'))('sharp') } catch {}
  try { return createRequire(path.resolve(HERE, '..', 'package.json'))('sharp') } catch {}
  return null
}

const specPath = process.argv[2]
if (!specPath) { console.error('用法: node gen-tool3d.mjs <spec.json> --out <projectRoot>'); process.exit(1) }
const outIdx = process.argv.indexOf('--out')
const root = resolve(outIdx >= 0 ? process.argv[outIdx + 1] : '.')
const spec = JSON.parse(readFileSync(specPath, 'utf8').replace(/^\uFEFF/, ''))

const modid = spec.modid
const name = spec.name
const kind = spec.kind ?? 'crossbow'
if (!modid || !name) { console.error('spec.modid / spec.name 必填'); process.exit(1) }

const AS = 64
const CX = 8

/* 图集区域：与 weapon3d.mjs 逐像素一致，方便两张图集共用一套画法 */
const regions = {
  blade:  { x: 0,  y: 0,  w: 16, h: 48 },
  guard:  { x: 16, y: 0,  w: 16, h: 8 },
  wrap:   { x: 16, y: 8,  w: 16, h: 16 },
  pommel: { x: 16, y: 24, w: 16, h: 8 },
  gem:    { x: 16, y: 32, w: 8,  h: 8 },
  tassel: { x: 24, y: 32, w: 8,  h: 16 },
  accent: { x: 32, y: 0,  w: 32, h: 16 },
  flat:   { x: 32, y: 16, w: 16, h: 16 },
}
const uvOf = (r) => [r.x / 4, r.y / 4, (r.x + r.w) / 4, (r.y + r.h) / 4]
const FACES = ['down', 'up', 'north', 'south', 'west', 'east']

const els = []
function box(elName, from, to, region, opts = {}) {
  const uv = uvOf(regions[region])
  const faces = {}
  for (const f of FACES) faces[f] = { uv, texture: '#all' }
  const el = { name: elName, from, to, faces }
  if (opts.shade === false) el.shade = false
  els.push(el)
  return el
}

const MIN_PARTS = Math.ceil(spec.minParts ?? 34.1)

/* ------------------------------ 十字弩 ------------------------------ */
function crossbow() {
  const zf = 6.5 // 正面 z
  const zb = 9.5 // 背面 z
  /* 弩身：托肩 → 三段枪身 */
  box('stock_butt', [7, -2, zf + 0.3], [9, 2, zb - 0.3], 'guard')
  box('butt_plate', [6.6, -2.6, zf], [9.4, -2.0, zb], 'accent')
  box('stock_lower', [7, 2, zf + 0.4], [9, 8, zb - 0.4], 'blade')
  box('stock_mid', [7, 8, zf + 0.4], [9, 14, zb - 0.4], 'blade')
  box('stock_upper', [7, 14, zf + 0.4], [9, 20, zb - 0.4], 'blade')
  box('cheek_rest_left', [6.3, 6.5, zf + 0.6], [7, 12.5, zb - 0.6], 'flat')
  box('cheek_rest_right', [9, 6.5, zf + 0.6], [9.7, 12.5, zb - 0.6], 'flat')
  /* 弩臂座 */
  box('riser', [6.4, 19.4, zf + 0.2], [9.6, 21.4, zb - 0.2], 'guard')
  box('riser_plate', [7.1, 20.0, zf - 0.4], [8.9, 21.0, zf + 0.2], 'accent')
  /* 双弩臂：各两段 + 臂尖 */
  box('limb_left_inner', [1.2, 20.2, zf + 0.8], [6.4, 21.2, zb - 0.8], 'blade')
  box('limb_right_inner', [9.6, 20.2, zf + 0.8], [14.8, 21.2, zb - 0.8], 'blade')
  box('limb_left_tip', [-2.2, 20.6, zf + 1.0], [1.2, 21.4, zb - 1.0], 'blade')
  box('limb_right_tip', [14.8, 20.6, zf + 1.0], [18.2, 21.4, zb - 1.0], 'blade')
  box('limb_cap_left', [-2.7, 20.4, zf + 0.8], [-2.2, 21.6, zb - 0.8], 'accent', { shade: false })
  box('limb_cap_right', [18.2, 20.4, zf + 0.8], [18.7, 21.6, zb - 0.8], 'accent', { shade: false })
  box('limb_band_left', [4.2, 20.0, zf + 0.5], [4.7, 21.4, zb - 0.5], 'accent', { shade: false })
  box('limb_band_right', [11.3, 20.0, zf + 0.5], [11.8, 21.4, zb - 0.5], 'accent', { shade: false })
  /* 弓弦：两段（弩身左右各一） */
  box('string_left', [-2.0, 19.2, 7.7], [CX, 19.5, 8.3], 'flat', { shade: false })
  box('string_right', [CX, 19.2, 7.7], [18.0, 19.5, 8.3], 'flat', { shade: false })
  /* 弩矢：三段杆 + 箭头 + 两片尾羽 */
  box('bolt_shaft_low', [7.6, 21.6, 7.6], [8.4, 24.4, 8.4], 'blade')
  box('bolt_shaft_mid', [7.7, 24.4, 7.7], [8.3, 27.2, 8.3], 'blade')
  box('bolt_shaft_high', [7.75, 27.2, 7.75], [8.25, 29.8, 8.25], 'blade')
  box('bolt_tip', [7.85, 29.8, 7.85], [8.15, 31.4, 8.15], 'accent', { shade: false })
  box('fletching_left', [6.9, 22.4, 7.75], [7.6, 25.4, 8.25], 'tassel', { shade: false })
  box('fletching_right', [8.4, 22.4, 7.75], [9.1, 25.4, 8.25], 'tassel', { shade: false })
  /* 扳机 + 护圈 */
  box('trigger_guard', [7.3, 4.4, zf - 0.6], [8.7, 6.4, zf], 'guard')
  box('trigger', [7.7, 4.8, zf - 0.9], [8.3, 6.2, zf - 0.5], 'accent', { shade: false })
  /* 手枪握把 + 三圈缠绳 */
  box('grip_core', [7.15, 0.2, 5.2], [8.85, 5.6, 7.1], 'wrap')
  box('grip_wrap_1', [7.05, 1.2, 5.1], [8.95, 1.6, 7.2], 'accent', { shade: false })
  box('grip_wrap_2', [7.05, 2.6, 5.1], [8.95, 3.0, 7.2], 'accent', { shade: false })
  box('grip_wrap_3', [7.05, 4.0, 5.1], [8.95, 4.4, 7.2], 'accent', { shade: false })
  /* 瞄具 */
  box('sight_base', [7.4, 15.6, zf - 0.5], [8.6, 16.6, zf], 'guard')
  box('sight_gem', [7.65, 15.8, zf - 0.9], [8.35, 16.4, zf - 0.5], 'gem', { shade: false })
  /* 弩身装饰：两颗宝石 + 四铆钉 + 两道箍 */
  box('core_gem', [7.35, 11.6, zf - 0.4], [8.65, 12.8, zf + 0.1], 'gem', { shade: false })
  box('core_gem_rim', [7.2, 11.4, zf - 0.4], [8.8, 13.0, zf - 0.05], 'guard')
  box('rivet_1', [6.85, 16.6, zf + 0.1], [7.15, 16.9, zf + 0.5], 'accent', { shade: false })
  box('rivet_2', [8.85, 16.6, zf + 0.1], [9.15, 16.9, zf + 0.5], 'accent', { shade: false })
  box('rivet_3', [6.85, 9.4, zf + 0.1], [7.15, 9.7, zf + 0.5], 'accent', { shade: false })
  box('rivet_4', [8.85, 9.4, zf + 0.1], [9.15, 9.7, zf + 0.5], 'accent', { shade: false })
  box('band_lower', [6.9, 7.6, zf + 0.3], [9.1, 8.2, zb - 0.3], 'accent', { shade: false })
  box('band_upper', [6.9, 17.4, zf + 0.3], [9.1, 18.0, zb - 0.3], 'accent', { shade: false })
  /* 背带：两股 + 结 */
  box('sling_left', [6.5, 2.6, zb - 0.2], [6.9, 8.6, zb + 0.2], 'tassel', { shade: false })
  box('sling_right', [9.1, 2.6, zb - 0.2], [9.5, 8.6, zb + 0.2], 'tassel', { shade: false })
  box('sling_knot', [7.3, 1.6, zb - 0.1], [8.7, 2.6, zb + 0.3], 'tassel', { shade: false })
}

/* ------------------------------- 钻机 ------------------------------- */
function drill() {
  box('butt_cap', [6.8, -1.0, 6.8], [9.2, 0.4, 9.2], 'guard')
  box('butt_ring', [6.6, 0.4, 6.6], [9.4, 0.8, 9.4], 'accent', { shade: false })
  /* 手枪握把（向后下方） */
  box('grip_core', [7.1, 0.2, 5.0], [8.9, 6.2, 7.0], 'wrap')
  box('grip_wrap_1', [7.0, 1.0, 4.9], [9.0, 1.4, 7.1], 'accent', { shade: false })
  box('grip_wrap_2', [7.0, 2.6, 4.9], [9.0, 3.0, 7.1], 'accent', { shade: false })
  box('grip_wrap_3', [7.0, 4.2, 4.9], [9.0, 4.6, 7.1], 'accent', { shade: false })
  box('trigger_guard', [7.2, 3.0, 4.4], [8.8, 3.6, 5.0], 'guard')
  box('trigger', [7.5, 3.6, 4.5], [8.5, 4.8, 4.9], 'accent', { shade: false })
  /* 枪体三段 + 三道箍 */
  box('motor_lower', [6.4, 6.2, 6.4], [9.6, 10.4, 9.6], 'guard')
  box('motor_mid', [6.4, 10.4, 6.4], [9.6, 14.4, 9.6], 'guard')
  box('motor_upper', [6.4, 14.4, 6.4], [9.6, 18.2, 9.6], 'guard')
  box('motor_band_1', [6.15, 6.4, 6.15], [9.85, 6.95, 9.85], 'accent', { shade: false })
  box('motor_band_2', [6.15, 12.6, 6.15], [9.85, 13.15, 9.85], 'accent', { shade: false })
  box('motor_band_3', [6.15, 17.7, 6.15], [9.85, 18.25, 9.85], 'accent', { shade: false })
  /* 压力表（左侧）*/
  box('gauge_base', [6.05, 10.6, 6.6], [6.45, 13.0, 7.6], 'guard')
  box('gauge_face', [5.85, 10.9, 6.7], [6.1, 12.7, 7.5], 'gem', { shade: false })
  box('hose_port', [7.0, 8.2, 9.6], [9.0, 10.2, 10.2], 'guard')
  box('hose_ring', [7.2, 8.6, 10.2], [8.8, 9.8, 10.6], 'accent', { shade: false })
  /* 卡盘 + 四颗螺栓 */
  box('chuck', [6.0, 18.2, 6.0], [10.0, 19.8, 10.0], 'guard')
  box('chuck_ring', [5.75, 19.4, 5.75], [10.25, 19.95, 10.25], 'accent', { shade: false })
  box('chuck_bolt_1', [6.1, 18.5, 5.85], [6.5, 18.9, 6.05], 'accent', { shade: false })
  box('chuck_bolt_2', [9.5, 18.5, 5.85], [9.9, 18.9, 6.05], 'accent', { shade: false })
  box('chuck_bolt_3', [6.1, 18.5, 9.95], [6.5, 18.9, 10.15], 'accent', { shade: false })
  box('chuck_bolt_4', [9.5, 18.5, 9.95], [9.9, 18.9, 10.15], 'accent', { shade: false })
  /* 钻头四段 + 尖 + 两道环 */
  box('bit_1', [6.6, 19.8, 6.6], [9.4, 23.0, 9.4], 'blade')
  box('bit_2', [6.9, 23.0, 6.9], [9.1, 26.0, 9.1], 'blade')
  box('bit_3', [7.1, 26.0, 7.1], [8.9, 29.0, 8.9], 'blade')
  box('bit_4', [7.3, 29.0, 7.3], [8.7, 31.4, 8.7], 'blade')
  box('bit_tip', [7.75, 31.4, 7.75], [8.25, 33.0, 8.25], 'accent', { shade: false })
  box('bit_ring_1', [6.5, 22.6, 6.5], [9.5, 23.1, 9.5], 'accent', { shade: false })
  box('bit_ring_2', [6.8, 25.7, 6.8], [9.2, 26.2, 9.2], 'accent', { shade: false })
  /* 螺旋排屑槽：四段错位的暗色筋 */
  box('flute_1', [6.35, 20.2, 7.5], [6.7, 23.0, 8.5], 'flat')
  box('flute_2', [9.3, 23.0, 7.5], [9.65, 25.9, 8.5], 'flat')
  box('flute_3', [7.5, 26.1, 6.85], [8.5, 29.0, 7.2], 'flat')
  box('flute_4', [7.5, 29.1, 8.8], [8.5, 31.3, 9.15], 'flat')
  /* 侧散热窗 */
  box('vent_1', [9.6, 7.6, 7.2], [9.85, 8.2, 8.8], 'flat')
  box('vent_2', [9.6, 9.0, 7.2], [9.85, 9.6, 8.8], 'flat')
  box('vent_3', [9.6, 15.6, 7.2], [9.85, 16.2, 8.8], 'flat')
  /* 四颗机铆钉 */
  box('rivet_1', [6.25, 11.0, 6.5], [6.55, 11.4, 6.9], 'accent', { shade: false })
  box('rivet_2', [9.45, 11.0, 6.5], [9.75, 11.4, 6.9], 'accent', { shade: false })
  box('rivet_3', [6.25, 15.4, 9.1], [6.55, 15.8, 9.5], 'accent', { shade: false })
  box('rivet_4', [9.45, 15.4, 9.1], [9.75, 15.8, 9.5], 'accent', { shade: false })
}

if (kind === 'crossbow') crossbow()
else if (kind === 'drill') drill()
else { console.error('未知 kind: ' + kind + '（支持 crossbow / drill）'); process.exit(1) }

/* 下限：与 weapon3d.mjs 一致，实测 3D 装备平均 34.1 个 element */
if (els.length < MIN_PARTS) {
  console.error('[gen-tool3d] 部件只有 ' + els.length + ' 个，低于 ' + MIN_PARTS +
    '（实测 22 个 3D 装备模型平均 34.1）。补箍 / 铆钉 / 缠绳 / 弦 / 尾羽等部件。')
  process.exit(2)
}

/* display：与 weapon3d 的 big_handheld 同值；gui 按包围盒算，保证不溢出物品格 */
const BIG = {
  thirdperson_righthand: { rotation: [0, -90, 55], translation: [0, 8, 0.5], scale: [1.75, 1.75, 0.85] },
  thirdperson_lefthand: { rotation: [0, 90, -55], translation: [0, 8, 0.5], scale: [1.7, 1.7, 0.85] },
  firstperson_righthand: { rotation: [0, -90, 25], translation: [1.13, 3.2, 1.13], scale: [1.36, 1.36, 0.68] },
  firstperson_lefthand: { rotation: [0, 90, -25], translation: [1.13, 3.2, 1.13], scale: [1.36, 1.36, 0.68] },
  ground: { translation: [0, 4, 0], scale: [1, 1, 0.5] },
  fixed: { rotation: [0, 180, 0], translation: [0, 0, -1], scale: [2, 2, 1] },
  head: { rotation: [0, 0, 0], translation: [0, 6, 0], scale: [1.5, 1.5, 1.5] },
}
const SLOT = 15
const xs = els.flatMap((e) => [e.from[0], e.to[0]])
const ys = els.flatMap((e) => [e.from[1], e.to[1]])
const minX = Math.min(...xs), maxX = Math.max(...xs)
const minY = Math.min(...ys), maxY = Math.max(...ys)
const s = Math.min(SLOT / (maxX - minX), SLOT / (maxY - minY))
const r4 = (v) => Math.round(v * 10000) / 10000

const model = {
  gui_light: 'front',
  texture_size: [AS, AS],
  textures: { all: modid + ':item/' + name, particle: modid + ':item/' + name },
  display: {
    ...BIG,
    gui: {
      rotation: [0, 0, 0],
      translation: [r4(8 - s * (minX + maxX) / 2), r4(8 - s * (minY + maxY) / 2), 0],
      scale: [r4(s), r4(s), r4(s)],
    },
  },
  elements: els,
}

const files = []
const write = (rel, content) => {
  const p = join(root, rel)
  mkdirSync(dirname(p), { recursive: true })
  writeFileSync(p, content)
  files.push(rel.replaceAll('\\', '/'))
}
write('src/main/resources/assets/' + modid + '/models/item/' + name + '.json', JSON.stringify(model, null, 2) + '\n')

/* 图集：每个区域按 y 方向铺一条色阶，随后用 pixel_art 精修 */
const pal = spec.palette ?? {}
const rgba = Buffer.alloc(AS * AS * 4)
const hex = (c) => { const v = c.replace('#', ''); return [parseInt(v.slice(0, 2), 16), parseInt(v.slice(2, 4), 16), parseInt(v.slice(4, 6), 16)] }
const rampFor = {
  blade: pal.blade ?? ['#2b3038', '#3f4750', '#5b6670', '#8a97a2', '#c2ccd4'],
  guard: pal.guard ?? ['#1c2026', '#2a3038', '#3a424c', '#4a545e'],
  wrap: [pal.wrap ?? '#2a2118'],
  pommel: pal.guard ?? ['#1c2026', '#2a3038'],
  gem: [pal.gem ?? '#8fd8ff'],
  tassel: [pal.tassel ?? '#7a4a2c'],
  accent: ['#c9a227'],
  flat: ['#12161a'],
}
for (const [key, r] of Object.entries(regions)) {
  const ramp = rampFor[key] ?? ['#808080']
  for (let y = r.y; y < r.y + r.h; y++) {
    for (let x = r.x; x < r.x + r.w; x++) {
      const t = (y - r.y) / Math.max(1, r.h - 1)
      const [rr, gg, bb] = hex(ramp[Math.min(ramp.length - 1, Math.floor(t * ramp.length))])
      const o = (y * AS + x) * 4
      rgba[o] = rr; rgba[o + 1] = gg; rgba[o + 2] = bb; rgba[o + 3] = 255
    }
  }
}
try {
  const sharp = loadSharp()
  if (!sharp) throw new Error('sharp 不可用：请确认 dsh-pixel-art 插件已安装或设置 DSH_SHARP_PKG')
  const png = join(root, 'src/main/resources/assets/' + modid + '/textures/item/' + name + '.png')
  mkdirSync(dirname(png), { recursive: true })
  await sharp(rgba, { raw: { width: AS, height: AS, channels: 4 } }).png().toFile(png)
  files.push('src/main/resources/assets/' + modid + '/textures/item/' + name + '.png')
} catch (e) {
  console.error('图集生成失败：', e.message.split('\n')[0])
}

const len = maxY - minY
const md = [
  '# ' + name + ' · 3D 器械（' + kind + '）',
  '',
  '- 长度：' + len.toFixed(1) + ' 单位 ≈ ' + (len / 16).toFixed(2) + ' 格',
  '- 部件：' + els.length + ' 个（' + els.map((e) => e.name).join('、') + '）',
  '- 图集：' + AS + 'x' + AS + '，gui 缩放 ' + r4(s),
  '',
  '| 区域 | 像素 | 用途 |',
  '|---|---|---|',
  '| blade | (0,0) 16x48 | 枪身 / 弩臂 / 钻头 |',
  '| guard | (16,0) 16x8 | 机匣 / 卡盘 / 弩臂座 |',
  '| wrap | (16,8) 16x16 | 握把 |',
  '| pommel | (16,24) 16x8 | 暗部配重 |',
  '| gem | (16,32) 8x8 | 压力表 / 瞄具宝石 |',
  '| tassel | (24,32) 8x16 | 背带 / 尾羽 |',
  '| accent | (32,0) 32x16 | 箍 / 铆钉 / 缠绳 / 箭头 |',
  '| flat | (32,16) 16x16 | 弦 / 排屑槽 / 散热窗 |',
  '',
].join('\n')
write('docs/' + name + '-atlas.md', md)

console.log(JSON.stringify({
  wrote: files,
  kind,
  lengthUnits: Number(len.toFixed(1)),
  lengthBlocks: Number((len / 16).toFixed(2)),
  elements: els.length,
  guiScale: r4(s),
}, null, 2))
