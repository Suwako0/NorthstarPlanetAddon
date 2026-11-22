#!/usr/bin/env node
/**
 * gen-entity-model.mjs — 把 Blockbench 5 的 .bbmodel 工程转换成 Minecraft Java 实体模型的
 * PartDefinition（MeshDefinition）源码。零依赖，只用 Node 内置模块。
 *
 * 用法：
 *   node tools/gen-entity-model.mjs <input.bbmodel> <output.java 绝对路径> <package> <ClassName>
 *
 * 例：
 *   node tools/gen-entity-model.mjs build/scratch/models/sunfish.bbmodel \
 *        "src/main/java/.../SunfishMesh.java" \
 *        com.suwako.northstarplanetaddon.mercury.client.model SunfishMesh
 *
 * 转换约定（与项目几何规范一致）：
 *   - 只接受轴对齐几何：任何 cube 或任何组的 rotation 非 [0,0,0] 一律报错退出（非零退出码），
 *     因为旋转体素无法 1:1 还原成 Java 的 PartPose（PartPose 只有 offset + 三个欧拉角，且
 *     子部件的 pivot 必须落在父部件的坐标系里）。
 *   - inflate 烘焙进 from/to：from -= inflate，to += inflate（Minecraft 实体模型没有 inflate）。
 *   - 组 origin 是绝对坐标；PartPose.offset 用 origin - 父组 origin（顶层组的父 origin 视为 0）。
 *   - cube 的 from 换成相对组 origin 的坐标，尺寸用烘焙 inflate 之后的 to - from。
 *   - 组名原样作为部件名（手写模型类会用 getChild(name) 查找，改名会破坏动画）。
 *
 * .bbmodel 5.0 的真实结构（用 sunfish.bbmodel 实测，不是猜的）：
 *   - 顶层 meta.format_version = "5.0"，meta.model_format = "java_block"，resolution = {width,height}。
 *   - elements[]：每个 cube 带 name/from/to/inflate/uv_offset/faces/uuid/type="cube"；
 *     inflate 可能缺省（视为 0）；rotation 字段可能整个不存在（视为 [0,0,0]）；
 *     uv_offset 可能缺省（该 cube 的 UV 被逐面手动改过），此时从 faces.north 反推。
 *   - groups[]：组的“真身”，带 name/uuid/origin/rotation；但它的 children 数组是空的，
 *     **不能用它遍历树**。个别组的 origin 数组会有多余尾项（Blockbench 历史数据残留），
 *     只取前三个分量并告警。
 *   - outliner[]：树结构，节点对象只有 {uuid, isOpen, children}，name/origin/rotation 要按 uuid
 *     回到 groups[] 里查；children 里的字符串是 cube 的 uuid。
 */

import fs from 'node:fs';
import path from 'node:path';

/* ------------------------------------------------------------------ */
/* 错误报告                                                            */
/* ------------------------------------------------------------------ */

const warnings = [];

function warn(msg) {
    warnings.push(msg);
    process.stderr.write(`[warn] ${msg}\n`);
}

function fail(msg, details = []) {
    process.stderr.write(`[error] ${msg}\n`);
    for (const line of details) process.stderr.write(`        ${line}\n`);
    process.stderr.write('\n');
    process.stderr.write('用法: node tools/gen-entity-model.mjs <input.bbmodel> <output.java 绝对路径> <package> <ClassName>\n');
    process.exit(1);
}

/* ------------------------------------------------------------------ */
/* Java 保留字                                                         */
/* ------------------------------------------------------------------ */

const JAVA_KEYWORDS = new Set([
    'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const', 'continue',
    'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float', 'for', 'goto', 'if',
    'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'native', 'new', 'package', 'private',
    'protected', 'public', 'return', 'short', 'static', 'strictfp', 'super', 'switch', 'synchronized', 'this',
    'throw', 'throws', 'transient', 'try', 'void', 'volatile', 'while', 'true', 'false', 'null', 'var',
    'record', 'sealed', 'permits', 'yield',
]);

/* ------------------------------------------------------------------ */
/* 命令行                                                              */
/* ------------------------------------------------------------------ */

