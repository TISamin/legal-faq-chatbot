# Use official OpenJDK image
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy Maven wrapper and project files
COPY . .

# Build the app
RUN ./mvnw clean package -DskipTests

# Run the JAR
CMD ["java", "-jar", "target/*.jar"]
