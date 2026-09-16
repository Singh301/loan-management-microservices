FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY discovery-server/target/*.jar app.jar
RUN chown app:app /app/app.jar
USER app
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
