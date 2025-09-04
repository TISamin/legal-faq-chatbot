# Step 1: Use an official JDK as build environment
FROM eclipse-temurin:21-jdk AS build

# Step 2: Set working directory inside the container
WORKDIR /app

# Step 3: Copy Maven wrapper and config
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Step 4: Give execution permission to mvnw
RUN chmod +x mvnw

# Step 5: Download dependencies (offline cache)
RUN ./mvnw dependency:go-offline -B

# Step 6: Copy the entire source code
COPY src src

# Step 7: Build the application JAR
RUN ./mvnw package -DskipTests

# Step 8: Use a lightweight JRE for running the app
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port (Render uses $PORT automatically)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java","-jar","app.jar"]