const USAGE = [
    '用法: node tools/gen-entity-model.mjs <input.bbmodel> <output.java 绝对路径> <package> <ClassName>',
    '',
    '  <input.bbmodel>     Blockbench 5 工程文件（java_block 格式）',
    '  <output.java>       输出 Java 源码路径（绝对路径；目录会自动创建）',
    '  <package>           目标包名，例如 com.example.mod.client.model',
    '  <ClassName>         生成的 package-private final 类名，例如 SunfishMesh',
].join('\n');

const argv = process.argv.slice(2);

if (argv.includes('-h') || argv.includes('--help')) {
    process.stdout.write(USAGE + '\n');
    process.exit(0);
}

if (argv.length !== 4) {
    fail(`参数数量不对：需要 4 个参数，实际收到 ${argv.length} 个。`, argv.map((a, i) => `argv[${i}] = ${a}`));
}

const [inPathArg, outPathArg, packageName, className] = argv;

if (!/^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*$/.test(packageName)) {
    fail(`包名不合法: "${packageName}"`);
}
if (!/^[A-Za-z_$][A-Za-z0-9_$]*$/.test(className)) {
    fail(`类名不合法: "${className}"`);
}
if (JAVA_KEYWORDS.has(className)) {
    fail(`类名不能是 Java 关键字: "${className}"`);
}

const inPath = path.resolve(inPathArg);
const outPath = path.resolve(outPathArg);

if (!fs.existsSync(inPath)) {
    fail(`输入文件不存在: ${inPath}`);
}
if (!fs.statSync(inPath).isFile()) {
    fail(`输入路径不是文件: ${inPath}`);
}
if (path.extname(outPath).toLowerCase() !== '.java') {
    fail(`输出文件必须以 .java 结尾: ${outPath}`);
}
if (!path.isAbsolute(outPathArg)) {
    // 允许但不鼓励：解析后仍是合法路径，只是提示一下。
    warn(`输出路径不是绝对路径（已解析为 ${outPath}）；任务约定用绝对路径。`);
}

/* ------------------------------------------------------------------ */
/* 读入 + 解析                                                         */
/* ------------------------------------------------------------------ */

let project;
try {
    const raw = fs.readFileSync(inPath, 'utf8');
    project = JSON.parse(raw);
} catch (err) {
    fail(`读取/解析 ${inPath} 失败: ${err.message}`);
}

if (project === null || typeof project !== 'object' || Array.isArray(project)) {
    fail(`${inPath} 不是一个 JSON 对象，不像是 .bbmodel 工程文件。`);
}

const formatVersion = project?.meta?.format_version;
if (typeof formatVersion === 'string' && !formatVersion.startsWith('5')) {
    warn(`format_version 是 ${formatVersion}，本脚本按 Blockbench 5 的结构解析；若字段有出入请先核对。`);
}

if (!Array.isArray(project.elements) || project.elements.length === 0) {
    fail(`${inPath} 里没有 elements 数组（或为空），没有可转换的 cube。`);
}
if (!Array.isArray(project.outliner) || project.outliner.length === 0) {
    fail(`${inPath} 里没有 outliner 数组（或为空），无法还原组树。`);
}
if (!Array.isArray(project.groups)) {
    warn('没有 groups 数组；只能依赖 outliner 节点自带的 name/origin（Blockbench 5 通常不会有 name）。');
}

const elements = project.elements;
const groups = Array.isArray(project.groups) ? project.groups : [];

/* ------------------------------------------------------------------ */
/* 基础校验：元素类型 / uuid / 数值                                     */
/* ------------------------------------------------------------------ */

const elementByUuid = new Map();
for (const el of elements) {
    if (el?.type !== 'cube') {
        fail(`elements 里出现了非 cube 元素: ${JSON.stringify({ name: el?.name, type: el?.type })}`, [
            '本转换器只支持 type === "cube" 的体素几何（mesh 无法用 CubeListBuilder 还原）。',
        ]);
    }
    if (typeof el.uuid !== 'string' || el.uuid.length === 0) {
        fail(`cube "${el.name ?? '<未命名>'}" 缺少 uuid，无法与 outliner 关联。`);
    }
    if (elementByUuid.has(el.uuid)) {
        fail(`cube uuid 重复: ${el.uuid}`);
    }
    elementByUuid.set(el.uuid, el);
}

