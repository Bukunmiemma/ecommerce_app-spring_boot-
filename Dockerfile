# Step 1: Use an official Java runtime
FROM eclipse-temurin:25-jdk

# Step 2: Set working directory inside container
WORKDIR /app

# Step 3: Copy your jar file into the container
COPY target/*.jar app.jar

# Step 4: Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]