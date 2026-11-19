# 北极星星球附属 / Northstar Planet Addon
# 从四个旧工程重建 src/main/resources，并把命名空间统一到 northstar_planet_addon。
#
# 做的事：
#   1) 清空并重建 assets/ 与 data/（保留 resources 根下的 META-INF、mixins.json、pack.mcmeta）
#   2) 从四个旧工程拷入 assets/<旧命名空间>、data/<旧命名空间>（月球另含结构模板用的历史命名空间）
#   3) data/minecraft/** 与 data/northstar/** 是多模块共用的标签文件，按 values 并集合并
#   4) 四个模块的 lang/en_us.json、lang/zh_cn.json、sounds.json 按并集合并（保留原始文本，不转义）
#   5) 把所有内容里的旧命名空间替换成新命名空间（Java 侧的替换见 regen-module-packages.ps1）
#
# 用法：powershell -ExecutionPolicy Bypass -File tools\regen-resources.ps1
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$oldProjectsRoot = Split-Path -Parent $root
$res = Join-Path $root 'src\main\resources'
$new = 'northstar_planet_addon'
$utf8 = New-Object System.Text.UTF8Encoding($false)

$modules = @(
    @{ proj = 'NorthstarMoonAddon';    ns = 'northstar_moon_addon';    extra = @('northstar_moonaddon') },
    @{ proj = 'NorthstarMercuryAddon'; ns = 'northstar_mercury_addon'; extra = @() },
    @{ proj = 'NorthstarMarsAddon';    ns = 'northstarmarsaddon';      extra = @() },
    @{ proj = 'phosphoros';            ns = 'phosphoros';              extra = @() }
)

$langKeyFix = @{
    'northstar_moon_addon'    = @(, @('"itemGroup.northstar_planet_addon"', '"itemGroup.northstar_planet_addon.moon"'))
    'northstar_mercury_addon' = @(, @('"itemGroup.northstar_planet_addon"', '"itemGroup.northstar_planet_addon.mercury"'))
    'northstarmarsaddon'      = @(, @('"itemGroup.northstar_planet_addon.main"', '"itemGroup.northstar_planet_addon.mars"'))
    'phosphoros'              = @(
        , @('"itemGroup.northstar_planet_addon.main"', '"itemGroup.northstar_planet_addon.venus"')
        , @('"effect.northstar_planet_addon.corrosion"', '"effect.northstar_planet_addon.acid_corrosion"')
        , @('"advancement.northstar_planet_addon.root.title": "机械动力：启明星"', '"advancement.northstar_planet_addon.root.title": "北极星金星附属"')
        , @('"advancement.northstar_planet_addon.root.title": "Create: Phosphoros"', '"advancement.northstar_planet_addon.root.title": "Northstar Venus Addon"')
    )
}

function Rewrite-Namespace([string] $text) {
    $t = $text
    $t = $t.Replace('northstar_moonaddon:', ($new + ':'))
    $t = $t.Replace('northstar_moon_addon', $new)
    $t = $t.Replace('northstar_mercury_addon', $new)
    $t = $t.Replace('northstarmarsaddon', $new)
    $t = $t.Replace('phosphoros:', ($new + ':'))
    $t = $t.Replace('.phosphoros.', ('.' + $new + '.'))
    $t = $t.Replace('"phosphoros"', ('"' + $new + '"'))
    return $t
}

# ---------------------------------------------------------------- 1) 清空 assets / data
foreach ($sub in @('assets', 'data')) {
    $p = Join-Path $res $sub
    if (Test-Path $p) { Remove-Item $p -Recurse -Force }
    New-Item -ItemType Directory -Path $p -Force | Out-Null
}

$sharedData = @{}
$count = 0