const groupByUuid = new Map();
for (const g of groups) {
    if (typeof g.uuid !== 'string') {
        fail(`groups 里有组缺少 uuid: ${JSON.stringify(g?.name ?? g)}`);
    }
    groupByUuid.set(g.uuid, g);
}

function isVec3(v) {
    return Array.isArray(v) && v.length >= 3 && v.slice(0, 3).every((n) => typeof n === 'number' && Number.isFinite(n));
}

function describeVec(v) {
    if (!Array.isArray(v)) return String(v);
    return `[${v.join(', ')}]`;
}

/* ------------------------------------------------------------------ */
/* 硬性规则 3：只接受轴对齐几何                                         */
/* ------------------------------------------------------------------ */

{
    const badCubes = [];
    for (const el of elements) {
        if (!('rotation' in el) || el.rotation === null || el.rotation === undefined) continue; // 缺省 = 无旋转
        if (!isVec3(el.rotation)) {
            fail(`cube "${el.name}" 的 rotation 不是三元数字数组: ${describeVec(el.rotation)}`);
        }
        if (el.rotation.slice(0, 3).some((n) => n !== 0)) {
            badCubes.push(`cube "${el.name}" rotation=${describeVec(el.rotation)}`);
        }
    }

    const badGroups = [];
    for (const g of groups) {
        if (!('rotation' in g) || g.rotation === null || g.rotation === undefined) continue;
        if (!isVec3(g.rotation)) {
            fail(`组 "${g.name}" 的 rotation 不是三元数字数组: ${describeVec(g.rotation)}`);
        }
        if (g.rotation.slice(0, 3).some((n) => n !== 0)) {
            badGroups.push(`组 "${g.name}" rotation=${describeVec(g.rotation)}`);
        }
    }

    if (badCubes.length || badGroups.length) {
        fail(
            `检测到旋转几何：本项目的几何约定是只用轴对齐体素，旋转无法 1:1 还原成 Java PartPose，拒绝生成。共 ${badCubes.length} 个 cube / ${badGroups.length} 个组。`,
            [...badGroups, ...badCubes],
        );
    }
}

/* ------------------------------------------------------------------ */
/* 硬性校验：cube 的 from/to/inflate/uv_offset                          */
/* ------------------------------------------------------------------ */

for (const el of elements) {
    if (!isVec3(el.from)) fail(`cube "${el.name}" 的 from 不是三元数字数组: ${describeVec(el.from)}`);
    if (!isVec3(el.to)) fail(`cube "${el.name}" 的 to 不是三元数字数组: ${describeVec(el.to)}`);
    for (let i = 0; i < 3; i++) {
        if (el.to[i] < el.from[i]) {
            fail(`cube "${el.name}" 的 to 在轴 ${'xyz'[i]} 上小于 from: from=${describeVec(el.from)} to=${describeVec(el.to)}`);
        }
    }
    if ('inflate' in el && el.inflate !== null && el.inflate !== undefined) {
        if (typeof el.inflate !== 'number' || !Number.isFinite(el.inflate)) {
            fail(`cube "${el.name}" 的 inflate 不是数字: ${JSON.stringify(el.inflate)}`);
        }
    }
    if (el.box_uv === false) {
        warn(`cube "${el.name}" 的 box_uv 为 false（逐面 UV），texOffs 只能取本脚本反推的近似原点。`);
    }
}

/**
 * 计算 texOffs。优先用 uv_offset（box UV 的左上原点，单位为贴图像素）；
 * 缺失时用 box UV 布局从 faces.north 反推：north 面占 (u+d, v+d) 起，故 u = north.x0 - d, v = north.y0 - d。
 */
