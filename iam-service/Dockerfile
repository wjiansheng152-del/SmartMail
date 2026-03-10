FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY iam-service/target/iam-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
