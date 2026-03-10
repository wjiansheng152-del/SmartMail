FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY tracking-service/target/tracking-service-*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