function texOffsOf(el) {
    const size = [el.to[0] - el.from[0], el.to[1] - el.from[1], el.to[2] - el.from[2]];
    const d = size[2];

    if (Array.isArray(el.uv_offset)) {
        if (el.uv_offset.length < 2 || typeof el.uv_offset[0] !== 'number' || typeof el.uv_offset[1] !== 'number') {
            fail(`cube "${el.name}" 的 uv_offset 不是 [u, v]: ${describeVec(el.uv_offset)}`);
        }
        const u = Math.round(el.uv_offset[0]);
        const v = Math.round(el.uv_offset[1]);
        if (el.uv_offset[0] !== u || el.uv_offset[1] !== v) {
            warn(`cube "${el.name}" 的 uv_offset 不是整数: ${describeVec(el.uv_offset)} → 取整为 [${u}, ${v}]。`);
        }
        return { u, v, derived: false };
    }

    // uv_offset 缺省：从 box UV 的 north 面反推。
    const northUv = el.faces?.north?.uv;
    if (!Array.isArray(northUv) || northUv.length < 2) {
        fail(`cube "${el.name}" 既没有 uv_offset，也没有 faces.north.uv，无法确定 texOffs。`);
    }
    const u = Math.round(northUv[0] - d);
    const v = Math.round(northUv[1] - d);

    // 交叉验证其余面是否落在标准 box UV 布局上（up/down 的 x 在 Blockbench 里是反向存的，故比极值）。
    const w = size[0];
    const h = size[1];
    const expect = {
        east: [u, v + d],
        north: [u + d, v + d],
        west: [u + d + w, v + d],
        south: [u + d + w + d, v + d],
        up: [u + d, v],
        down: [u + d + w, v],
    };
    const mismatched = [];
    for (const [face, [ex, ey]] of Object.entries(expect)) {
        const uv = el.faces?.[face]?.uv;
        if (!Array.isArray(uv) || uv.length < 4) continue;
        const x0 = Math.min(uv[0], uv[2]);
        const y0 = Math.min(uv[1], uv[3]);
        if (Math.round(x0) !== ex || Math.round(y0) !== ey) {
            mismatched.push(`${face}: uv=${describeVec(uv)} 期望原点 (${ex}, ${ey})`);
        }
    }
    if (mismatched.length) {
        warn(`cube "${el.name}" 缺 uv_offset，按 box UV 布局反推为 [${u}, ${v}]，但部分面不符合标准布局（逐面 UV 被改过）：${mismatched.join('; ')}`);
    } else {
        warn(`cube "${el.name}" 缺 uv_offset，按 box UV 布局从 faces.north 反推为 [${u}, ${v}]。`);
    }
    return { u, v, derived: true };
}

/* ------------------------------------------------------------------ */
/* 解析 outliner 树                                                    */
/* ------------------------------------------------------------------ */

const usedCubeUuids = new Set();
const groupNames = new Set();

/** @type {{kind:'group', name:string, uuid:string, origin:number[], cubes:object[], children:object[], parent:object|null, depth:number}[]} */
const orderedGroups = [];
/** @type {object[]} */
const orderedCubes = [];

let groupCount = 0;

