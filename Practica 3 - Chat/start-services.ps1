#!/usr/bin/env pwsh
<#
.SYNOPSIS
Script para abrir automáticamente todas las terminales necesarias

.DESCRIPTION
Abre 3 terminales de PowerShell en paralelo:
1. Puente Node.js
2. Servidor UDP Java
3. Frontend Angular
#>

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "
╔═══════════════════════════════════════════════════════════════╗
║   🚀 INICIANDO SERVICIOS EN PARALELO                         ║
╚═══════════════════════════════════════════════════════════════╝
" -ForegroundColor Green

# Terminal 1: Puente Node.js
Write-Host "📡 Abriendo Terminal 1: Puente Node.js..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot'; Write-Host 'Terminal 1: Puente Node.js' -ForegroundColor Green; Write-Host 'Ejecutando: node server.js' -ForegroundColor Yellow; node server.js"

Start-Sleep -Seconds 2

# Terminal 2: Servidor UDP Java
Write-Host "☕ Abriendo Terminal 2: Servidor UDP Java..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot\Chat Grupal\src\main\java'; Write-Host 'Terminal 2: Servidor UDP Java' -ForegroundColor Green; Write-Host 'Ejecutando: java ChatGrupal.demo.ChatServer' -ForegroundColor Yellow; java ChatGrupal.demo.ChatServer"

Start-Sleep -Seconds 2

# Terminal 3: Frontend Angular
Write-Host "🅰️  Abriendo Terminal 3: Frontend Angular..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot\FrontEnd'; Write-Host 'Terminal 3: Frontend Angular' -ForegroundColor Green; Write-Host 'Instalando dependencias...' -ForegroundColor Yellow; npm install; Write-Host 'Ejecutando: npm start' -ForegroundColor Yellow; npm start"

Write-Host "
✅ Servicios iniciados en paralelo:

📡 Terminal 1: Puente Node.js
   └─ Puerto UDP: 5000
   └─ Puerto WebSocket: 8080

☕ Terminal 2: Servidor UDP Java
   └─ Puerto: 5000

🅰️  Terminal 3: Frontend Angular
   └─ URL: http://localhost:4200

⏳ Espera ~30 segundos a que todos estén listos...
" -ForegroundColor Green

# Esperar a que los servicios estén listos
Start-Sleep -Seconds 5

# Abrir navegador
Write-Host "🌐 Abriendo navegador..." -ForegroundColor Cyan
Start-Sleep -Seconds 5
Start-Process "http://localhost:4200"

Write-Host "
═════════════════════════════════════════════════════════════════

✅ SISTEMA COMPLETAMENTE DESPLEGADO

Servicios activos:
  ✅ Puente UDP ↔ WebSocket: http://localhost:8080
  ✅ Frontend Angular: http://localhost:4200
  ✅ Servidor UDP Java: Escuchando en puerto 5000

Para crear un cliente UDP de prueba, abre otra terminal y ejecuta:
  cd 'Chat Grupal\src\main\java'
  java ChatGrupal.demo.ChatClient

═════════════════════════════════════════════════════════════════
" -ForegroundColor Green
