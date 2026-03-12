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

---

## 8. 发信设置页 404：delivery 镜像未包含 SMTP 配置接口

**现象**：前端打开「发信设置」或保存 SMTP 配置时，接口返回 404，提示「获取配置失败」「保存失败」。

**原因**：delivery-service 的 Dockerfile 仅 COPY 本地已编译的 JAR（`delivery-service/target/delivery-service-*.jar`）。若未先执行 `mvn package -pl delivery-service -am`，镜像使用的是旧 JAR，不包含新增的 `SmtpConfigController`（GET/PUT `/api/delivery/smtp-config`）。

**修复**：Docker 部署 delivery 前，须在项目根目录先执行 `.\mvnw.cmd package -pl delivery-service -am` 生成最新 JAR，再执行 `docker-compose build delivery` 与 `docker-compose up -d delivery`。文档中已说明该顺序（STARTUP-AND-VERIFICATION.md「发信设置页 404」）。

**涉及文件**：`docs/STARTUP-AND-VERIFICATION.md`、`delivery-service/Dockerfile`（注释中说明需先 mvn package）

---

## 9. common 模块缺少 slf4j 导致 delivery 编译失败

**现象**：执行 `mvn package -pl delivery-service -am` 时，common 模块编译报错：`程序包 org.slf4j 不存在`、`找不到符号: 类 Logger`（GlobalExceptionHandler 使用 slf4j）。

**原因**：common 的 `pom.xml` 未声明 `slf4j-api` 依赖，而 `GlobalExceptionHandler` 使用了 `Logger`/`LoggerFactory`。

**修复**：在 `common/pom.xml` 的 `<dependencies>` 中增加：
```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
</dependency>
```

**涉及文件**：`common/pom.xml`

---

## 10. SmtpConfigController 中 logDebug 参数个数不匹配导致编译失败

**现象**：delivery-service 编译报错：`无法将方法 logDebug 应用到给定类型; 需要: String,String,String,Object,String,Object 找到: String,String,String,String`。

**原因**：调试用 `logDebug` 方法签名为 6 个参数（message, hypothesisId, k1, v1, k2, v2），其中一处调用（parseUserId missing）只传了 4 个参数。

**修复**：为该调用补全后两个参数，例如 `logDebug("parseUserId missing", "H2", "reason", "userIdStr null or blank", "n/a", "");`。后续清理调试代码时已移除全部 logDebug 调用及方法本身。

---

## 11. 发信设置页 500：smtp_config 表不存在

**现象**：执行 GET/PUT `/api/delivery/smtp-config` 返回 500，前端显示「服务器错误」。

**原因**：租户库（如 `tenant_default`）中未执行 `tenant_default-smtp_config.sql`，表 `smtp_config` 不存在，Mapper 查询时抛出异常。

**修复**：
1. 在租户库中执行 `docs/sql/tenant_default-smtp_config.sql` 建表。Docker 下示例（PowerShell，UTF-8 避免中文乱码）：
   ```powershell
   $sql = [System.IO.File]::ReadAllText("$PWD\docs\sql\tenant_default-smtp_config.sql", [System.Text.Encoding]::UTF8)
   $sql | docker-compose exec -T mysql mysql -uroot -proot tenant_default
   ```
2. SmtpConfigController 中对异常做捕获，若异常信息包含 "doesn't exist" 或 "smtp_config"，返回明确 errorInfo：「smtp_config 表不存在，请在租户库执行 docs/sql/tenant_default-smtp_config.sql」，便于前端展示。
3. STARTUP-AND-VERIFICATION.md 中租户建表列表已加入 `tenant_default-smtp_config.sql`，常见问题中增加「发信设置页 500」说明。

**涉及文件**：`delivery-service/.../SmtpConfigController.java`（mapToBizException）、`docs/STARTUP-AND-VERIFICATION.md`

---

## 12. 发信设置页 500：delivery 在 Docker 中未配置 MySQL 连接

**现象**：建表后发信设置接口仍返回 500，后端日志或前端返回的错误信息为 `CannotGetJdbcConnectionException: Failed to obtain JDBC Connection`。

**原因**：docker-compose 中 delivery 服务未配置租户数据源环境变量，运行时仍使用默认 `application.yml` 中的 `jdbc:mysql://localhost:3306`。在容器内 localhost 指向本容器，无法连接 MySQL 容器。

