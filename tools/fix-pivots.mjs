// 修正 .bbmodel 里骨骼组的枢轴（origin）。Blockbench 的 group.origin 在脚本里赋值会被写坏
// （出现 4~6 分量、y 覆盖 x 等异常），所以直接改文件层：cube 坐标是模型绝对坐标，
// 组 origin 只是动画枢轴，改它不会移动几何。
// 用法：node tools/fix-pivots.mjs <input.bbmodel> [output.bbmodel]
import fs from 'node:fs';

const [input, output = input] = process.argv.slice(2);
if (!input) {
  console.error('usage: node tools/fix-pivots.mjs <input.bbmodel> [output.bbmodel]');
  process.exit(2);
}

const SHARED = {
  body: [0, 12, 2],
  neck1: [0, 14, -2],
  neck2: [0, 16, -4.5],
  head: [0, 17.5, -6],
  jaw: [0, 15.5, -7],
  horn_l: [-3, 20, -7], horn_l2: [-4, 21.5, -6.5], horn_l3: [-5, 22.5, -6],
  horn_r: [3, 20, -7], horn_r2: [4, 21.5, -6.5], horn_r3: [5, 22.5, -6],
  fin_l: [-3.5, 12, 0.5], fin_l_tip: [-6, 10.5, 2],
  fin_r: [3.5, 12, 0.5], fin_r_tip: [6, 10.5, 2],
  belly: [0, 10, 6],
  tail1: [0, 10, 9.5], tail2: [0, 10.5, 12], tail3: [0, 12, 14.5], tail_fin: [0, 13, 16.5],
  streamer_a: [0, 14, 17.5], streamer_b: [0, 12.5, 18], streamer_c: [0, 10.5, 18.5], streamer_d: [0, 8.5, 17.5],
  spine1: [0, 17, -5], spine2: [0, 17, -3], spine3: [0, 16, -1], spine4: [0, 15, 1],
  spine5: [0, 14, 3], spine6: [0, 13, 5], spine7: [0, 12, 7],
  ventral_l: [-2, 7, 5], ventral_r: [2, 7, 5]
};

const EXTRA = {
  core: [0, 12.5, -2.5],
  barbel_l: [-3.5, 17, -11],
  barbel_r: [3.5, 17, -11],
  crest: [0, 21.5, -6]
};

const model = JSON.parse(fs.readFileSync(input, 'utf8'));
const pivots = { ...SHARED, ...EXTRA };
const report = [];
for (const group of model.groups || []) {
  const target = pivots[group.name];
  if (!target) continue;
  const before = Array.isArray(group.origin) ? group.origin.slice() : [];
  group.origin = [target[0], target[1], target[2]];
  report.push({ name: group.name, before, after: group.origin });
}

fs.writeFileSync(output, JSON.stringify(model, null, '\t'));
console.log(JSON.stringify({
  file: output,
  fixed: report.length,
  groups: (model.groups || []).length,
  changed: report.filter(r => JSON.stringify(r.before) !== JSON.stringify(r.after))
}, null, 2));
