@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT=%~dp0"

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" goto findJavaFx

for /d %%J in ("%USERPROFILE%\.jdks\temurin-*") do (
    if exist "%%~fJ\bin\javac.exe" (
        set "JAVA_HOME=%%~fJ"
        goto findJavaFx
    )
)

for /d %%J in ("%USERPROFILE%\.jdks\openjdk-*") do (
    if exist "%%~fJ\bin\javac.exe" (
        set "JAVA_HOME=%%~fJ"
        goto findJavaFx
    )
)

for /d %%J in ("C:\Program Files\Eclipse Adoptium\jdk-*") do (
    if exist "%%~fJ\bin\javac.exe" (
        set "JAVA_HOME=%%~fJ"
        goto findJavaFx
    )
)

echo No JDK was found. Install JDK 21 or newer, or set JAVA_HOME.
exit /b 1

:findJavaFx
set "JAVAC=%JAVA_HOME%\bin\javac.exe"
set "JAVA=%JAVA_HOME%\bin\java.exe"

if defined JAVAFX_HOME (
    if exist "%JAVAFX_HOME%\lib\javafx-controls.jar" set "FX_MODULE_PATH=%JAVAFX_HOME%\lib"
    if exist "%JAVAFX_HOME%\javafx-controls.jar" set "FX_MODULE_PATH=%JAVAFX_HOME%"
)

if not defined FX_MODULE_PATH if defined PATH_TO_FX (
    if exist "%PATH_TO_FX%\javafx-controls.jar" set "FX_MODULE_PATH=%PATH_TO_FX%"
)

if defined FX_MODULE_PATH goto compile

set "OPENJFX_CACHE=%USERPROFILE%\.gradle\caches\modules-2\files-2.1\org.openjfx"

for /f "delims=" %%J in ('dir /b /s "%OPENJFX_CACHE%\javafx-base\javafx-base-*-win.jar" 2^>nul') do if not defined FX_BASE set "FX_BASE=%%J"
for /f "delims=" %%J in ('dir /b /s "%OPENJFX_CACHE%\javafx-graphics\javafx-graphics-*-win.jar" 2^>nul') do if not defined FX_GRAPHICS set "FX_GRAPHICS=%%J"
for /f "delims=" %%J in ('dir /b /s "%OPENJFX_CACHE%\javafx-controls\javafx-controls-*-win.jar" 2^>nul') do if not defined FX_CONTROLS set "FX_CONTROLS=%%J"

if defined FX_BASE if defined FX_GRAPHICS if defined FX_CONTROLS (
    set "FX_MODULE_PATH=%FX_BASE%;%FX_GRAPHICS%;%FX_CONTROLS%"
)

if not defined FX_MODULE_PATH (
    echo JavaFX was not found.
    echo Set JAVAFX_HOME to a JavaFX SDK folder, or run Gradle once so the OpenJFX jars are downloaded.
    exit /b 1
)

:compile
if not exist "%ROOT%out\classes" mkdir "%ROOT%out\classes"

"%JAVAC%" ^
    --module-path "%FX_MODULE_PATH%" ^
    --add-modules javafx.controls,javafx.graphics ^
    -d "%ROOT%out\classes" ^
    "%ROOT%src\main\java\io\github\rohanpurohit7\mididj\MidiDjBoxFxApp.java"

if errorlevel 1 exit /b 1

if /I "%~1"=="--compile-only" exit /b 0

"%JAVA%" ^
    --module-path "%FX_MODULE_PATH%" ^
    --add-modules javafx.controls,javafx.graphics ^
    -cp "%ROOT%out\classes;%ROOT%src\main\resources" ^
    io.github.rohanpurohit7.mididj.MidiDjBoxFxApp
