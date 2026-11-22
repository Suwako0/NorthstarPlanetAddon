#!/usr/bin/env node
/**
 * fix-hold.mjs — 把 3D 武器的握持变换改成"握把居中"版本。
 *
 * 为什么：原版 `item/handheld` 的 thirdperson translation 是 `[0,4,0.5]`，
 * 其中 Y=4 是给 **2D 图标**补的 —— 2D 图标的手柄画在左下角，距图标中心约 4~6 单位，
 * 所以要往上抬一点手柄才落在手里。
 *
 * 3D 模型只要把握把中心对齐到模型原点（模型 0..16 空间的 (8,8,8)），
 * 旋转/缩放都以原点为中心，手柄天然就在手上，**不需要再补这 4 个单位**，
 * 否则整把武器会浮在手外 0.25 格。所以这里把 thirdperson 平移压到 `[0,1,0.5]`
 * （留 1 单位微调 + 0.5 的 Z 前后偏移），rotation / scale 保持原版值不动。
 *
 * 用法：node tools/fix-hold.mjs <model.json...>
 */
import { readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'

const TRANSLATION = [0, 1, 0.5]

for (const arg of process.argv.slice(2)) {
  const file = resolve(arg)
  const model = JSON.parse(readFileSync(file, 'utf8'))
  const display = model.display ?? {}
  let changed = 0

  for (const key of ['thirdperson_righthand', 'thirdperson_lefthand']) {
    const entry = display[key]
    if (!entry || !entry.translation) continue
    const before = entry.translation.join(',')
    // 左手镜像原版是 x 相同、y 相同、z 相同（只有 rotation 镜像），这里统一取同一组平移
    entry.translation = [...TRANSLATION]
    if (entry.translation.join(',') !== before) changed += 1
  }

  const name = file.split(/[\\/]/).pop()
  const tp = display.thirdperson_righthand ?? {}
  writeFileSync(file, JSON.stringify(model, null, 2) + '\n')
  console.log(`${name}: thirdperson translation → [${(tp.translation ?? []).join(', ')}]` +
    ` | rotation [${(tp.rotation ?? []).join(', ')}] | scale ${(tp.scale ?? [])[0]}` +
    (changed ? '' : ' (已是最新)'))
}
