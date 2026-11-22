@echo off
rem ============================================================================
rem  一键启动 dsh —— 在当前工程目录里拉起 DeepSeek Harness 的 Web GUI
rem
rem  双击即可。行为：
rem    1. 以本 bat 所在目录（模组根目录）作为工作目录启动，会话就在这个工程里。
rem    2. 若 http://127.0.0.1:3080 已有实例在跑，不再重复启动，只把浏览器打开。
rem    3. 自动找 dsh 启动器：全局安装 > PATH 上的 dsh > npx 缓存 > npx 拉取。
rem    dsh web 会自己打开默认浏览器；关掉这个黑窗口 = 停止服务。
rem
rem  用法：  start-dsh.bat                启动（自动开浏览器）
rem          start-dsh.bat --check        只检查环境，不启动
rem          start-dsh.bat --port 3090    透传给 dsh web 的参数
rem ============================================================================
setlocal EnableExtensions
chcp 65001 >nul
pushd "%~dp0"

set "CHECKONLY="
set "DSH_ARGS="
:parse
if "%~1"=="" goto parsed
if /i "%~1"=="--check" (set "CHECKONLY=1") else (set "DSH_ARGS=%DSH_ARGS% %1")
shift
goto parse
:parsed

echo ---------------------------------------------------------------
echo   工作目录 : %CD%

rem ------------------------------------------------------ 端口占用检测
set "RUNNING="
netstat -ano | findstr /c:":3080" | findstr /i "LISTENING" >nul 2>&1
if not errorlevel 1 set "RUNNING=1"

rem ------------------------------------------------------ 找 dsh 启动器
set "DSH="
if exist "%APPDATA%\npm\dsh.cmd" set "DSH=%APPDATA%\npm\dsh.cmd"
if not defined DSH for /f "delims=" %%p in ('where dsh.cmd 2^>nul') do if not defined DSH set "DSH=%%p"
if not defined DSH for /d %%d in ("%LOCALAPPDATA%\npm-cache\_npx\*") do (
    if exist "%%d\node_modules\.bin\dsh.cmd" set "DSH=%%d\node_modules\.bin\dsh.cmd"
)

if defined DSH (
    echo   启动器   : %DSH%
) else (
    where node >nul 2>&1
    if errorlevel 1 (
        echo   [失败] 既没找到 dsh，也没有 node。
        echo          先装 Node.js ^(^>=18^)： https://nodejs.org/
        echo          再执行： npx -y @deepseek-ai/dsh --version
        goto :fail
    )
    echo   启动器   : npx -y @deepseek-ai/dsh ^(本地无缓存，需要联网^)
)

if defined RUNNING (
    echo   Web UI   : http://127.0.0.1:3080  ^(已在运行^)
) else (
    echo   Web UI   : http://127.0.0.1:3080  ^(即将启动^)
)
echo ---------------------------------------------------------------
echo.

if defined CHECKONLY (
    echo [--check] 环境检查完毕，未启动服务。
    goto :done
)

if defined RUNNING (
    echo 已经有 dsh 在 3080 上跑着，直接打开浏览器。
    start "" "http://127.0.0.1:3080"
    goto :done
)

echo 启动中…… 浏览器会自动打开；关掉本窗口即停止服务。
echo.
if defined DSH (
    call "%DSH%" web %DSH_ARGS%
) else (
    call npx -y @deepseek-ai/dsh web %DSH_ARGS%
)
set "RC=%ERRORLEVEL%"
echo.
if not "%RC%"=="0" (
    echo dsh 退出码 %RC%
    goto :fail
)
goto :done

:done
echo ===============================================================
popd
endlocal
exit /b 0

:fail
echo ===============================================================
popd
pause
endlocal
exit /b 1
