# Docker - Guía de Despliegue

## Descripción

El proyecto está containerizado con Docker Compose para ejecutar todos los servicios en contenedores:

| Contenedor | Servicio | Puerto | Tecnología |
|-----------|----------|--------|-----------|
| **backend** | Servidor UDP | 5000/udp | Java 17 |
| **bridge** | Puente UDP↔WebSocket | 8080 | Node.js 18 |
| **frontend** | Interfaz Web | 80 | Angular + Nginx |

---

## Requisitos

- **Docker Desktop** instalado (incluye Docker Engine + Docker Compose)
  - [Descargar Docker Desktop](https://www.docker.com/products/docker-desktop)
- **Verificar instalación:**
  ```bash
  docker --version
  docker-compose --version
  ```

---

## Iniciar Todos los Servicios

### Opción 1: Docker Compose (Recomendado)

```bash
# Navegar al directorio raíz del proyecto
cd "C:\Users\[usuario]\...\Practica 3 - Chat"

# Construir imágenes y levantar contenedores
docker-compose up --build

# En otra terminal, verificar estado
docker-compose ps
```

**Salida esperada:**
```
NAME              COMMAND                 STATUS      PORTS
chat-backend      java -jar app.jar       Up 2 min    5000/udp
chat-bridge       node server.js          Up 2 min    0.0.0.0:8080->8080/tcp, 5000/udp
chat-frontend     nginx -g daemon off;    Up 1 min    0.0.0.0:80->80/tcp
```

### Opción 2: Construir Imágenes Manualmente

```bash
# Backend
cd "Chat Grupal"
docker build -t chat-backend:latest .

# Bridge (raíz)
cd ..
docker build -t chat-bridge:latest .

# Frontend
cd FrontEnd
docker build -t chat-frontend:latest .
```

---

## Acceder a los Servicios

### Frontend (Navegador)
```
http://localhost
```

### Bridge WebSocket
```
ws://localhost:8080
```

### Backend UDP
```
localhost:5000/udp
```

---

## Comandos Útiles

### Ver logs de un contenedor

```bash
# Bridge logs
docker-compose logs -f bridge

# Backend logs
docker-compose logs -f backend

# Frontend logs
docker-compose logs -f frontend

# Todos los logs
docker-compose logs -f
```

### Entrar a un contenedor

```bash
# Shell del Bridge
docker-compose exec bridge sh

# Shell del Frontend
docker-compose exec frontend sh

# Shell del Backend
docker-compose exec backend bash
```

### Detener servicios

```bash
# Detener (sin eliminar)
docker-compose stop

# Reiniciar
docker-compose restart

# Detener y eliminar contenedores
docker-compose down

# Detener, eliminar e limpiar volúmenes
docker-compose down -v
```

### Construir sin cachè

```bash
docker-compose build --no-cache
```

---

## Arquitectura en Docker

```
┌────────────────────────────────────────────────────────────┐
│                   Docker Network                           │
│                   (chat-network)                           │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │   Backend    │  │    Bridge    │  │  Frontend    │    │
│  │   (Java)     │  │  (Node.js)   │  │  (Angular)   │    │
│  │              │  │              │  │              │    │
│  │ :5000/udp    │  │ :8080        │  │ :80          │    │
│  └──────────────┘  └──────────────┘  └──────────────┘    │
│        │                   │                   │          │
└────────┼───────────────────┼───────────────────┼──────────┘
         │                   │                   │
         └───────────────────────────────────────┘
                Host Ports (acceso externo)
```

---

## Troubleshooting

### Puerto ya en uso

```bash
# Encontrar qué usa el puerto 8080
netstat -ano | findstr :8080

# En Windows PowerShell
Get-NetTCPConnection -LocalPort 8080 | select OwningProcess
Get-Process -Id [PID]

# Cambiar puertos en docker-compose.yaml si es necesario
```

### Error: "Cannot connect to Docker daemon"

```bash
# Asegúrate de que Docker Desktop está corriendo
# En Windows: Abre Docker Desktop desde el menú de inicio
```

### Contenedor sale inmediatamente

```bash
# Ver logs del error
docker-compose logs backend
docker-compose logs bridge
docker-compose logs frontend

# Reconstruir sin cachè
docker-compose build --no-cache
```

### WebSocket no conecta desde Frontend

1. Verificar que el Bridge está corriendo:
   ```bash
   docker-compose ps
   ```

2. Verificar logs del Bridge:
   ```bash
   docker-compose logs bridge
   ```

3. Verificar URL en `chat.service.ts`:
   ```typescript
   private readonly SERVER_URL = 'ws://localhost:8080';
   ```

---

## Configuración de Producción

### Variables de Entorno

Crear archivo `.env`:
```env
NODE_ENV=production
BACKEND_HOST=backend
BACKEND_PORT=5000
WS_PORT=8080
JAVA_OPTS=-Xmx1g -Xms512m
```

Usar en `docker-compose.yaml`:
```yaml
environment:
  - JAVA_OPTS=${JAVA_OPTS}
  - NODE_ENV=${NODE_ENV}
```

### Registry Privado

```bash
# Taggear imagen
docker tag chat-backend:latest my-registry/chat-backend:latest

# Pushear
docker push my-registry/chat-backend:latest
```

---

## Monitoreo

### Ver uso de recursos

```bash
docker stats
```

### Inspeccionar contenedor

```bash
docker-compose inspect backend
```

---

## Limpieza

```bash
# Eliminar contenedores detenidos
docker container prune

# Eliminar imágenes no usadas
docker image prune

# Eliminar todo (contenedores, imágenes, volúmenes)
docker system prune -a --volumes
```

---

**Resumen:** Con Docker Compose, todo está containerizado y listo para deployment con `docker-compose up --build`.