function parseGroupNode(node, parent, depth) {
    const uuid = node.uuid;
    if (typeof uuid !== 'string' || uuid.length === 0) {
        fail(`outliner 里有组节点缺少 uuid: ${JSON.stringify(Object.keys(node ?? {}))}`);
    }

    const def = groupByUuid.get(uuid);
    if (!def) {
        fail(`outliner 里的组 ${uuid} 在 groups 数组里查不到，拿不到 name/origin。`, [
            'Blockbench 5 的 outliner 节点只存 {uuid, isOpen, children}，名称与 origin 在 groups[] 中。',
        ]);
    }

    const name = typeof def.name === 'string' && def.name.length > 0 ? def.name : node.name;
    if (typeof name !== 'string' || name.length === 0) {
        fail(`组 ${uuid} 没有名字（groups[] 与 outliner 节点都没有 name）。`);
    }
    if (groupNames.has(name)) {
        fail(`组名重复: "${name}"。部件名必须唯一，否则 getChild(name) 会有歧义。`);
    }
    groupNames.add(name);

    if (!Array.isArray(def.origin)) {
        fail(`组 "${name}" 的 origin 不是数组: ${JSON.stringify(def.origin)}`, [
            'Blockbench 的组只有 origin（绝对坐标），没有逐轴位置拆分。',
        ]);
    }
    if (def.origin.length > 3) {
        warn(`组 "${name}" 的 origin 有 ${def.origin.length} 个分量 ${describeVec(def.origin)}，只取前三个 [${def.origin.slice(0, 3).join(', ')}]（Blockbench 历史数据残留）。`);
    }
    if (!isVec3(def.origin)) {
        fail(`组 "${name}" 的 origin 不是三元数字数组: ${describeVec(def.origin)}`);
    }

    const g = {
        kind: 'group',
        name,
        uuid,
        origin: def.origin.slice(0, 3),
        cubes: [],
        children: [],
        parent,
        depth,
    };
    groupCount++;
    orderedGroups.push(g);

    if (!Array.isArray(node.children)) {
        fail(`组 "${name}" 的 outliner 节点缺少 children 数组。`);
    }

    for (const child of node.children) {
        if (typeof child === 'string') {
            addCube(child, g);
        } else if (child && typeof child === 'object') {
            const childGroup = parseGroupNode(child, g, depth + 1);
            g.children.push(childGroup);
        } else {
            fail(`组 "${name}" 的 children 里有无法识别的节点: ${JSON.stringify(child)}`);
        }
    }

    return g;
}

function addCube(uuid, group) {
    const el = elementByUuid.get(uuid);
    if (!el) {
        fail(`outliner 引用了不存在的 cube uuid: ${uuid}（组 "${group.name}"）。`);
    }
    if (usedCubeUuids.has(uuid)) {
        fail(`cube "${el.name}" (${uuid}) 在 outliner 里被引用了多次。`);
    }
    usedCubeUuids.add(uuid);
    const c = {
        kind: 'cube',
        name: el.name,
        uuid,
        element: el,
        group,
    };
    group.cubes.push(c);
    orderedCubes.push(c);
}

for (const rootNode of project.outliner) {
    if (typeof rootNode === 'string') {
        fail(`outliner 顶层直接挂着 cube（uuid ${rootNode}），它不属于任何组；本转换器要求所有 cube 都在组里。`);
    }
    if (!rootNode || typeof rootNode !== 'object') {
        fail(`outliner 顶层节点无法识别: ${JSON.stringify(rootNode)}`);
    }
    parseGroupNode(rootNode, null, 0);
}

const orphanCubes = elements.filter((el) => !usedCubeUuids.has(el.uuid));
if (orphanCubes.length) {
    warn(`${orphanCubes.length} 个 cube 不在 outliner 树里，被跳过: ${orphanCubes.map((e) => e.name).join(', ')}`);
}

if (groupCount === 0) {
    fail('没有解析到任何组，无法生成 PartDefinition 树。');
}

/* ------------------------------------------------------------------ */
/* Java 代码生成                                                       */
/* ------------------------------------------------------------------ */

const RESERVED_VARS = new Set(['root', 'mesh', 'PART_NAMES', 'build']);

const usedVars = new Set();
const varNameOf = new Map();

function javaVarName(groupName) {
    if (varNameOf.has(groupName)) return varNameOf.get(groupName);

    let base = groupName.replace(/[^A-Za-z0-9_]/g, '_');
    if (!/^[A-Za-z_]/.test(base)) base = `g_${base}`;
    if (JAVA_KEYWORDS.has(base) || RESERVED_VARS.has(base)) base = `${base}_`;

    let id = base;
    let n = 2;
    while (usedVars.has(id)) id = `${base}_${n++}`;
    usedVars.add(id);
    varNameOf.set(groupName, id);
    return id;
}

/** 浮点数格式化：一律带 F 后缀，去掉多余小数（4 → 4.0F，3.5 → 3.5F，-0.25 → -0.25F）。 */
function fmtF(value) {
    if (typeof value !== 'number' || !Number.isFinite(value)) {
        throw new Error(`无法格式化的非有限数值: ${value}`);
    }
    let v = Math.round(value * 1e6) / 1e6; // 消掉 0.1 之类二进制浮点噪声
    if (Object.is(v, -0)) v = 0;
    let s = v.toFixed(6);
    s = s.replace(/0+$/, '');
    if (s.endsWith('.')) s += '0';
    return `${s}F`;
}

