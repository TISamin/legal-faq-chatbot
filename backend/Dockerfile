# Step 1: Use an official JDK base image
FROM eclipse-temurin:21-jdk AS build

# Step 2: Set the working directory
WORKDIR /app

# Step 3: Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Step 4: Download dependencies
RUN ./mvnw dependency:go-offline

# Step 5: Copy source code
COPY src src

# Step 6: Build the application JAR
RUN ./mvnw clean package -DskipTests

# Step 7: Use a smaller JDK image for runtime
FROM eclipse-temurin:21-jdk AS run
WORKDIR /app

# Copy only the built JAR
COPY --from=build /app/target/*.jar app.jar

# Expose the port Render will use
EXPOSE 8080

# Start the app
CMD ["java", "-jar", "app.jar"]
