@echo off
rem ============================================================================
rem  Nephele asset pipeline - one click AFTER you saved the .bbmodel in Blockbench
rem
rem  Why a script: this pipeline has a fixed order and skipping/reordering a step
rem  silently ships stale assets. Two bugs already came from that:
rem    * animations exported while reading the wrong key -> every frame was zero
rem    * textures exported BEFORE the last UV pack -> in-game UVs sampled garbage
rem
rem  Blockbench side (must be done in the app / via MCP first):
rem    1. geometry edits
rem    2. pack_uv
rem    3. detail_cubes (main texture + glowmask)
rem    4. export_model codec=bedrock  -> assets/.../geo/entity/nephele.geo.json
rem    5. save_project                -> build/scratch/models/nephele.bbmodel
rem
rem  Then run this script. It handles the rest:
rem    * rebuild_nephele_anim.py   rewrite tail/fin/body animation curves
rem    * bbmodel_to_geckolib.py    .bbmodel -> animation.json + texture PNGs
rem    * verify_nephele_anim.py    measure real travel in blocks and print a table
rem
rem  Usage:  export_nephele.bat            (stays open)
rem          export_nephele.bat --no-pause
rem ============================================================================
setlocal EnableExtensions
pushd "%~dp0.."

set "PY=%PYTHON%"
if not defined PY (
    where py >nul 2>nul && set "PY=py -3"
)
if not defined PY (
    if exist "%USERPROFILE%\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe" (
        set "PY=%USERPROFILE%\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe"
    )
)
if not defined PY (
    echo [FAIL] no python found. Set PYTHON=^<path to python.exe^> and retry.
    goto :fail
)

set "MODEL=build\scratch\models\nephele.bbmodel"
set "GEO=src\main\resources\assets\northstar_planet_addon\geo\entity\nephele.geo.json"
set "ANIM=src\main\resources\assets\northstar_planet_addon\animations\entity\nephele.animation.json"
set "TEX=src\main\resources\assets\northstar_planet_addon\textures\entity\nephele.png"

for %%f in ("%MODEL%" "%GEO%" "%ANIM%") do (
    if not exist "%%~f" (
        echo [FAIL] missing %%~f
        echo        Did you run the 5 Blockbench steps listed at the top of this file?
        goto :fail
    )
)
if not exist "%TEX%" (
    echo [WARN] %TEX% not found yet - it will be created by bbmodel_to_geckolib.py
)

echo.
echo [1/3] rewriting animation curves from the .bbmodel ...
%PY% tools\rebuild_nephele_anim.py
if errorlevel 1 goto :fail

echo.
echo [2/3] extracting animation.json + texture PNGs ...
%PY% tools\bbmodel_to_geckolib.py
if errorlevel 1 goto :fail

echo.
echo [3/3] measuring real travel (blocks) ...
%PY% tools\verify_nephele_anim.py

echo.
echo ===============================================================
echo  Done. Now rebuild the jar:
echo      build-jar.bat        (or: gradlew.bat build)
echo  Check: tail stroke should be ^>= 4 blocks in idle, fins ^>= 1.2 blocks.
echo ===============================================================
popd
if /i not "%~1"=="--no-pause" pause
exit /b 0

:fail
echo.
echo ===============================================================
echo  Pipeline failed - nothing was built. See the error above.
echo ===============================================================
popd
if /i not "%~1"=="--no-pause" pause
exit /b 1
