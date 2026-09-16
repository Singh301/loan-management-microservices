FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY audit-service/target/*.jar app.jar
RUN chown app:app /app/app.jar
USER app
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
