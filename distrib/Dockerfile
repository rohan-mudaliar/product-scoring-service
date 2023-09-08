# Use the official OpenJDK base image
FROM openjdk:11-jre-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from your build into the container at /app
COPY build/libs/ScoringService-0.0.1-SNAPSHOT.jar /app/app.jar

# Expose the port that the application will run on
EXPOSE 8080

# Define environment variables (optional)

# Run the application when the container starts
CMD ["java", "-jar", "app.jar"]
