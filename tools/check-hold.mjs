#!/usr/bin/env node
/**
 * check-hold.mjs — 校验 6 把 3D 武器的「握持」是否真的正确，不是靠肉眼。
 *
 * 判据（三条全过才算 pass）：
 *   1. 握把中心（grip / wrap 部件的包围盒中点）落在模型空间的 (8,8,8)，
 *      也就是物品渲染器平移 0.5 格后的**支点原点** —— 手正好握在握把上；
 *   2. thirdperson 的 translation 不含 2D 图标那 4 单位补偿，应为 [0,1,0.5]；
 *   3. rotation 为原版 [0,∓90,55]，scale 为单手 0.85 / 长柄 0.75。
 *
 * 用法：node tools/check-hold.mjs [resourcesDir]
 */
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const resources = process.argv[2] ?? 'src/main/resources'
const NS = 'northstar_planet_addon'
const MODELS = [
  ['molten_core_sword', 'one'],
  ['starcore_cannon', 'long'],
  ['acidic_spear', 'long'],
  ['pressure_hammer', 'one'],
  ['thunder_crossbow', 'long'],
  ['pressure_drill', 'long'],
]
const EXPECT_SCALE = { one: 0.85, long: 0.75 }

let failed = 0
for (const [name, kind] of MODELS) {
  const model = JSON.parse(readFileSync(join(resources, 'assets', NS, 'models/item', name + '.json'), 'utf8'))
  const els = model.elements ?? []
  const grip = els.filter((e) => /grip|wrap/.test(e.name ?? ''))
  const problems = []

  if (!grip.length) {
    problems.push('没有 grip/wrap 部件，无法判定握把')
  } else {
    const center = [0, 1, 2].map((axis) => {
      const vals = grip.flatMap((e) => [e.from[axis], e.to[axis]])
      return (Math.min(...vals) + Math.max(...vals)) / 2
    })
    const off = Math.max(...center.map((c) => Math.abs(c - 8)))
    if (off > 0.01) problems.push(`握把中心 (${center.map((c) => c.toFixed(2)).join(',')}) 偏离 (8,8,8) ${off.toFixed(2)}`)
  }

  for (const key of ['thirdperson_righthand', 'thirdperson_lefthand']) {
    const t = model.display?.[key]?.translation
    const r = model.display?.[key]?.rotation
    const s = model.display?.[key]?.scale?.[0]
    const expectedRot = key.endsWith('righthand') ? [0, -90, 55] : [0, 90, -55]
    if (!t || t.join(',') !== '0,1,0.5') problems.push(`${key}.translation = [${(t ?? []).join(',')}]，应为 [0,1,0.5]`)
    if (!r || r.join(',') !== expectedRot.join(',')) problems.push(`${key}.rotation = [${(r ?? []).join(',')}]，应为 [${expectedRot.join(',')}]`)
    if (s !== EXPECT_SCALE[kind]) problems.push(`${key}.scale = ${s}，应为 ${EXPECT_SCALE[kind]}`)
  }

  if (problems.length) {
    failed += 1
    console.log(`✗ ${name}`)
    for (const p of problems) console.log('    - ' + p)
  } else {
    const s = model.display.thirdperson_righthand.scale[0]
    console.log(`✓ ${name}  握把=(8,8,8)  rotation=[0,-90,55]  translation=[0,1,0.5]  scale=${s}`)
  }
}
console.log(failed === 0 ? '\n全部通过：6 把武器的握把都精确落在支点原点。' : `\n${failed} 把不合格。`)
process.exit(failed === 0 ? 0 : 1)
