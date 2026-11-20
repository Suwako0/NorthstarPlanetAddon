#!/usr/bin/env node
/**
 * patch-armor-model.mjs — 修 armor3d.mjs 生成的盔甲模型，让它能直接编译进 1.21.1。
 *
 * 1) 补 ArmorDecoModel 接口（渲染层要按统一类型调用 setSlot）。
 * 2) 给 addBox(...) / PartPose.offset(...) 里的 double 字面量补 `F`：
 *    1.21.1 这几个方法的形参是 float，生成器写的是 `-2.2` 这样的字面量，
 *    直接编译会报「从 double 转换到 float 可能会有损失」（实测 12 处/模型）。
 *
 * 用法：node patch-armor-model.mjs <file.java...>
 */
import { readFileSync, writeFileSync } from 'node:fs'
import { resolve } from 'node:path'

const IMPORT = 'import com.suwako.northstarplanetaddon.client.render.ArmorDecoModel;'
const FLOAT_CALLS = ['addBox(', 'PartPose.offset(', 'PartPose.rotation(', 'PartPose.offsetAndRotation(']
const DECIMAL = /(?<![\w.])(-?\d+\.\d+)(?![\dFf])/g

/** 只作用于「整行里出现在 float 形参位置」的数字：按行处理，重写调用括号内的片段。 */
function fixFloats(src) {
  let changed = 0
  const out = src.split('\n').map((line) => {
    let result = line
    for (const call of FLOAT_CALLS) {
      let from = 0
      for (;;) {
        const start = result.indexOf(call, from)
        if (start < 0) break
        const open = start + call.length - 1
        let depth = 0
        let end = -1
        for (let i = open; i < result.length; i += 1) {
          if (result[i] === '(') depth += 1
          else if (result[i] === ')') {
            depth -= 1
            if (depth === 0) { end = i; break }
          }
        }
        if (end < 0) break
        const inner = result.slice(open + 1, end)
        const fixed = inner.replace(DECIMAL, (m) => { changed += 1; return m + 'F' })
        result = result.slice(0, open + 1) + fixed + result.slice(end)
        from = open + 1 + fixed.length
      }
    }
    return result
  }).join('\n')
  return { out, changed }
}

for (const arg of process.argv.slice(2)) {
  const file = resolve(arg)
  let src = readFileSync(file, 'utf8')

  const { out, changed } = fixFloats(src)
  src = out

  if (!src.includes(IMPORT)) {
    src = src.replace(/^(package [^\n]+;\n)/m, '$1\n' + IMPORT + '\n')
  }
  src = src.replace(' extends HumanoidModel<LivingEntity> {',
    ' extends HumanoidModel<LivingEntity> implements ArmorDecoModel {')

  writeFileSync(file, src)
  console.log(`${file} — float 字面量修正 ${changed} 处`)
}
