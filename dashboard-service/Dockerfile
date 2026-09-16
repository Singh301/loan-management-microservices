FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY dashboard-service/target/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
