@echo off
REM Script para ejecutar el servidor HTTP en Windows

echo ========================================
echo   Iniciando Servidor HTTP Simple
echo ========================================
echo.

cd /d "%~dp0"

REM Compilar si es necesario
if not exist "target\classes\http_server\Main.class" (
    echo Compilando el proyecto...
    call mvn clean compile
    echo.
)

REM Ejecutar el servidor
echo Ejecutando servidor...
echo.
java -cp target\classes http_server.Main

pause
