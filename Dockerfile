# Stage 1: Build Stage
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy all files
COPY . .

# Copy frontend files to backend static resources
# This allows Spring Boot to serve the frontend automatically
RUN mkdir -p backend/src/main/resources/static && \
    cp -r frontend/* backend/src/main/resources/static/

# Build the Spring Boot application
WORKDIR /app/backend
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

# Stage 2: Run Stage
FROM eclipse-temurin:21-jre

WORKDIR /app

# Install Python 3 (required for data collection scripts if enabled)
RUN apt-get update && apt-get install -y python3 && rm -rf /var/lib/apt/lists/*

# Copy only the built JAR from the builder stage
COPY --from=builder /app/backend/build/libs/*.jar app.jar

# Render provides the PORT environment variable and Spring reads it from application.yml.
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Dfile.encoding=UTF-8", "-jar", "app.jar"]