function q(str) {
    return `"${String(str).replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"`;
}

function displayPath(p) {
    const rel = path.relative(process.cwd(), p);
    const normalized = rel && !rel.startsWith('..') && !path.isAbsolute(rel) ? rel : p;
    return normalized.replace(/\\/g, '/');
}

const INDENT_STMT = '        '; // 8 空格：build() 里的语句缩进（照抄示例格式）
const INDENT_CONT = '                '; // 16 空格：CubeListBuilder 链式调用的续行

const lines = [];

lines.push(`package ${packageName};`);
lines.push('');
lines.push('import net.minecraft.client.model.geom.PartPose;');
lines.push('import net.minecraft.client.model.geom.builders.CubeListBuilder;');
lines.push('import net.minecraft.client.model.geom.builders.MeshDefinition;');
lines.push('import net.minecraft.client.model.geom.builders.PartDefinition;');
lines.push('');
lines.push('/**');
lines.push(
    ` * 由 tools/gen-entity-model.mjs 从 ${displayPath(inPath)} 生成（${orderedCubes.length} cubes / ${groupCount} groups）。`,
);
lines.push(
    ` * 重新生成：node tools/gen-entity-model.mjs ${displayPath(inPath)} ${displayPath(outPath)} ${packageName} ${className}`,
);
lines.push(' */');
lines.push(`final class ${className} {`);
lines.push('    private ' + className + '() {');
lines.push('    }');
lines.push('');

// PART_NAMES：按层级先父后子（pre-order）。
const partNames = orderedGroups.map((g) => g.name);
{
    const prefix = '    static final String[] PART_NAMES = {';
    let line = prefix;
    const pieces = partNames.map((n, i) => q(n) + (i === partNames.length - 1 ? '' : ', '));
    for (const piece of pieces) {
        if (line.length + piece.length > 118 && line !== prefix) {
            lines.push(line.replace(/ +$/, ''));
            line = '            ' + piece;
        } else {
            line += piece;
        }
    }
    lines.push(line + '};');
}

lines.push('');
lines.push('    static void build(MeshDefinition mesh) {');
lines.push(`${INDENT_STMT}PartDefinition root = mesh.getRoot();`);

for (const g of orderedGroups) {
    const parentVar = g.parent ? javaVarName(g.parent.name) : 'root';
    const selfVar = javaVarName(g.name);
    const parentOrigin = g.parent ? g.parent.origin : [0, 0, 0];
    const dx = g.origin[0] - parentOrigin[0];
    const dy = g.origin[1] - parentOrigin[1];
    const dz = g.origin[2] - parentOrigin[2];

    lines.push(
        `${INDENT_STMT}PartDefinition ${selfVar} = ${parentVar}.addOrReplaceChild(${q(g.name)}, CubeListBuilder.create()`,
    );

    for (const c of g.cubes) {
        const el = c.element;
        const inflate = typeof el.inflate === 'number' && Number.isFinite(el.inflate) ? el.inflate : 0;

        // 规则 4：inflate 烘焙进 from/to。
        const fx = el.from[0] - inflate;
        const fy = el.from[1] - inflate;
        const fz = el.from[2] - inflate;
        const tx = el.to[0] + inflate;
        const ty = el.to[1] + inflate;
        const tz = el.to[2] + inflate;

        // 规则 5：坐标相对组 origin，尺寸用烘焙后的 to - from。
        const x0 = fx - g.origin[0];
        const y0 = fy - g.origin[1];
        const z0 = fz - g.origin[2];
        const sx = tx - fx;
        const sy = ty - fy;
        const sz = tz - fz;

        if (sx <= 0 || sy <= 0 || sz <= 0) {
            fail(`cube "${c.name}" 在烘焙 inflate 后尺寸非正: ${fmtF(sx)} x ${fmtF(sy)} x ${fmtF(sz)}`);
        }

        const { u, v } = texOffsOf(el);

        lines.push(`${INDENT_CONT}// ${c.name}`);
        lines.push(
            `${INDENT_CONT}.texOffs(${u}, ${v}).addBox(${fmtF(x0)}, ${fmtF(y0)}, ${fmtF(z0)}, ${fmtF(sx)}, ${fmtF(sy)}, ${fmtF(sz)})`,
        );
    }

    lines.push(`${INDENT_CONT}, PartPose.offset(${fmtF(dx)}, ${fmtF(dy)}, ${fmtF(dz)}));`);
}

