FROM eclipse-temurin:21-jre-alpine@sha256:974b08960c5d96694c780e65b2d5705268ab1e1ca1a0dd0caf4ba6c3fe34d699
WORKDIR /app
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app app && mkdir -p /app/uploads && chown -R app:app /app
COPY --chown=app:app document-service/target/*.jar app.jar
USER app
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
