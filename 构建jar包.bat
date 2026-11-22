@echo off
rem ============================================================================
rem  Build the jar - Northstar Planet Addon (NeoForge 1.21.1 / Gradle 9.2.1)
rem
rem  Double-click to run. Steps:
rem    1. Find JDK 21 (fall back to 17) and set JAVA_HOME. Without JAVA_HOME the
rem       Gradle wrapper fails with "-classpath requires class path specification".
rem       The version is read from <jdk>\release, so no temp files and no tricky
rem       quoting (the old java -version parsing was the source of the
rem       "'_jdk' is not recognized" errors).
rem    2. Run "gradlew build"; retry up to 3 times because the local proxy
rem       occasionally drops the TLS handshake, then fall back to --offline.
rem       Java ignores the Windows system proxy, so the proxy must be configured
rem       in %USERPROFILE%\.gradle\gradle.properties.
rem    3. Print the produced jar.
rem
rem  Usage:  build-jar.bat                normal build (stays open at the end)
rem          build-jar.bat --offline      offline mode straight away
rem          build-jar.bat clean build    pass any extra gradle tasks/args
rem          build-jar.bat --no-pause     do not pause (for scripts/CI)
rem
rem  Keep this file ASCII-only: cmd.exe mis-parses non-ASCII bytes in .bat
rem  files (it splits lines at random byte offsets), which breaks the script.
rem ============================================================================
setlocal EnableExtensions EnableDelayedExpansion
pushd "%~dp0"

rem ---------------------------------------------------------------- arguments
set "GRADLE_ARGS="
set "NOPAUSE="
set "TASKS_GIVEN="
:parse
if "%~1"=="" goto parsed
if /i "%~1"=="--no-pause" (
    set "NOPAUSE=1"
) else (
    set "GRADLE_ARGS=!GRADLE_ARGS! %~1"
    set "ARG=%~1"
    if not "!ARG:~0,1!"=="-" set "TASKS_GIVEN=1"
)
shift
goto parse
:parsed
rem Only add the default "build" task when the caller passed options but no task.
if not defined TASKS_GIVEN set "GRADLE_ARGS=!GRADLE_ARGS! build"

rem ---------------------------------------------------------------- 1. pick JDK
set "JDK21="
set "JDK17="

call :scan_jdk "%JAVA_HOME%"
for /d %%d in ("%ProgramFiles%\Java\*")                      do call :scan_jdk "%%~d"
for /d %%d in ("%ProgramFiles%\Eclipse Adoptium\*")          do call :scan_jdk "%%~d"
for /d %%d in ("%ProgramFiles%\Microsoft\*")                 do call :scan_jdk "%%~d"
for /d %%d in ("%ProgramFiles%\Zulu\*")                      do call :scan_jdk "%%~d"
for /d %%d in ("%ProgramFiles%\BellSoft\*")                  do call :scan_jdk "%%~d"
for /d %%d in ("%ProgramFiles%\Amazon Corretto\*")           do call :scan_jdk "%%~d"
for /d %%d in ("%ProgramFiles%\Semeru\*")                    do call :scan_jdk "%%~d"
for /d %%d in ("%ProgramFiles%\JetBrains\*")                 do call :scan_jdk "%%~d"
for /d %%d in ("%LocalAppData%\Programs\Eclipse Adoptium\*") do call :scan_jdk "%%~d"
for /d %%d in ("%USERPROFILE%\.jdks\*")                      do call :scan_jdk "%%~d"
for /d %%d in ("%USERPROFILE%\.gradle\jdks\*")               do call :scan_jdk "%%~d"

echo ---------------------------------------------------------------
if defined JDK21 (
    set "JAVA_HOME=!JDK21!"
    echo   JDK 21 : !JDK21!
) else if defined JDK17 (
    set "JAVA_HOME=!JDK17!"
    echo   JDK 17 : !JDK17!
    echo   NOTE   : no JDK 21 found; Gradle will provision the 21 toolchain
) else (
    echo   [FAIL] No JDK 17 or 21 found.
    echo          Install Temurin 21: https://adoptium.net/temurin/releases/?version=21
    goto :fail
)
echo   Project: %CD%
if defined GRADLE_ARGS echo   Args   : !GRADLE_ARGS!
echo ---------------------------------------------------------------
echo.

rem ---------------------------------------------------------------- 2. build
set "ATTEMPT=1"
:attempt
echo [1/2] Attempt !ATTEMPT!/3: gradlew.bat !GRADLE_ARGS!
call gradlew.bat !GRADLE_ARGS!
if not errorlevel 1 goto built
echo.
echo       Build failed, retrying (network/proxy drops are usually transient).
set /a ATTEMPT+=1
if !ATTEMPT! LEQ 3 (
    timeout /t 5 /nobreak >nul
    goto attempt
)
echo [1/2] Three online attempts failed - retrying with --offline
echo       (only works when the Gradle cache already has every artifact)
call gradlew.bat !GRADLE_ARGS! --offline
if errorlevel 1 goto :fail

:built
rem ---------------------------------------------------------------- 3. output
echo.
echo [2/2] Build succeeded, artifacts:
set "FOUND="
for %%j in ("build\libs\*.jar") do (
    echo     %%~fj
    set "FOUND=1"
)
if not defined FOUND (
    echo     [WARN] no jar in build\libs
    goto :fail
)
echo.
echo Drop that jar into .minecraft\mods.
echo.
goto :done

rem ---------------------------------------------------------------- sub: jdk scan
:scan_jdk
rem %~1 = candidate JDK directory. Its "release" file has JAVA_VERSION="21.0.12"
if "%~1"=="" goto :eof
if not exist "%~1\bin\java.exe" goto :eof
if not exist "%~1\release" goto :eof
set "JVER="
set "JMAJOR="
for /f "usebackq tokens=2 delims==" %%v in (`findstr /b /c:"JAVA_VERSION=" "%~1\release" 2^>nul`) do set "JVER=%%~v"
if not defined JVER goto :eof
for /f "tokens=1 delims=." %%m in ("!JVER!") do set "JMAJOR=%%m"
if not defined JMAJOR goto :eof
if !JMAJOR! GEQ 21 if not defined JDK21 set "JDK21=%~1"
if !JMAJOR! GEQ 17 if not defined JDK17 set "JDK17=%~1"
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
echo Build failed. Check, in this order:
echo   1. the Gradle error lines above
echo   2. network errors (handshake / timeout): make sure the local proxy is
echo      running and that %USERPROFILE%\.gradle\gradle.properties has
echo      systemProp.https.proxyHost / systemProp.https.proxyPort
echo   3. JDK: install Temurin 21, or launch Gradle with 17 and let the
echo      21 toolchain be used for compiling
echo   4. dependencies already cached: build-jar.bat --offline
echo   5. full stack trace: build-jar.bat --stacktrace
echo ===============================================================
popd
if not defined NOPAUSE pause
endlocal
exit /b 1
