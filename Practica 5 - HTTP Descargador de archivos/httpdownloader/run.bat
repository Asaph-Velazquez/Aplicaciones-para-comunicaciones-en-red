@echo off
REM Script para ejecutar el Servidor HTTP
REM Práctica 5 - HTTP Descargador de Archivos

echo.
echo ========================================
echo   Iniciando Servidor HTTP - Java SE
echo ========================================
echo.

REM Verificar que el proyecto está compilado
if not exist "target\classes\backend\Main.class" (
    echo [ERROR] El proyecto no esta compilado.
    echo Por favor, ejecuta primero: mvn clean compile
    echo.
    pause
    exit /b 1
)

REM Ejecutar el servidor
echo [INFO] Ejecutando servidor en puerto 8080...
echo [INFO] Accede a: http://localhost:8080
echo.
echo Presiona Ctrl+C para detener el servidor
echo.

java -cp target\classes backend.Main

pause
