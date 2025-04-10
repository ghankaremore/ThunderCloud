FROM openjdk:21-jdk-slim
WORKDIR /app
COPY demofinalfinal/target/*.jar app.jar
EXPOSE 4200
ENTRYPOINT ["java", "-jar", "app.jar"]
