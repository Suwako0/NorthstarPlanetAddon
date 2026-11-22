@echo off
rem ============================================================================
rem  一键构建 jar —— Northstar Planet Addon (NeoForge 1.21.1 / Gradle 9.2.1)
rem
rem  双击即可。它做三件事：
rem    1. 找 JDK 21 并设好 JAVA_HOME。Gradle wrapper 在没有 JAVA_HOME 时会报
rem       "-classpath requires class path specification"；找不到 21 就退到 17，
rem       由 Gradle 的 foojay resolver 自动下载 21 工具链。
rem    2. 调 gradlew build；失败时自动用 --offline 重试（maven.neoforged.net
rem       在部分网络下不可达，而依赖已在本机 Gradle 缓存里）。
rem    3. 打印产出的 jar 路径。
rem
rem  用法：  build-jar.bat                 正常构建（结束后停留）
rem          build-jar.bat --offline       直接用离线模式
rem          build-jar.bat clean build     透传任意 gradle 参数
rem          build-jar.bat --no-pause      构建完不停留（脚本/CI 调用）
rem ============================================================================
setlocal EnableExtensions
chcp 65001 >nul
pushd "%~dp0"

rem ------------------------------------------------------------ 解析参数
set "GRADLE_ARGS="
set "NOPAUSE="
:parse
if "%~1"=="" goto parsed
if /i "%~1"=="--no-pause" (set "NOPAUSE=1") else (set "GRADLE_ARGS=%GRADLE_ARGS% %1")
shift
goto parse
:parsed

rem ------------------------------------------------------------ 1. 选 JDK
set "JDK21="
set "JDK17="

call :scan_jdk "%JAVA_HOME%"
for /d %%d in ("%ProgramFiles%\Java\*")                       do call :scan_jdk "%%~d"
for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\*")           do call :scan_jdk "%%~d"
for /d %%d in ("%ProgramFiles%\Microsoft\*")                  do call :scan_jdk "%%~d"
for /d %%d in ("%ProgramFiles%\Zulu\*")                       do call :scan_jdk "%%~d"
for /d %%d in ("%ProgramFiles%\BellSoft\*")                   do call :scan_jdk "%%~d"
for /d %%d in ("%ProgramFiles%\Amazon Corretto\*")            do call :scan_jdk "%%~d"
for /d %%d in ("%ProgramFiles%\Semeru\*")                     do call :scan_jdk "%%~d"
for /d %%d in ("%ProgramFiles%\JetBrains\*")                  do call :scan_jdk "%%~d"
for /d %%d in ("%LocalAppData%\Programs\Eclipse Adoptium\*")  do call :scan_jdk "%%~d"
for /d %%d in ("%USERPROFILE%\.jdks\*")                       do call :scan_jdk "%%~d"
for /d %%d in ("%USERPROFILE%\.gradle\jdks\*")                do call :scan_jdk "%%~d"

echo ---------------------------------------------------------------
if defined JDK21 (
    set "JAVA_HOME=%JDK21%"
    echo   JDK 21  : %JDK21%
) else if defined JDK17 (
    set "JAVA_HOME=%JDK17%"
    echo   JDK 17  : %JDK17%
    echo   注意    : 没找到 JDK 21，Gradle 会用 foojay 自动下载 21 工具链
    echo             需要访问 api.foojay.io，首次构建会多花几分钟
    if exist "%USERPROFILE%\.gradle\jdks" del /q "%USERPROFILE%\.gradle\jdks\*.part" 2>nul
    if exist "%USERPROFILE%\.gradle\jdks" del /q "%USERPROFILE%\.gradle\jdks\*.lock" 2>nul
) else (
    echo   [失败] 没找到 JDK 17 或 21。
    echo          请安装 JDK 21 ^(Temurin^) 后重试：
    echo          https://adoptium.net/temurin/releases/?version=21
    goto :fail
)
echo   项目    : %CD%
if defined GRADLE_ARGS echo   额外参数: %GRADLE_ARGS%
echo ---------------------------------------------------------------
echo.

rem ------------------------------------------------------------ 2. 构建
echo [1/2] gradlew %GRADLE_ARGS% build
call gradlew.bat %GRADLE_ARGS% build
if errorlevel 1 (
    echo.
    echo [1/2] 构建失败 —— 改用 --offline 重试
    echo.
    call gradlew.bat %GRADLE_ARGS% build --offline
)
if errorlevel 1 goto :fail

rem ------------------------------------------------------------ 3. 产物
echo.
echo [2/2] 构建成功，产物：
set "FOUND="
for %%j in ("build\libs\*.jar") do (
    echo     %%~fj
    set "FOUND=1"
)
if not defined FOUND (
    echo     [警告] build\libs 下没有 jar 文件
    goto :fail
)
echo.
echo 把这个 jar 放进 .minecraft\mods 即可。
echo.
goto :done

rem ------------------------------------------------------------ 子过程
:scan_jdk
rem %~1 = 候选 JDK 目录；记第一个 >=21 的和第一个 >=17 的
rem 注意：不能写成 for /f ('"路径\java.exe" -version ...')，cmd 会把带引号的命令拆坏
if "%~1"=="" goto :eof
if not exist "%~1\bin\java.exe" goto :eof
set "JF=%TEMP%\_jdkver_%RANDOM%%RANDOM%.txt"
"%~1\bin\java.exe" -version >"%JF%" 2>&1
set "JVER="
for /f "tokens=3" %%v in ('type "%JF%" ^| findstr /i /c:" version "') do if not defined JVER set "JVER=%%~v"
del /q "%JF%" 2>nul
if not defined JVER goto :eof
set "JMAJOR="
for /f "tokens=1 delims=." %%m in ("%JVER%") do set "JMAJOR=%%m"
if not defined JMAJOR goto :eof
if %JMAJOR% GEQ 21 if not defined JDK21 set "JDK21=%~1"
if %JMAJOR% GEQ 17 if not defined JDK17 set "JDK17=%~1"
goto :eof

:done
echo ===============================================================
popd
if not defined NOPAUSE pause
endlocal
exit /b 0

:fail
echo.
echo ===============================================================
echo 构建失败，按顺序排查：
echo   1. 看上面 Gradle 的报错行
echo   2. JDK 版本不对 —— 装 JDK 21
echo   3. 依赖拉不到 —— build-jar.bat --offline
echo   4. 需要完整堆栈 —— build-jar.bat --stacktrace
echo ===============================================================
popd
if not defined NOPAUSE pause
endlocal
exit /b 1