lines.push('    }');
lines.push('}');
lines.push('');

const source = lines.join('\n');

/* ------------------------------------------------------------------ */
/* 生成结果自检：括号/引号配平                                          */
/* ------------------------------------------------------------------ */

function checkBalanced(text) {
    const stack = [];
    const pairs = { '}': '{', ')': '(', ']': '[' };
    let inString = false;
    let inChar = false;
    let inLineComment = false;
    let inBlockComment = false;
    let lineNo = 1;

    for (let i = 0; i < text.length; i++) {
        const ch = text[i];
        const next = text[i + 1];

        if (ch === '\n') {
            lineNo++;
            inLineComment = false;
            continue;
        }
        if (inLineComment) continue;
        if (inBlockComment) {
            if (ch === '*' && next === '/') {
                inBlockComment = false;
                i++;
            }
            continue;
        }
        if (inString) {
            if (ch === '\\') i++;
            else if (ch === '"') inString = false;
            continue;
        }
        if (inChar) {
            if (ch === '\\') i++;
            else if (ch === "'") inChar = false;
            continue;
        }

        if (ch === '/' && next === '/') {
            inLineComment = true;
            i++;
            continue;
        }
        if (ch === '/' && next === '*') {
            inBlockComment = true;
            i++;
            continue;
        }
        if (ch === '"') {
            inString = true;
            continue;
        }
        if (ch === "'") {
            inChar = true;
            continue;
        }

        if (ch === '{' || ch === '(' || ch === '[') {
            stack.push({ ch, lineNo });
        } else if (ch === '}' || ch === ')' || ch === ']') {
            const top = stack.pop();
            if (!top || top.ch !== pairs[ch]) {
                return `第 ${lineNo} 行的 '${ch}' 不匹配（栈顶是 ${top ? `'${top.ch}'（第 ${top.lineNo} 行）` : '空'}）`;
            }
        }
    }

    if (inString) return '存在未闭合的字符串字面量';
    if (inBlockComment) return '存在未闭合的块注释';
    if (stack.length) {
        const top = stack[stack.length - 1];
        return `第 ${top.lineNo} 行的 '${top.ch}' 未闭合`;
    }
    return null;
}

const balanceError = checkBalanced(source);
if (balanceError) {
    fail(`生成的 Java 源码括号/引号不配平: ${balanceError}`);
}

// 结构自检：每个组名都以字符串字面量的形式出现在 addOrReplaceChild 里。
for (const g of orderedGroups) {
    if (!source.includes(`addOrReplaceChild(${q(g.name)},`)) {
        fail(`内部错误：组 "${g.name}" 没有出现在生成的源码里。`);
    }
}

/* ------------------------------------------------------------------ */
/* 写出                                                                */
/* ------------------------------------------------------------------ */

try {
    fs.mkdirSync(path.dirname(outPath), { recursive: true });
    fs.writeFileSync(outPath, source, 'utf8');
} catch (err) {
    fail(`写出 ${outPath} 失败: ${err.message}`);
}

/* ------------------------------------------------------------------ */
/* 汇报                                                                */
/* ------------------------------------------------------------------ */

process.stdout.write(`source   : ${displayPath(inPath)}\n`);
process.stdout.write(`target   : ${outPath}\n`);
process.stdout.write(`class    : ${packageName}.${className}\n`);
process.stdout.write(`cubes    : ${orderedCubes.length}\n`);
process.stdout.write(`groups   : ${groupCount}\n`);
process.stdout.write(`parts    : ${partNames.join(', ')}\n`);
process.stdout.write(`lines    : ${source.split('\n').length}\n`);
process.stdout.write(`warnings : ${warnings.length}\n`);
process.stdout.write('OK\n');
