#!/usr/bin/env node
/**
 * set-texture-size.mjs — 把 3D 物品模型的 texture_size 与图集实际尺寸对齐。
 *
 * uv 数值不用改：uv 是 0..16 空间，像素 = uv/16 * texture_size。所以把图集 2 倍放大、
 * texture_size 从 64 改成 128，采样位置完全不变，只是分辨率翻倍（可以画 1px 级细节）。
 *
 * 用法：node set-texture-size.mjs <model.json> <size>
 */
import { readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'

const [fileArg, sizeArg] = process.argv.slice(2)
if (!fileArg || !sizeArg) {
  console.error('用法: node set-texture-size.mjs <model.json> <size>')
  process.exit(1)
}
const file = resolve(fileArg)
const size = Number(sizeArg)
const model = JSON.parse(readFileSync(file, 'utf8'))
model.texture_size = [size, size]
writeFileSync(file, JSON.stringify(model, null, 2) + '\n')
console.log((file.split(/[\\/]/).pop()) + ': texture_size = [' + size + ', ' + size + ']')
