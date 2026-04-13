FROM eclipse-temurin:21-jre-alpine

EXPOSE 8080

WORKDIR /app

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]