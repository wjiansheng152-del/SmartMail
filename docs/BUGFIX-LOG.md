# SmartMail 问题修复记录

本文档记录开发与联调过程中发现并修复的问题，便于后续排查与发布说明。

---

## 1. Docker Compose：`docker-compose` 命令不存在

**现象**：PowerShell 下执行 `docker-compose build` 报错「无法将 docker-compose 项识别为 cmdlet…」。

**原因**：新版本 Docker Desktop 仅提供 Compose V2，命令为 `docker compose`（空格），不再提供独立的 `docker-compose` 可执行文件。

**修复**：统一使用 `docker compose`（中间为空格），例如：
- `docker compose build`
- `docker compose up -d`

---

## 2. 各业务服务启动失败：No qualifying bean of type '...Mapper' available

**现象**：contact-service、template-service、campaign-service、tracking-service、audit-service 启动时报：
- `No MyBatis mapper was found in '[com.smartmail.xxx]' package`
- `NoSuchBeanDefinitionException: No qualifying bean of type 'com.smartmail.xxx.mapper.XXXMapper' available`

**原因**：
- template / campaign / tracking / audit 四个服务的启动类缺少 `@MapperScan`，MyBatis 未扫描 Mapper 接口；
- contact-service 虽有 `@MapperScan`，在排除 DataSource 自动配置、打 fat JAR 时扫描仍可能失效。

**修复**：
1. **template、campaign、tracking、audit**：在各自 `*Application.java` 中增加：
   - `import org.mybatis.spring.annotation.MapperScan;`
   - `@MapperScan("com.smartmail.<模块>.mapper")`
2. **contact-service**：新增 `MybatisPlusConfig.java`，显式注册 `MapperScannerConfigurer`，扫描 `com.smartmail.contact.mapper`，并指定 `sqlSessionFactoryBeanName="sqlSessionFactory"`；同时从 `ContactApplication` 上移除 `@MapperScan`，避免冲突。配置类使用 `@Configuration(proxyBeanMethods = false)` 且 `@Bean` 方法为 `public static`，避免 BeanDefinitionRegistryPostProcessor 过早创建导致的 WARN。

**涉及文件**：
- `template-service/.../TemplateApplication.java`
- `campaign-service/.../CampaignApplication.java`
- `tracking-service/.../TrackingApplication.java`
- `audit-service/.../AuditApplication.java`
- `contact-service/.../config/MybatisPlusConfig.java`（新建）
- `contact-service/.../ContactApplication.java`（移除 @MapperScan）

---

## 3. PowerShell 下 curl 调用 API 报错（Headers 参数类型错误）

**现象**：在 PowerShell 中执行 `curl -X GET ... -H "Authorization: Bearer ..."` 报错「无法将…String 类型的 Authorization… 转换为 System.Collections.IDictionary 类型」。

**原因**：PowerShell 中 `curl` 是 `Invoke-WebRequest` 的别名，参数语义与 Linux curl 不同，`-H` 不被支持。

**修复**：使用 Windows 自带的 `curl.exe` 调用，例如：
```powershell
curl.exe -X GET "http://localhost:8080/api/contact/contact/page?page=1&size=20" -H "Authorization: Bearer <token>" -H "Content-Type: application/json"
```

---

## 4. MySQL 建表脚本通过管道执行时中文乱码导致语法错误

**现象**：`Get-Content .\docs\sql\tenant_default-template.sql -Raw | docker exec -i smartmail-mysql-1 mysql -uroot -proot` 执行报错：
- `ERROR 1064 (42000): You have an error in your SQL syntax... near '??????'`

**原因**：PowerShell 默认编码读取 SQL 文件时，中文 COMMENT 变成乱码，MySQL 解析失败。

**修复**：使用 UTF-8 读取后再管道传入：
```powershell
Get-Content .\docs\sql\tenant_default-template.sql -Raw -Encoding UTF8 | docker exec -i smartmail-mysql-1 mysql -uroot -proot
```
对其他 `tenant_default-*.sql` 同样加上 `-Encoding UTF8`。

---

## 5. contact-service 请求 /api/contact/contact/page 返回 500，Hikari 一直 “Starting...”

**现象**：通过网关访问 contact 分页接口返回 500；容器日志中反复出现 `HikariPool-1 - Starting...`，无 “Start completed” 也无异常，连接似乎挂起。

**原因**：
1. **数据库地址在容器内错误**：默认 `application.yml` 使用 `localhost:3306`，在 Docker 容器内 localhost 指向本容器，无法连到 MySQL 容器；
2. **Docker 网络隔离**：mysql、redis、rabbitmq、mailhog 未加入 `smartmail` 网络，而 contact、gateway 等在该网络中；contact 解析服务名 `mysql` 失败（`getent hosts mysql` 无结果），导致连接一直无法建立。

**修复**：
1. **contact-service 在 Docker 下使用正确库地址与密码**：新增 `application-docker.yml`，配置 `app.tenant.base-url: jdbc:mysql://mysql:3306`、`password: root`；docker-compose 中 contact 设置 `SPRING_PROFILES_ACTIVE: docker`。
2. **统一网络**：在 `docker-compose.yml` 中为 **mysql、redis、rabbitmq、mailhog** 增加 `networks: - smartmail`，与 contact、gateway 等处于同一网络，使服务名 `mysql` 可解析。
3. **可选**：在 `TenantDataSourceConfig` 中为 Hikari 设置 `connectionTimeout`、`initializationFailTimeout`（如 15000ms），便于连接失败时快速报错而非长时间挂起。

**涉及文件**：
- `contact-service/src/main/resources/application-docker.yml`（新建）
- `docker-compose.yml`（mysql/redis/rabbitmq/mailhog 增加 networks；contact 增加 SPRING_PROFILES_ACTIVE）
- `contact-service/.../TenantDataSourceConfig.java`（可选：Hikari 超时）

---

## 6. 500 等未捕获异常无服务端日志

**现象**：接口返回 `{"errorCode":"500","errorInfo":"服务器内部错误"}` 时，无法从日志中看到具体异常堆栈。

**修复**：在 `common` 模块的 `GlobalExceptionHandler` 中，对 `Exception.class` 的 `@ExceptionHandler` 方法内增加日志输出，例如：
```java
log.error("未捕获异常 path={} {}", request.getRequestURI(), ex.getMessage(), ex);
```
便于后续通过 `docker logs` 等查看根因。

**涉及文件**：`common/.../exception/GlobalExceptionHandler.java`

---

## 7. docker-compose 版本告警

**现象**：执行 `docker compose` 时出现：
`the attribute 'version' is obsolete, it will be ignored, please remove it to avoid potential confusion`

**修复**：从 `docker-compose.yml` 顶部删除 `version: "3.8"` 行（Compose V2 不再需要该字段）。

**涉及文件**：`docker-compose.yml`

---

## 附录：快速检查清单

- 所有需连 MySQL 的服务在 Docker 中应使用 **服务名**（如 `mysql`）且与 mysql 处于 **同一网络**（`networks: - smartmail`）。
- 使用 Docker 时，业务服务通过 **SPRING_PROFILES_ACTIVE=docker** 或环境变量覆盖，确保 `base-url` 指向 `jdbc:mysql://mysql:3306`，密码与 compose 中 MySQL 一致（如 `root`）。
- 租户库与表需先执行 `docs/sql/` 下脚本（如 `schema-tenant-default.sql`、`tenant_default-*.sql`），且 PowerShell 管道执行时加 `-Encoding UTF8`。