foreach ($m in $modules) {
    $srcRes = Join-Path (Join-Path $oldProjectsRoot $m.proj) 'src\main\resources'
    $a = Join-Path $srcRes ('assets\' + $m.ns)
    if (Test-Path $a) {
        $dst = Join-Path $res ('assets\' + $m.ns)
        New-Item -ItemType Directory -Path $dst -Force | Out-Null
        Get-ChildItem $a -Recurse -File | ForEach-Object {
            $rel = $_.FullName.Substring($a.Length).TrimStart('\')
            $target = Join-Path $dst $rel
            $dir = Split-Path $target -Parent
            if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
            Copy-Item $_.FullName $target -Force
            $count++
        }
    }
    foreach ($dataNs in ((@($m.ns) + @($m.extra)))) {
        $d = Join-Path $srcRes ('data\' + $dataNs)
        if (-not (Test-Path $d)) { continue }
        Get-ChildItem $d -Recurse -File | ForEach-Object {
            $rel = $_.FullName.Substring($d.Length).TrimStart('\')
            $target = Join-Path $res ('data\' + $dataNs + '\' + $rel)
            $dir = Split-Path $target -Parent
            if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
            Copy-Item $_.FullName $target -Force
            $count++
        }
    }
    foreach ($sharedNs in @('minecraft', 'northstar')) {
        $d = Join-Path $srcRes ('data\' + $sharedNs)
        if (-not (Test-Path $d)) { continue }
        Get-ChildItem $d -Recurse -File | ForEach-Object {
            $rel = $sharedNs + '\' + $_.FullName.Substring($d.Length).TrimStart('\')
            $key = $rel.ToLowerInvariant()
            if (-not $sharedData.ContainsKey($key)) {
                $sharedData[$key] = @{
                    rel     = $rel
                    values  = (New-Object System.Collections.ArrayList)
                    body    = $null
                    modules = (New-Object System.Collections.ArrayList)
                }
            }
            $entries = $sharedData[$key]
            [void]$entries.modules.Add($m.ns)
            $text = [System.IO.File]::ReadAllText($_.FullName, $utf8)
            if ($null -eq $entries.body) { $entries.body = $text }
            $obj = $text | ConvertFrom-Json
            foreach ($v in @($obj.values)) {
                if ($v -and -not $entries.values.Contains($v)) { [void]$entries.values.Add($v) }
            }
        }
    }
}

foreach ($key in $sharedData.Keys) {
    $e = $sharedData[$key]
    $target = Join-Path (Join-Path $res 'data') $e.rel
    $dir = Split-Path $target -Parent
    if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    if ($e.modules.Count -gt 1) {
        $lines = ($e.values | ForEach-Object { '    "' + $_ + '"' }) -join ",`r`n"
        $json = "{`r`n  `"replace`": false,`r`n  `"values`": [`r`n" + $lines + "`r`n  ]`r`n}`r`n"
        [System.IO.File]::WriteAllText($target, $json, $utf8)
        Write-Host ("shared tag union: {0} <- [{1}] = {2} values" -f $e.rel, ($e.modules -join ','), $e.values.Count)
    } else {
        [System.IO.File]::WriteAllText($target, $e.body, $utf8)
    }
    $count++
}

# ---------------------------------------------------------------- 2) 语言与声音：并集
$langEn = [ordered]@{}
$langZh = [ordered]@{}
$soundsBlobs = [ordered]@{}

function Collect-Lang([string] $blob, $store, [string] $label) {
    $text = Rewrite-Namespace $blob
    if ($langKeyFix.ContainsKey($label)) {
        foreach ($fix in $langKeyFix[$label]) { $text = $text.Replace($fix[0], $fix[1]) }
    }
    foreach ($line in ($text -split "`r?`n")) {
        $mm = [regex]::Match($line, '^\s*"([^"]+)"\s*:\s*(.+?),?\s*$')
        if (-not $mm.Success) { continue }
        $k = $mm.Groups[1].Value
        $v = $mm.Groups[2].Value.Trim()
        if ($store.Contains($k)) {
            if ($store[$k] -ne $v) {
                Write-Host ("   dup key (last wins): {0}" -f $k)
                $store[$k] = $v
            }
        } else {
            $store[$k] = $v
        }
    }
}

foreach ($m in $modules) {
    $assetsDir = Join-Path (Join-Path (Join-Path $oldProjectsRoot $m.proj) 'src\main\resources') ('assets\' + $m.ns)
    $en = Join-Path $assetsDir 'lang\en_us.json'
    $zh = Join-Path $assetsDir 'lang\zh_cn.json'
    $snd = Join-Path $assetsDir 'sounds.json'
    if (Test-Path $en) { Collect-Lang ([System.IO.File]::ReadAllText($en, $utf8)) $langEn $m.ns }
    if (Test-Path $zh) { Collect-Lang ([System.IO.File]::ReadAllText($zh, $utf8)) $langZh $m.ns }
    Write-Host ("   lang after {0}: en={1} zh={2}" -f $m.ns, $langEn.Count, $langZh.Count)
    if (Test-Path $snd) { $soundsBlobs[$m.ns] = (Rewrite-Namespace ([System.IO.File]::ReadAllText($snd, $utf8))) }
}

$langOut = Join-Path $res ('assets\' + $new + '\lang')
New-Item -ItemType Directory -Path $langOut -Force | Out-Null

$pairs = New-Object System.Collections.ArrayList
[void]$pairs.Add(@('en_us.json', $langEn))
[void]$pairs.Add(@('zh_cn.json', $langZh))
foreach ($pair in $pairs) {
    $lines = New-Object System.Collections.ArrayList
    foreach ($k in $pair[1].Keys) { [void]$lines.Add(('  "{0}": {1}' -f $k, $pair[1][$k])) }
    $body = "{" + "`r`n" + ($lines -join ",`r`n") + "`r`n" + "}" + "`r`n"
    [System.IO.File]::WriteAllText((Join-Path $langOut $pair[0]), $body, $utf8)
    Write-Host ("lang/{0}: {1} keys" -f $pair[0], $pair[1].Count)
}

if ($soundsBlobs.Count -gt 0) {
    $keys = @($soundsBlobs.Keys)
    $merged = $soundsBlobs[$keys[0]]
    for ($i = 1; $i -lt $keys.Count; $i++) {
        $inner = $soundsBlobs[$keys[$i]].Trim()
        $inner = $inner.Substring(1, $inner.Length - 2).Trim()
        if ([string]::IsNullOrWhiteSpace($inner)) { continue }
        $idx = $merged.LastIndexOf('}')
        $head = $merged.Substring(0, $idx).TrimEnd()
        if ($head.EndsWith('{')) { $merged = $head + "`r`n" + $inner + "`r`n}" + "`r`n" }
        else { $merged = $head + ",`r`n" + $inner + "`r`n}" + "`r`n" }
        Write-Host ("sounds.json <- {0}" -f $keys[$i])
    }
    [System.IO.File]::WriteAllText((Join-Path $res ('assets\' + $new + '\sounds.json')), $merged, $utf8)
}

Write-Host ("copied {0} files" -f $count)

# ---------------------------------------------------------------- 3) 归一到统一命名空间
$assetsFinal = Join-Path $res ('assets\' + $new)
$dataFinal = Join-Path $res ('data\' + $new)
New-Item -ItemType Directory -Path $assetsFinal -Force | Out-Null
New-Item -ItemType Directory -Path $dataFinal -Force | Out-Null

$moved = 0
$clashes = New-Object System.Collections.ArrayList
foreach ($m in $modules) {
    $a = Join-Path $res ('assets\' + $m.ns)
    if (Test-Path $a) {
        Get-ChildItem $a -Recurse -File | ForEach-Object {
            $rel = $_.FullName.Substring($a.Length).TrimStart('\')
            if ($rel -eq 'lang\en_us.json' -or $rel -eq 'lang\zh_cn.json' -or $rel -eq 'sounds.json') { return }
            $target = Join-Path $assetsFinal $rel
            if (Test-Path $target) { [void]$clashes.Add("assets\$rel <= $($m.ns)") }
            $dir = Split-Path $target -Parent
            if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
            Move-Item $_.FullName $target -Force
            $moved++
        }
        Remove-Item $a -Recurse -Force
    }
    foreach ($dataNs in ((@($m.ns) + @($m.extra)))) {
        $d = Join-Path $res ('data\' + $dataNs)
        if (-not (Test-Path $d)) { continue }
        Get-ChildItem $d -Recurse -File | ForEach-Object {
            $rel = $_.FullName.Substring($d.Length).TrimStart('\')
            $target = Join-Path $dataFinal $rel
            if (Test-Path $target) { [void]$clashes.Add("data\$rel <= $dataNs") }
            $dir = Split-Path $target -Parent
            if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
            Move-Item $_.FullName $target -Force
            $moved++
        }
        Remove-Item $d -Recurse -Force
    }
}

# 内容里的命名空间替换（只动 JSON，NBT 等二进制原样；
# 语言与 sounds.json 在合并阶段已经处理过，这里跳过，避免同一字符串被替换两次）
$textFiles = @()
$textFiles += Get-ChildItem $assetsFinal -Recurse -File -Filter *.json | Where-Object {
    $_.FullName -notmatch '\\lang\\' -and $_.Name -ne 'sounds.json'
}
$textFiles += Get-ChildItem (Join-Path $res 'data') -Recurse -File -Filter *.json
$rewritten = 0
foreach ($f in $textFiles) {
    $t = [System.IO.File]::ReadAllText($f.FullName, $utf8)
    $o = Rewrite-Namespace $t
    if ($o -ne $t) { [System.IO.File]::WriteAllText($f.FullName, $o, $utf8); $rewritten++ }
}

# ---------------------------------------------------------------- 4) 对外名称统一
# 四个星球附属统一成「北极星<星球>附属」，金星继续保留「启明星」作为素材名（只改标签页标题与效果名区分）
$displayFix = @(
    , @('"itemGroup.northstar_planet_addon.moon": "北极星月球拓展"', '"itemGroup.northstar_planet_addon.moon": "北极星月球附属"')
    , @('"itemGroup.northstar_planet_addon.moon": "NorthStar Moon Addon"', '"itemGroup.northstar_planet_addon.moon": "Northstar Moon Addon"')
    , @('"itemGroup.northstar_planet_addon.mercury": "北极星：水星附属"', '"itemGroup.northstar_planet_addon.mercury": "北极星水星附属"')
    , @('"itemGroup.northstar_planet_addon.mercury": "Northstar: Mercury Addon"', '"itemGroup.northstar_planet_addon.mercury": "Northstar Mercury Addon"')
    , @('"itemGroup.northstar_planet_addon.mars": "北极星：火星扩展"', '"itemGroup.northstar_planet_addon.mars": "北极星火星附属"')
    , @('"key.categories.northstar_planet_addon": "北极星：火星扩展"', '"key.categories.northstar_planet_addon": "北极星火星附属"')
    , @('"itemGroup.northstar_planet_addon.mars": "Northstar: Mars Addon"', '"itemGroup.northstar_planet_addon.mars": "Northstar Mars Addon"')
    , @('"key.categories.northstar_planet_addon": "Northstar: Mars Addon"', '"key.categories.northstar_planet_addon": "Northstar Mars Addon"')
    , @('"itemGroup.northstar_planet_addon.venus": "机械动力：启明星"', '"itemGroup.northstar_planet_addon.venus": "北极星金星附属"')
    , @('"itemGroup.northstar_planet_addon.venus": "Create: Phosphoros"', '"itemGroup.northstar_planet_addon.venus": "Northstar Venus Addon"')
    , @('"effect.northstar_planet_addon.acid_corrosion": "腐蚀"', '"effect.northstar_planet_addon.acid_corrosion": "酸蚀"')
    , @('"effect.northstar_planet_addon.acid_corrosion": "Corrosion"', '"effect.northstar_planet_addon.acid_corrosion": "Acid Corrosion"')
)
foreach ($langFile in @((Join-Path $langOut 'zh_cn.json'), (Join-Path $langOut 'en_us.json'))) {
    $t = [System.IO.File]::ReadAllText($langFile, $utf8)
    foreach ($fix in $displayFix) { $t = $t.Replace($fix[0], $fix[1]) }
    [System.IO.File]::WriteAllText($langFile, $t, $utf8)
}

Write-Host ("moved {0} files into assets/{1} and data/{1}; rewritten {2} JSON" -f $moved, $new, $rewritten)
if ($clashes.Count -gt 0) {
    Write-Host ("path clashes: {0}" -f $clashes.Count)
    foreach ($c in $clashes) { Write-Host ('   ! ' + $c) }
}
