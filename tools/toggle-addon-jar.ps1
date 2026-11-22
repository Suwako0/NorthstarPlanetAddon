#!/usr/bin/env pwsh
<#
.SYNOPSIS
  一键启停本模组：把 mods 里的 northstar_planet_addon-*.jar 改名成 .disabled 或改回来。
.DESCRIPTION
  用来做 A/B 测试：如果禁用本模组后游戏能进，问题就在本模组；
  如果照样卡在启动画面，就是整合包/环境问题。改回只需再加 -Enable。
.EXAMPLE
  pwsh -File tools\toggle-addon-jar.ps1 -Disable
  pwsh -File tools\toggle-addon-jar.ps1 -Enable
#>
param(
  [string]$GameDir = "D:\game\mc\.minecraft\versions\FloatingReverie",
  [switch]$Disable,
  [switch]$Enable
)
if (-not $Disable -and -not $Enable) { Write-Host "用法: -Disable 或 -Enable"; exit 1 }

$mods = Join-Path $GameDir "mods"
if (-not (Test-Path $mods)) { Write-Host "找不到 mods 目录: $mods"; exit 1 }

$jars = Get-ChildItem $mods -File -Filter "northstar_planet_addon*.jar"
$disabled = Get-ChildItem $mods -File -Filter "northstar_planet_addon*.jar.disabled"

if ($Disable) {
  if (-not $jars) { Write-Host "已经是禁用状态（没有 .jar）"; exit 0 }
  foreach ($j in $jars) { Rename-Item $j.FullName ($j.FullName + ".disabled"); Write-Host "[禁用] $($j.Name) → $($j.Name).disabled" }
  Write-Host "现在启动游戏做 A/B 测试；测完用 -Enable 改回来。"
} else {
  if (-not $disabled) { Write-Host "没有找到 .disabled 文件（可能本来就是启用状态）"; exit 0 }
  foreach ($d in $disabled) {
    $target = $d.FullName -replace '\.disabled$', ''
    if (Test-Path $target) { Write-Host "[跳过] 目标已存在: $target" ; continue }
    Rename-Item $d.FullName $target; Write-Host "[恢复] $($d.Name) → $(Split-Path $target -Leaf)"
  }
}
