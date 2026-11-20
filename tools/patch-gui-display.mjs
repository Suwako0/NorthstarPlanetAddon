#!/usr/bin/env node
/**
 * patch-gui-display.mjs — 给 weapon3d.mjs 产出的 3D 物品模型补上 `gui` display 变换。
 *
 * weapon3d.mjs 只写 thirdperson / firstperson / ground / fixed / head，没有 gui。
 * 3D elements 模型在 GUI 里按 16 单位 = 16 px 的原点恒等变换绘制，
 * 一把 38 单位长的巨剑会直接溢出物品格。这里按包围盒算出「缩放到 15 单位 + 居中」的
 * gui 变换写回模型 JSON（不碰 elements 与其它 display 项）。
 *
 * 用法：node patch-gui-display.mjs <projectRoot> <modelName...>
 */
import { readFileSync, writeFileSync } from 'node:fs'
import { join, resolve } from 'node:path'

const [rootArg, ...names] = process.argv.slice(2)
if (!rootArg || names.length === 0) {
  console.error('用法: node patch-gui-display.mjs <projectRoot> <modelName...>')
  process.exit(1)
}
const root = resolve(rootArg)
const modid = 'northstar_planet_addon'
const SLOT = 15 // 目标占用 15/16 个格子，留 1px 透明度

for (const name of names) {
  const file = join(root, 'src/main/resources/assets', modid, 'models/item', name + '.json')
  const model = JSON.parse(readFileSync(file, 'utf8'))
  const els = model.elements
  if (!Array.isArray(els) || els.length === 0) {
    console.error(name + ': 没有 elements，跳过（不是 3D 模型）')
    continue
  }
  let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity
  for (const e of els) {
    minX = Math.min(minX, e.from[0], e.to[0])
    maxX = Math.max(maxX, e.from[0], e.to[0])
    minY = Math.min(minY, e.from[1], e.to[1])
    maxY = Math.max(maxY, e.from[1], e.to[1])
  }
  const w = maxX - minX
  const h = maxY - minY
  const s = Math.min(SLOT / w, SLOT / h)
  const tx = 8 - s * (minX + maxX) / 2
  const ty = 8 - s * (minY + maxY) / 2
  model.display = model.display ?? {}
  model.display.gui = {
    rotation: [0, 0, 0],
    translation: [round(tx), round(ty), 0],
    scale: [round(s), round(s), round(s)],
  }
  writeFileSync(file, JSON.stringify(model, null, 2) + '\n')
  console.log(name + ': gui scale ' + round(s) + ', translation ' + round(tx) + ',' + round(ty) +
    ' （包围盒 ' + round(w) + 'x' + round(h) + ' 单位）')
}

function round(v) {
  return Math.round(v * 10000) / 10000
}
