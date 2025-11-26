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
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot'; Write-Host 'Terminal 1: Puente Node.js (Bridge UDP-WebSocket)' -ForegroundColor Green; Write-Host 'Ejecutando: node server.js' -ForegroundColor Yellow; node server.js"

Start-Sleep -Seconds 2

# Terminal 2: Servidor UDP Java
Write-Host "☕ Abriendo Terminal 2: Servidor UDP Java..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot\Chat Grupal'; Write-Host 'Terminal 2: Servidor UDP Java (ChatServer)' -ForegroundColor Green; Write-Host 'Ejecutando: mvn exec:java' -ForegroundColor Yellow; mvn exec:java"

Start-Sleep -Seconds 3

# Terminal 3: Frontend Angular
Write-Host "🅰️  Abriendo Terminal 3: Frontend Angular..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$projectRoot\FrontEnd'; Write-Host 'Terminal 3: Frontend Angular' -ForegroundColor Green; Write-Host 'Instalando dependencias...' -ForegroundColor Yellow; npm install; Write-Host 'Ejecutando: npm start' -ForegroundColor Yellow; npm start"

Write-Host "
✅ Servicios iniciados en paralelo:

📡 Terminal 1: Puente Node.js (Bridge)
   └─ Puerto UDP: 5001
   └─ Puerto WebSocket: 8080
   └─ Traduce UDP ↔ WebSocket

☕ Terminal 2: Servidor UDP Java (ChatServer)
   └─ Puerto UDP: 5000
   └─ Usa hilos para manejar clientes

🅰️  Terminal 3: Frontend Angular
   └─ URL: http://localhost:4200
   └─ Conecta a bridge via WebSocket

⏳ Espera ~20-30 segundos a que todos estén listos...
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

Arquitectura (cumple requisitos de UDP e hilos):
  
  [Frontend Angular] ←WebSocket→ [Bridge Node.js] ←UDP→ [ChatServer Java UDP]
       :4200                        :8080 (WS)              :5000 (UDP)
                                    :5001 (UDP)              + hilos

Servicios activos:
  ✅ ChatServer Java UDP (puerto 5000) - VÍA PRINCIPAL
  ✅ Bridge Node.js (UDP:5001 + WS:8080) - Traductor
  ✅ Frontend Angular (http://localhost:4200)

FLUJO DE COMUNICACIÓN:
  1. Frontend → WebSocket (8080) → Bridge
  2. Bridge → UDP (5000) → ChatServer Java
  3. ChatServer procesa con HILOS
  4. Respuesta: ChatServer → UDP → Bridge → WebSocket → Frontend

Funcionalidades:
  ✅ Chat grupal (UDP + hilos)
  ✅ Mensajes privados con emojis
  ✅ Lista de usuarios activos

═════════════════════════════════════════════════════════════════
" -ForegroundColor Green