**修复**：在 `docker-compose.yml` 的 delivery 服务下增加与 tracking、audit 等一致的 MySQL 配置，并依赖 mysql 健康后再启动：
- 环境变量：`APP_TENANT_BASE_URL: jdbc:mysql://mysql:3306`、`APP_TENANT_USERNAME: root`、`APP_TENANT_PASSWORD: root`
- `depends_on`: 增加 `mysql: condition: service_healthy`

**涉及文件**：`docker-compose.yml`

---

## 13. 创建活动时带 Token 但 created_by 仍为 NULL（已澄清）

**现象**：POST 创建活动时请求头已带 `Authorization: Bearer <accessToken>`，但部分活动在库表 `campaign.created_by` 中为 NULL，导致发信走默认通道而非该用户的 SMTP。

**原因**：经调试日志与库表核对，网关解析 JWT 的 userId、转发 X-User-Id、Campaign 接收并 setCreatedBy、以及 MyBatis-Plus 持久化均正常。**created_by 为 NULL 的记录来自未带 Token 的创建请求**（如早期用 Postman 未填 Authorization、或前端未传 Token）。带 Token 创建的活动（如 id 3、4）在库中 `created_by` 正确为 1。

**结论与建议**：无需改业务代码。创建活动接口**必须带有效 Token**，网关才会注入 X-User-Id，Campaign 才会写入 created_by；未带或无效 Token 时 created_by 为空，发信会走默认通道。文档中已强调「创建活动需带 Token」。

**涉及文件**：无代码修复；`docs/PROJECT-STATUS.md` 中项目状态已汇总；调试用 NDJSON 日志已从 `JwtAuthFilter`、`CampaignController` 中移除。

---

## 14. 登录后客户/模板/分组/活动接口 500（Unknown column 'local_id'）

**现象**：登录后访问客户、模板、分组、营销活动等接口返回 500，错误信息含 `Unknown column 'local_id'` 或类似列不存在。

**原因**：业务已改为按 local_id 对外暴露 id，Mapper 查询/写入使用了 `local_id` 列，但租户库中对应表尚未执行 local_id 迁移脚本，表中无该列。

**修复**：在租户库（如 tenant_default）中按顺序执行 docs/sql 下迁移脚本：`tenant_default-contact-migration-local_id.sql`、`tenant_default-template-migration-local_id.sql`、`tenant_default-contact_group-migration-local_id.sql`、`tenant_default-campaign-migration-local_id.sql`。执行完成后重启 contact-service、template-service、campaign-service（或重新部署）。

**涉及文件**：无代码修改；`docs/sql/` 下上述迁移脚本；`docs/STARTUP-AND-VERIFICATION.md` 中补充迁移说明。

---

## 15. 加入分组返回 200 但分组内「当前成员」看不到该客户

**现象**：调用「将客户加入分组」接口返回 200，但前端在分组「当前成员」列表或按该分组筛选客户时，看不到刚加入的客户。

**原因**：加入分组时前端传的 groupId 为分组的 **local_id**，后端已按 local_id 解析并正确写入 contact_group_member。但客户分页接口在按 groupId 筛选时，误将 groupId 当作分组的**内部主键**查询 contact_group_member，导致查不到刚写入的记录（因表中存的是内部 group id）。

**修复**：在 ContactServiceImpl.pageList 中，当 groupId != null 时，先通过 contactGroupService.getByLocalIdAndTenant(groupId, tenantId) 得到内部 group id，再调用 selectPageByGroupId(internalGroupId, tenantId)。与分组成员接口对 groupId 的约定一致（均为 local_id）。

**涉及文件**：`contact-service/.../service/impl/ContactServiceImpl.java`

---

## 16. 立即发送/计划触发到错误活动（id 不一致）

**现象**：前端对「活动 id=1」点击立即发送或创建计划并触发，实际投递使用的活动不是该条（如用了内部主键为 1 的另一条活动）。

**原因**：对外展示的活动 id 为 **local_id**（按 created_by 从 1 连续），而 delivery 调用 campaign 的 GET 时未传用户信息，campaign 将路径中的 id 当作内部主键查询，导致 id 与 created_by 不对应。

