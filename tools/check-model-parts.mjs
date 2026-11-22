// 静态一致性检查：手写模型类里 childOrEmpty(parent, "name") 用到的每个部件名，
// 必须存在于对应生成 mesh 的 PART_NAMES 里；否则运行时那个部件会静默不动（tolerant lookup 只 warn）。
// 用法：node tools/check-model-parts.mjs
import fs from 'node:fs';
import path from 'node:path';

const modelDir = path.resolve('src/main/java/com/suwako/northstarplanetaddon/mercury/client/model');

function readPartNames(meshFile) {
  const src = fs.readFileSync(meshFile, 'utf8');
  const m = /PART_NAMES\s*=\s*\{([\s\S]*?)\};/.exec(src);
  if (!m) throw new Error(`no PART_NAMES in ${meshFile}`);
  return new Set([...m[1].matchAll(/"([^"]+)"/g)].map(x => x[1]));
}

function readLookups(file) {
  const src = fs.readFileSync(file, 'utf8');
  return [...src.matchAll(/childOrEmpty\(\s*[^,]+,\s*"([^"]+)"\s*\)/g)].map(x => x[1]);
}

const checks = [
  {
    model: 'SunfishModel.java',
    mesh: 'SunfishMesh.java',
    extra: 'SkyfishModel.java'
  },
  {
    model: 'BlazingSunfishModel.java',
    mesh: 'BlazingSunfishMesh.java',
    extra: 'SkyfishModel.java'
  }
];

let failed = false;
for (const check of checks) {
  const names = readPartNames(path.join(modelDir, check.mesh));
  const used = [...readLookups(path.join(modelDir, check.model)), ...readLookups(path.join(modelDir, check.extra))];
  const missing = [...new Set(used)].filter(n => !names.has(n));
  if (missing.length) failed = true;
  console.log(JSON.stringify({
    mesh: check.mesh,
    meshParts: names.size,
    lookups: new Set(used).size,
    missingInMesh: missing
  }, null, 2));
}
process.exit(failed ? 1 : 0);
