FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY campaign-service/target/campaign-service-*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