**修复**：1）schedule_job 表增加 created_by 字段，创建计划时写入；2）CampaignTriggerPayload 与调度触发链路传递 createdBy；3）PrepareSendService.prepareAndEnqueue(campaignId, createdBy, tenantId, scheduleId)，delivery 请求 campaign 时若 createdBy 非空则请求头带 X-User-Id；4）campaign 的 GET 在存在 X-User-Id 时按 getByLocalIdAndCreatedBy(campaignId, createdBy) 查询。前端创建计划时传 createdBy（如 row.createdBy ?? store.state.user.userId）。

**涉及文件**：scheduler 表迁移、ScheduleJob/ScheduleCreateRequest、CampaignTriggerPayload、DownstreamClient.getCampaign、PrepareSendService；campaign-service CampaignController/ServiceImpl；前端 CampaignListView、ScheduleListView 与类型定义。

---

## 17. 定时发送下列表未按用户隔离、计划 ID 未从 1 开始

**现象**：定时发送下列表展示所有用户的计划；计划 ID 显示为内部主键，不是「按用户从 1 开始」的序号。

**原因**：列表接口未按 created_by 过滤；schedule_job 表无 local_id，创建与列表返回的是内部主键。

**修复**：1）SchedulerController.list 读取请求头 X-User-Id，调用 scheduleJobService.listByCreatedBy(createdBy) 仅返回该用户计划；2）schedule_job 增加 local_id 列，迁移脚本按 created_by 分区赋序号，唯一键 uk_created_by_local_id；3）ScheduleJob 实体与 Service 创建时分配 nextLocalIdForCreatedBy，列表返回 ScheduleJobListItem，id 为 local_id；创建接口返回 id 为 local_id。

**涉及文件**：`tenant_default-scheduler-migration-local_id.sql`、ScheduleJob、ScheduleJobService、SchedulerController、ScheduleJobListItem、ScheduleJobMapper.xml。

---

## 本次会话说明（用户网页配置 SMTP）

第 8～12 条为实现「用户网页配置 SMTP」及 Docker 部署时遇到的问题与修复：支持每个登录用户在发信设置页配置自己的 SMTP，发送时按活动创建人使用其配置发信；建表、common 依赖、Controller 编译、租户库建表与 Docker 下 MySQL 连接均已补齐。第 13 条为 created_by 为 NULL 的排查结论（带 Token 即正常）。

## 本次会话说明（local_id、分组成员、调度与计划隔离）

第 14～17 条为本会话中与 local_id、分组成员解析、调度投递 createdBy、定时发送按用户隔离及计划 ID（local_id）相关的 Bug 与修复。功能汇总见 PROJECT-STATUS.md「三、近期更新汇总」。

---

## 18. 注册账号 docker-compose down 后无法登录、首次 up 后立刻登录失败

**现象**：使用 Docker 时，执行 `docker-compose down` 再 `docker-compose up -d` 后，之前注册的非 admin 账号无法登录，需再次注册才能登录（但该账号下的模板等业务数据仍在）；另有时首次 up 后立刻调用登录接口失败，需多次执行 up 或等待一段时间后才正常。

**原因**：  
1. IAM 在 Docker 内原使用 H2 内存库（`jdbc:h2:mem:iam`），用户与租户元数据未持久化，容器重建后数据丢失。  
2. 改为 MySQL 后，IAM 启动时若 MySQL 尚未完全就绪，Hikari 默认 30 秒连接超时易导致首次连接失败，表现为「刚 up 后立刻登录失败」。

**修复**：  
1. IAM 在 Docker 下改为连接 MySQL 独立库 `iam`：新增 `iam-service/src/main/resources/application-docker.yml`（数据源 URL 含 `createDatabaseIfNotExist=true`，JPA `ddl-auto: update`），并修改 `docker-compose.yml` 中 IAM 的 `SPRING_DATASOURCE_*` 为 MySQL（root/root）。用户与租户数据随 `mysql_data` 卷持久化。  
2. 在 `application-docker.yml` 中为 Hikari 设置 `connection-timeout: 60000`，启动阶段有 60 秒等待 MySQL，减少「首次 up 后立刻登录失败」。

**涉及文件**：`iam-service/src/main/resources/application-docker.yml`、`docker-compose.yml`。详见 PROJECT-STATUS.md「本会话改动：IAM 持久化与登录」。
