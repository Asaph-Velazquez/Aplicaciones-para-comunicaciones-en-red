# Dockerfile para Angular Frontend
FROM node:18-alpine AS builder

WORKDIR /app

# Copiar package.json
COPY package.json package-lock.json ./

# Instalar dependencias
RUN npm ci

# Copiar código fuente
COPY . .

# Compilar para producción
RUN npm run build

# Etapa de producción (Nginx)
FROM nginx:alpine

# Copiar configuración de Nginx
COPY nginx.conf /etc/nginx/nginx.conf

# Copiar build a Nginx
COPY --from=builder /app/dist/chat-grupal/browser /usr/share/nginx/html

# Puerto HTTP
EXPOSE 80

# Comando de ejecución
CMD ["nginx", "-g", "daemon off;"]
