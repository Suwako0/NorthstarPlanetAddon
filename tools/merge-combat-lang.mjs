#!/usr/bin/env node
/**
 * merge-combat-lang.mjs — 把攻击模组的 tooltip 语言键合并进 en_us / zh_cn。
 *
 * attack.mjs 只产出 `docs/<name>-combat.lang.json.txt` 片段，这里按武器补上定制文案
 * （不再用生成器的 "A weapon with a special attack." 占位）。
 *
 * 用法：node tools/merge-combat-lang.mjs <resourcesDir>
 */
import { readFileSync, writeFileSync } from 'node:fs'
import { join } from 'node:path'

const resources = process.argv[2]
if (!resources) {
  console.error('用法: node tools/merge-combat-lang.mjs <resourcesDir>')
  process.exit(1)
}
const NS = 'northstar_planet_addon'
const en = {
  [`item.${NS}.molten_core_sword.desc`]: 'Molten Core Greatsword - the Ares forge core, still burning.',
  [`item.${NS}.molten_core_sword.ability`]: 'Right-click: geothermal slash, %1$s fire damage in a fan, %2$ss cooldown',
  [`item.${NS}.acidic_spear.desc`]: 'Acidic Spear - every hit stacks corrosion on the target.',
  [`item.${NS}.acidic_spear.ability`]: 'Right-click: acid burst, %1$s damage in a %2$s-range area and knockback',
  [`item.${NS}.pressure_hammer.desc`]: 'Pressure Hammer - heavy enough to overload a boss chassis.',
  [`item.${NS}.pressure_hammer.ability`]: 'Right-click after a full swing: shockwave, %1$s damage, %2$ss cooldown',
  [`item.${NS}.starcore_cannon.ability`]: 'Right-click: call down a starcore bombardment, %1$ss cooldown',
}
const zh = {
  [`item.${NS}.molten_core_sword.desc`]: '融核巨剑 —— 阿瑞斯熔炉的核心，至今仍在燃烧。',
  [`item.${NS}.molten_core_sword.ability`]: '右键「地热斩」：扇形内每个目标 %1$s 点火焰伤害并点燃，冷却 %2$s 秒',
  [`item.${NS}.acidic_spear.desc`]: '酸蚀矛 —— 每次命中都会在目标身上叠一层腐蚀。',
  [`item.${NS}.acidic_spear.ability`]: '右键「酸液迸发」：%2$s 格内 %1$s 点伤害并将目标击退',
  [`item.${NS}.pressure_hammer.desc`]: '压力锤 —— 重到能把 Boss 的机身砸出过载。',
  [`item.${NS}.pressure_hammer.ability`]: '蓄力满后右键「震荡波」：%1$s 点伤害，冷却 %2$s 秒',
  [`item.${NS}.starcore_cannon.ability`]: '右键「星核炮击」：在准心处召唤星核轰炸，冷却 %1$s 秒',
}

for (const [file, entries] of [['en_us.json', en], ['zh_cn.json', zh]]) {
  const path = join(resources, 'assets', NS, 'lang', file)
  const json = JSON.parse(readFileSync(path, 'utf8').replace(/^\uFEFF/, ''))
  let added = 0
  for (const [k, v] of Object.entries(entries)) {
    if (json[k] !== v) {
      json[k] = v
      added += 1
    }
  }
  writeFileSync(path, JSON.stringify(json, null, 2) + '\n')
  console.log(`${file}: 写入/更新 ${added} 条（总计 ${Object.keys(json).length} 条）`)
}
