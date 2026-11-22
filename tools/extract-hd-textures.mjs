// 从 .bbmodel 导出内嵌贴图（PNG）以及每个 cube 的 box-UV 矩形清单。
// 用法：node tools/extract-hd-textures.mjs <input.bbmodel> <outDir> <manifestPath>
// 说明：bbmodel 里的 textures[].source 是 data:image/png;base64,...，这里原样解码；
// cube 的 UV 矩形按 Minecraft box-UV 布局计算，供发光层遮罩脚本使用。
import fs from 'node:fs';
import path from 'node:path';

const [input, outDir, manifestPath] = process.argv.slice(2);
if (!input || !outDir) {
  console.error('usage: node tools/extract-hd-textures.mjs <input.bbmodel> <outDir> [manifest.json]');
  process.exit(2);
}

const model = JSON.parse(fs.readFileSync(input, 'utf8'));
fs.mkdirSync(outDir, { recursive: true });

const written = [];
for (const tex of model.textures || []) {
  const src = tex.source || '';
  const m = /^data:image\/png;base64,(.*)$/s.exec(src);
  if (!m) {
    console.error(`texture "${tex.name}" has no embedded PNG source, skipped`);
    continue;
  }
  const file = path.join(outDir, `${tex.name}.png`);
  fs.writeFileSync(file, Buffer.from(m[1], 'base64'));
  written.push({ name: tex.name, file, bytes: fs.statSync(file).size });
}

// box-UV island rect per cube: Java/Blockbench share this unwrap
const cubes = [];
for (const el of model.elements || []) {
  if (el.type && el.type !== 'cube') continue;
  if (!el.box_uv) continue;
  const [u, v] = el.uv_offset || [0, 0];
  const w = Math.abs(el.to[0] - el.from[0]);
  const h = Math.abs(el.to[1] - el.from[1]);
  const d = Math.abs(el.to[2] - el.from[2]);
  cubes.push({
    name: el.name,
    uuid: el.uuid,
    u, v,
    w, h, d,
    // whole box-UV block of this cube, in texture pixels
    rect: [u, v, Math.ceil(2 * d + 2 * w), Math.ceil(d + h)]
  });
}

if (manifestPath) {
  fs.writeFileSync(manifestPath, JSON.stringify({
    source: input,
    textureSize: [model.resolution?.width ?? model.texture_width ?? 128, model.resolution?.height ?? model.texture_height ?? 128],
    textures: written.map(t => t.name),
    cubes
  }, null, 2));
}

console.log(JSON.stringify({ textures: written, cubes: cubes.length, manifest: manifestPath || null }, null, 2));
