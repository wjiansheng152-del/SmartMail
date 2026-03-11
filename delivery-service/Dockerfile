# 使用本地已编译的 JAR（构建前请在项目根目录执行：.\mvnw.cmd package -pl delivery-service -am）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY delivery-service/target/delivery-service-*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
