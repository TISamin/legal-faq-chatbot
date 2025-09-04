# ===============================
# Stage 1: Build the application
# ===============================
FROM eclipse-temurin:21-jdk AS build

# Set working directory
WORKDIR /app

# Copy Maven wrapper and config
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable (fix for permission denied)
RUN chmod +x mvnw

# Download dependencies (caches them)
RUN ./mvnw dependency:go-offline

# Copy the source code
COPY src src

# Build the application (skip tests for faster build)
RUN ./mvnw clean package -DskipTests


# ===============================
# Stage 2: Run the application
# ===============================
FROM eclipse-temurin:21-jre AS run

# Set working directory
WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (Render will map this automatically)
EXPOSE 8080

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
