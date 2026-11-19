# 北极星星球附属 / Northstar Planet Addon
# 把四个旧附属工程（月球 / 水星 / 火星 / 金星）的 Java 包机械搬运并统一命名。
#
# 本脚本只生成「模块内容包」（block/client/entity/... 与 registry），四个模块的入口类
# （MoonModule / MercuryModule / MarsModule / VenusModule）与根入口 NorthstarPlanetAddon
# 是手写的，不在这里生成。
#
# 用法：powershell -ExecutionPolicy Bypass -File tools\regen-module-packages.ps1 [-Only mercury,mars]
param(
    [string[]] $Only = @('moon', 'mercury', 'mars', 'venus')
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot          # 工程根
$oldRoot = Split-Path -Parent $root              # 上一级：四个旧工程所在目录
$javaRoot = Join-Path $root 'src\main\java\com\suwako\northstarplanetaddon'
$new = 'northstar_planet_addon'
$utf8 = New-Object System.Text.UTF8Encoding($false)

$mods = @(
    @{ key = 'moon';    proj = 'NorthstarMoonAddon';    oldPkg = 'com.suwako.northstarmoonaddon';    newPkg = 'com.suwako.northstarplanetaddon.moon';    oldCls = 'NorthstarMoonAddon';    cls = 'MoonModule';
       nsTokens = @('northstar_moonaddon', 'northstar_moon_addon') }
    @{ key = 'mercury'; proj = 'NorthstarMercuryAddon'; oldPkg = 'com.suwako.northstarmercuryaddon'; newPkg = 'com.suwako.northstarplanetaddon.mercury'; oldCls = 'NorthstarMercuryAddon'; cls = 'MercuryModule';
       nsTokens = @('northstar_mercury_addon') }
    @{ key = 'mars';    proj = 'NorthstarMarsAddon';    oldPkg = 'com.vitran.northstarmarsaddon';     newPkg = 'com.suwako.northstarplanetaddon.mars';    oldCls = 'NorthstarMarsAddon';    cls = 'MarsModule';
       nsTokens = @('northstarmarsaddon') }
    @{ key = 'venus';   proj = 'phosphoros';            oldPkg = 'com.phosphoros';                    newPkg = 'com.suwako.northstarplanetaddon.venus';   oldCls = 'Phosphoros';            cls = 'VenusModule';
       nsTokens = @('phosphoros:') }
)

foreach ($m in $mods) {
    if ($Only -notcontains $m.key) { continue }

    $srcRoot = Join-Path (Join-Path (Join-Path $oldRoot $m.proj) 'src\main\java') ($m.oldPkg -replace '\.', '\')
    $outRoot = Join-Path $javaRoot $m.key
    if (Test-Path $outRoot) { Remove-Item $outRoot -Recurse -Force }
    New-Item -ItemType Directory -Path $outRoot -Force | Out-Null

    $entryFile = $m.oldCls + '.java'
    $count = 0
    Get-ChildItem $srcRoot -Recurse -File -Filter *.java | ForEach-Object {
        if ($_.Name -eq $entryFile) { return }          # 入口类手写，跳过

        $t = [System.IO.File]::ReadAllText($_.FullName, $utf8)

        # 1) 包名迁移
        $t = $t.Replace($m.oldPkg, $m.newPkg)
        # 2) 入口类改名
        $t = $t -creplace ('\b' + [regex]::Escape($m.oldCls) + '\b'), $m.cls
        # 3) 模块内的「模组 ID 常量」改成「命名空间常量」
        $t = $t -creplace '\bMODID\b', 'NAMESPACE'
        # 4) 事件订阅注解必须指向真正的 mod id
        $t = $t -creplace ('modid\s*=\s*' + $m.cls + '\.NAMESPACE'), 'modid = NorthstarPlanetAddon.MODID'
        # 5) 命名空间统一
        foreach ($tok in $m.nsTokens) { $t = $t.Replace($tok, $new) }
        if ($m.key -eq 'venus') {
            $t = $t.Replace('.phosphoros.', ('.' + $new + '.'))
            $t = $t.Replace('"phosphoros"', ('"' + $new + '"'))
        }
        # 6) 创造模式标签页给每个模块一个独立路径
        $t = $t.Replace('.register("main"', ('.register("' + $m.key + '"'))

        if ($t -cmatch 'NorthstarPlanetAddon\.MODID') {
            if ($t -notmatch 'import com\.suwako\.northstarplanetaddon\.NorthstarPlanetAddon;') {
                $t = $t -creplace '(?m)^(package [^\r\n]*;\r?\n)', ('$1' + "`r`n" + 'import com.suwako.northstarplanetaddon.NorthstarPlanetAddon;' + "`r`n")
            }
        }

        $rel = $_.FullName.Substring($srcRoot.Length).TrimStart('\')
        $target = Join-Path $outRoot $rel
        $dir = Split-Path $target -Parent
        if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
        [System.IO.File]::WriteAllText($target, $t, $utf8)
        $count++
    }
    Write-Host ("[{0}] 生成 {1} 个文件（入口类 {2}.java 手写，已跳过）" -f $m.key, $count, $m.cls)
}
Write-Host 'regen-module-packages.ps1 完成'
