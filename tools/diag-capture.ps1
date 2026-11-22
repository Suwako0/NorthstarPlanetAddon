#!/usr/bin/env pwsh
<#
.SYNOPSIS
  抓取 Minecraft 启动卡死时的现场：线程栈 + 最新日志。
.DESCRIPTION
  当游戏"卡在启动画面/未响应"时运行本脚本（游戏不要关）。它会：
    1) 找到 javaw.exe 的 PID 与它实际使用的 java 路径；
    2) 依次尝试 jcmd / 同版本 jstack / 任意可用 jstack 导出线程栈；
    3) 把 logs/latest.log、logs/debug.log 与栈文件一起复制到 build/diag/<时间戳>/（build/ 已忽略，属临时产物）。
  没抓到栈也不影响：日志也照样收，能定位到卡在哪个阶段。
.EXAMPLE
  pwsh -File tools\diag-capture.ps1
  pwsh -File tools\diag-capture.ps1 -GameDir "D:\game\mc\.minecraft\versions\FloatingReverie"
#>
param(
  [string]$GameDir = "D:\game\mc\.minecraft\versions\FloatingReverie",
  [string]$OutRoot = "build\diag"
)

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$out = Join-Path $OutRoot $stamp
New-Item -ItemType Directory -Force -Path $out | Out-Null

# 1) 找游戏进程
$proc = Get-CimInstance Win32_Process -Filter "Name='javaw.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -match 'minecraft|forgeclient|FloatingReverie' } |
        Sort-Object WorkingSetSize -Descending | Select-Object -First 1

if (-not $proc) {
  Write-Host "[!] 没找到正在运行的 javaw（游戏可能已经退出）。仍然会收集现有日志。"
  Write-Host "    如果是启动瞬间就没了，请先重开游戏、等 3 分钟不要点窗口，再运行本脚本。"
} else {
  Write-Host ("[+] PID {0}，内存占用 {1:N1} GB" -f $proc.ProcessId, ($proc.WorkingSetSize / 1GB))
  $javaExe = ($proc.CommandLine -split '"' | Where-Object { $_ -match 'java(w)?\.exe$' } | Select-Object -First 1)
  if (-not $javaExe) { $javaExe = ($proc.ExecutablePath) }
  Write-Host "[+] java: $javaExe"
  Set-Content (Join-Path $out "process.txt") ("PID={0}`r`nCmdLine={1}`r`nJava={2}" -f $proc.ProcessId, $proc.CommandLine, $javaExe) -Encoding UTF8

  $javaHome = if ($javaExe) { Split-Path (Split-Path $javaExe -Parent) -Parent } else { $null }
  $candidates = @()
  if ($javaHome) { $candidates += (Join-Path $javaHome "bin\jcmd.exe"); $candidates += (Join-Path $javaHome "bin\jstack.exe") }
  $candidates += "C:\Program Files\Microsoft\jdk-17.0.15.6-hotspot\bin\jstack.exe"
  $candidates = $candidates | Where-Object { $_ -and (Test-Path $_) } | Select-Object -Unique

  $dump = Join-Path $out "threads.txt"
  $ok = $false
  foreach ($tool in $candidates) {
    $name = Split-Path $tool -Leaf
    Write-Host "[*] 尝试 $name ..."
    try {
      if ($name -eq 'jcmd.exe') { & $tool $proc.ProcessId Thread.print 2>&1 | Out-File -FilePath $dump -Encoding UTF8 }
      else { & $tool -l $proc.ProcessId 2>&1 | Out-File -FilePath $dump -Encoding UTF8 }
      $txt = Get-Content $dump -Raw -ErrorAction SilentlyContinue
      if ($txt -and ($txt -match 'Thread|"' ) -and ($txt -notmatch 'Error|Unable to attach')) { $ok = $true; break }
    } catch {
      Write-Host ("    {0} 失败：{1}" -f $name, $_.Exception.Message)
    }
  }
  if ($ok) {
    Write-Host "[+] 线程栈已保存：$dump"
    # 快速高亮主线程位置，方便一眼看出卡在哪
    $main = Select-String -Path $dump -Pattern '^"main"' -Context 0,14 -ErrorAction SilentlyContinue
    if ($main) { Write-Host "`n--- main 线程顶部 ---"; $main[0].Context.PostContext | Select-Object -First 12 }
  } else {
    Write-Host "[!] 没能导出线程栈（缺少可用的 jcmd/jstack）。日志仍会收集。"
  }
}

# 2) 收日志
foreach ($f in @('logs\latest.log', 'logs\debug.log')) {
  $src = Join-Path $GameDir $f
  if (Test-Path $src) {
    Copy-Item $src (Join-Path $out (Split-Path $f -Leaf)) -Force
    Write-Host ("[+] 已收集 {0}（{1:N0} 字节）" -f $f, (Get-Item $src).Length)
  }
}
# 3) 系统事件里的 Application Hang / Error（最近 30 分钟）
try {
  Get-WinEvent -FilterHashtable @{ LogName='Application'; StartTime=(Get-Date).AddMinutes(-30) } -ErrorAction Stop |
    Where-Object { $_.ProviderName -match 'Application Hang|Application Error|Windows Error Reporting' } |
    Select-Object TimeCreated, ProviderName, Id, @{n='Msg';e={ ($_.Message -split "`n")[0] }} |
    Export-Csv (Join-Path $out "windows-events.csv") -NoTypeInformation -Encoding UTF8
  Write-Host "[+] 系统事件已导出：windows-events.csv"
} catch { Write-Host "[!] 读事件日志失败：$($_.Exception.Message)" }

Write-Host "`n完成。把整个目录发给助手即可：$out"
