# ==========================================
# Multi-Stage Dockerfile for SmartPark
# ==========================================

# Stage 1: Build the Application
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace

# Copy pom.xml and cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production jar
COPY src src
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime Image with Minimal JRE
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create a non-root dedicated application user
RUN groupadd -r smartpark && useradd -r -g smartpark smartpark

# Copy compiled artifact from builder stage
COPY --from=builder /workspace/target/*.jar app.jar

# Adjust ownership
RUN chown -R smartpark:smartpark /app
USER smartpark

# Configuration environment variables with sane defaults
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

# Run Spring Boot application honoring dynamic $PORT for Render/Railway/Cloud
ENTRYPOINT ["sh", "-c", "java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT} -jar app.jar"]
