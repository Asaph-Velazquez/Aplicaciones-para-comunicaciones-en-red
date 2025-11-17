# Dockerfile para Node.js Bridge (UDP ↔ WebSocket)
FROM node:18-alpine

WORKDIR /app

# Copiar package.json y package-lock.json
COPY package*.json ./

# Instalar dependencias
RUN npm ci --only=production

# Copiar código del servidor
COPY server.js .

# Puerto WebSocket
EXPOSE 8080

# Puerto UDP (para escuchar clientes Java)
EXPOSE 5000/udp

# Comando de ejecución
CMD ["node", "server.js"]
