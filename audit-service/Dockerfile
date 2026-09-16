FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY audit-service/target/*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
