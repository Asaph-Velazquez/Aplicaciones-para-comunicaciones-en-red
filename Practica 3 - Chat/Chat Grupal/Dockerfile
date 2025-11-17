# Dockerfile para Backend Java (ChatServer)
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copiar archivos de proyecto
COPY pom.xml .
COPY src ./src

# Compilar con Maven
RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copiar JAR compilado
COPY --from=builder /app/target/*.jar app.jar

# Puerto UDP (exponer para que Docker lo sepa)
EXPOSE 5000/udp

# Comando de ejecución
ENTRYPOINT ["java", "-jar", "app.jar"]
