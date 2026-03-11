FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# 从仓库根目录构建。构建前须先打 JAR：.\mvnw.cmd package -pl gateway-service -am -DskipTests
# 然后：docker build -f gateway-service/Dockerfile . 或 docker-compose build gateway
COPY gateway-service/target/gateway-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
