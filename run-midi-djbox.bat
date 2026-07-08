@echo off
setlocal

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto run

for /d %%J in ("%USERPROFILE%\.jdks\temurin-*") do (
    if exist "%%~fJ\bin\java.exe" (
        set "JAVA_HOME=%%~fJ"
        goto run
    )
)

for /d %%J in ("%USERPROFILE%\.jdks\openjdk-*") do (
    if exist "%%~fJ\bin\java.exe" (
        set "JAVA_HOME=%%~fJ"
        goto run
    )
)

for /d %%J in ("C:\Program Files\Eclipse Adoptium\jdk-*") do (
    if exist "%%~fJ\bin\java.exe" (
        set "JAVA_HOME=%%~fJ"
        goto run
    )
)

echo No JDK was found. Install JDK 21 or newer, or set JAVA_HOME.
exit /b 1

:run
set "PATH=%JAVA_HOME%\bin;%PATH%"
if "%~1"=="" (
    call "%~dp0gradlew.bat" run
) else (
    call "%~dp0gradlew.bat" %*
)
