FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# 从仓库根目录构建: docker build -f gateway-service/Dockerfile .
COPY gateway-service/target/gateway-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
