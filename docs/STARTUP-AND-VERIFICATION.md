# SmartMail 项目启动与验证指南

本文档说明如何准备环境、初始化数据库、启动各服务，并通过登录与接口调用完成基本验证。

---

## 一、环境要求

| 项目 | 要求 |
|------|------|
| JDK | 17 及以上 |
| Maven | 3.6+，或使用项目自带 `mvnw` / `mvnw.cmd` |
| Docker（可选） | 使用 docker-compose 一键启动依赖与全部服务时需安装 Docker、Docker Compose |
| MySQL | 8.0（若不用 Docker，需本地安装并创建库） |
| Redis | 7（当前主要用于可选能力，按需安装） |
| RabbitMQ | 3.x，带 management 插件（投递与调度依赖） |

---

## 二、数据库初始化

若使用 **Docker Compose** 启动 MySQL，会自动创建 `platform` 库；否则请先安装 MySQL 并创建空库（如 `platform`）。

**IAM 用户与租户数据（Docker 环境）**：IAM 服务在 Docker 下使用 MySQL 的独立库 `iam`（非 `platform`）。首次启动时通过 JDBC 参数 `createDatabaseIfNotExist=true` 自动创建 `iam` 库，并由 JPA（`ddl-auto: update`）自动创建/更新 `sys_user`、`tenant_metadata` 表，**无需**对 IAM 执行任何 SQL 脚本。用户与租户数据随 MySQL 的 `mysql_data` 卷持久化，重启容器后注册账号仍可登录。

在项目根目录下，按**顺序**执行 `docs/sql/` 中的脚本：

1. **平台库与租户 Schema**  
   - `schema-platform.sql`：平台库表结构。  
   - `schema-tenant-default.sql`：创建默认租户 Schema（如 `tenant_default`）。

2. **租户库表结构**（在对应租户 Schema 下执行，或脚本内已指定 schema）  
   - `tenant_default-contact.sql`  
   - `tenant_default-template.sql`  
   - `tenant_default-campaign.sql`  
   - `tenant_default-scheduler.sql`  
   - `tenant_default-delivery.sql`  
   - `tenant_default-tracking.sql`  
   - `tenant_default-smtp_config.sql`（发信设置页依赖）  
   - `tenant_default-abtest.sql`  
   - `tenant_default-unsubscribe-blacklist.sql`  
   - `tenant_default-audit.sql`  

3. **local_id 与 created_by 迁移**（若表已存在且需支持「对外 id 从 1 连续、按用户/租户隔离」）  
   在对应租户 Schema 下按顺序执行（表必须先有基础结构）：  
   - `tenant_default-contact-migration-local_id.sql`  
   - `tenant_default-template-migration-local_id.sql`  
   - `tenant_default-contact_group-migration-local_id.sql`  
   - `tenant_default-campaign-migration-local_id.sql`  
   - `tenant_default-scheduler-migration-created_by.sql`  
   - `tenant_default-scheduler-migration-local_id.sql`  
   未执行上述迁移时，客户/模板/分组/活动等接口可能报 500（Unknown column 'local_id'）。详见 BUGFIX-LOG.md 第 14 条。

执行方式示例（请根据实际库名与 schema 调整）：

```bash
# 示例：MySQL 命令行
mysql -u root -p < docs/sql/schema-platform.sql
mysql -u root -p < docs/sql/schema-tenant-default.sql
# 再执行各 tenant_default-*.sql
```

或使用图形化工具依次执行上述 SQL 文件，确保无报错。

---

## 三、启动方式

### 方式一：Docker Compose（推荐）

**构建或启动前需先打包后端 JAR**（Dockerfile 从项目根目录复制各模块 `*-service/target/*.jar`，未打包会报 “no such file or directory”）：

```bash
# Windows
.\mvnw.cmd clean package -DskipTests

# Linux / macOS
./mvnw clean package -DskipTests
```

然后在项目根目录执行：

```bash
docker-compose up -d
```

将启动：MySQL、Redis、RabbitMQ、MailHog，以及网关、IAM、contact、template、campaign、scheduler、delivery、tracking、audit 各服务。  
首次会构建镜像，构建上下文为项目根目录。IAM 依赖 MySQL 健康后启动，并连接 MySQL 的 `iam` 库（自动建库建表）；若首次 up 后立刻访问登录接口失败，可等待数十秒后重试（Hikari 连接超时已设为 60 秒）。

**端口一览**（宿主机）：

| 服务 | 端口 |
|------|------|
| 网关 | 8080 |
| IAM | 8081 |
| contact | 8082 |
| template | 8083 |
| campaign | 8084 |
| scheduler | 8085 |
| delivery | 8086 |
| tracking | 8087 |
| audit | 8088 |
| MySQL | 3306 |
| Redis | 6379 |
| RabbitMQ | 5672，管理界面 15672 |
| MailHog SMTP | 1025，Web 8025 |

使用 Docker 时，各服务通过服务名（如 `mysql`、`rabbitmq`）互相访问；本地调试时需在对应 `application.yml` 或 profile 中改为 `localhost` 与上述端口。

**若出现无法连接 Docker Hub（如 `failed to fetch oauth token`、连接 auth.docker.io 超时）**：多为网络或镜像源问题，可先**不启动前端**，仅启动基础设施与后端服务（前端需拉取 `node:20-alpine`、`nginx:alpine`，易失败）：

```bash
docker-compose up -d mysql redis rabbitmq mailhog gateway iam contact template campaign scheduler delivery tracking audit
```

前端可改为在本地用 `npm run dev` 等方式运行，或配置 Docker 镜像加速后再执行完整 `docker-compose up -d`（见本文档「五、常见问题」）。

### 方式二：本地 IDE / 命令行启动

1. **先启动依赖**  
   - MySQL、Redis、RabbitMQ（若需）需先就绪；可用 Docker 仅启动依赖：
     ```bash
     docker-compose up -d mysql redis rabbitmq mailhog
     ```
   - 确认各服务 `application.yml`（或 `application-{profile}.yml`）中的地址、端口、账号密码与当前环境一致。

2. **编译**  
   在项目根目录执行：
   ```bash
   .\mvnw.cmd package -DskipTests
   ```
   （Linux/Mac 使用 `./mvnw`。）

3. **启动顺序建议**  
   - 先启动 **iam-service**（端口 8081）；  
   - 再启动 **gateway-service**（端口 8080）；  
   - 其余服务（contact、template、campaign、scheduler、delivery、tracking、audit）可并行启动，端口见上表。  

   每个服务运行其主类（Spring Boot Application），例如：
   - IAM：`com.smartmail.iam.IamApplication`  
   - Gateway：`com.smartmail.gateway.GatewayApplication`  
   - 其他模块类似，见各模块 `src/main/java` 下包内的 Application 类。

---

## 四、验证步骤

### 1. 网关与 IAM 连通

确认网关（8080）将 `/api/iam/**` 转发到 IAM（8081）。  
可用登录接口自测（无需 Token）：

```bash
curl -X POST http://localhost:8080/api/iam/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"你的密码\"}"
```

- 若 IAM 使用内存用户（如 Spring Security 默认或配置的 user），请使用实际配置的用户名与密码。  
- 成功时返回 200 和 JSON，其中包含 `data.accessToken`、`data.refreshToken`；失败时可能是 401 或 4xx，Body 中带 `errorCode`、`errorInfo`。

### 2. 使用 Token 调用业务接口

从登录响应中取出 `accessToken`，请求需认证的接口，例如客户分页（contact 服务）：

```bash
curl -X GET "http://localhost:8080/api/contact/contact/page?page=1&size=20" \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json"
```

应返回 200 及分页数据（可能为空列表）。若返回 401，说明 Token 无效或未带对。

### 3. 验证 template / campaign / tracking / audit 接口

以下请求均需 Header：`Authorization: Bearer <accessToken>`，Base URL：`http://localhost:8080`。

| 服务 | 方法 | 路径 | 说明 |
|------|------|------|------|
| template | GET | `/api/template/template/list` | 模板列表 |
| template | POST | `/api/template/template` | 创建模板，Body 示例：`{"name":"欢迎邮件","subject":"欢迎","bodyHtml":"<p>你好</p>","variables":"name"}` |
| campaign | GET | `/api/campaign/campaign/list` | 活动列表 |
| campaign | POST | `/api/campaign/campaign` | 创建活动，Body 示例：`{"name":"首次营销","templateId":1,"groupId":1,"status":"draft"}` |
| tracking | GET | `/api/tracking/stats/1` | 活动 1 的打开/点击统计（campaignId=1） |
| audit | GET | `/api/audit/log/list?page=1&size=20` | 审计日志分页 |
| audit | POST | `/api/audit/log` | 写入审计，Body 示例：`{"userId":"1","action":"LOGIN","resource":"user","resourceId":"1","detail":"登录"}` |
| delivery | GET | `/api/delivery/smtp-config` | 当前用户 SMTP 配置（需登录，网关会带 X-User-Id） |
| delivery | PUT | `/api/delivery/smtp-config` | 保存当前用户 SMTP 配置 |

PowerShell 示例（将 `<token>` 换成实际 accessToken）：

```powershell
curl.exe -X GET "http://localhost:8080/api/template/template/list" -H "Authorization: Bearer <token>" -H "Content-Type: application/json"
curl.exe -X GET "http://localhost:8080/api/campaign/campaign/list" -H "Authorization: Bearer <token>" -H "Content-Type: application/json"
curl.exe -X GET "http://localhost:8080/api/tracking/stats/1" -H "Authorization: Bearer <token>" -H "Content-Type: application/json"
curl.exe -X GET "http://localhost:8080/api/audit/log/list?page=1&size=20" -H "Authorization: Bearer <token>" -H "Content-Type: application/json"
```

Docker 部署时，template / campaign / tracking / audit 需使用 `application-docker.yml`（`SPRING_PROFILES_ACTIVE=docker`）并确保已执行对应租户建表 SQL（见第二节）。若返回 500，可查看对应容器日志：`docker logs smartmail-template-1 --tail 30` 等。

### 4. 刷新 Token

```bash
curl -X POST http://localhost:8080/api/iam/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"<登录返回的 refreshToken>\"}"
```

应返回新的 accessToken 与 refreshToken。

### 5. 验证真实 SMTP 发信

发信链路：调度到点 → 投递 `smartmail.campaign.trigger` → delivery 的 `CampaignTriggerConsumer` 拉取活动/模板/分组联系人 → 创建批次与投递任务 → 投递 `smartmail.send.task` → `SendTaskConsumer` 按活动创建人（`createdBy`）取用户 SMTP 配置，有则用该配置发信，无则用默认通道（如 MailHog）发信。

#### 方式 A：默认通道（MailHog）验证

不配置用户 SMTP 时，发信走默认 `JavaMailSender`（Docker 下指向 MailHog）。用于验证「调度 → 触发 → 投递 → 发送」整条链路是否通畅。

1. **前置条件**：MySQL 已执行全部租户建表（含 `tenant_default-smtp_config.sql`）；RabbitMQ、delivery、scheduler、contact、template、campaign、gateway、IAM 已启动；MailHog 已启动（Docker 下为 `mailhog` 容器，宿主机访问 Web UI：`http://localhost:8025`）。
2. **准备数据**（以下均需带 `Authorization: Bearer <accessToken>`，先 POST `/api/iam/auth/login` 用 `admin` / `admin123` 取 token）：
   - 创建 1 个模板：`POST /api/template/template`，Body 示例：`{"name":"验证发信","subject":"SMTP 验证邮件","bodyHtml":"<p>您好，这是一封验证邮件。</p>","variables":""}`，记下返回的 `data.id`（如 `templateId=1`）。
   - 创建 1 个分组：`POST /api/contact/group`，Body：`{"name":"验证分组","ruleType":"static"}`，记下 `data.id`（如 `groupId=1`）。
   - 创建 1 个联系人：`POST /api/contact/contact`，Body：`{"email":"test@example.com","name":"测试"}`，记下 `data.id`（如 `contactId=1`）。
   - 将联系人加入分组（当前无 REST 接口，需在租户库执行 SQL，将 `<groupId>`、`<contactId>` 换成上面得到的 ID）：
     ```sql
     INSERT INTO tenant_default.contact_group_member (group_id, contact_id, create_time)
     VALUES (<groupId>, <contactId>, NOW());
     ```
   - 创建活动：`POST /api/campaign/campaign`，Body：`{"name":"验证活动","templateId":1,"groupId":1,"status":"draft"}`（templateId/groupId 与上面一致），记下 `data.id`（如 `campaignId=1`）。注意：创建时请求头会带 X-User-Id（网关从 JWT 注入），活动 `createdBy` 即为当前用户（如 admin 的 id=1）；未配置该用户 SMTP 时，发信走默认通道。
3. **创建发送计划**：`POST /api/scheduler/schedule`，Body 中 `runAt` 设为当前时间 1～2 分钟后（格式 `yyyy-MM-dd HH:mm:ss`），**注意时区约定**：

   - 若通过 **前端界面**「立即发送」或「创建计划」操作，前端会自动将本地时间转换为 UTC 再传给后端，**无需手工处理时区**；
   - 若通过 **Postman/终端直接调用接口**，`runAt` 应当使用 **UTC 时间**，避免 Docker 中按 UTC 运行的调度服务出现 8 小时时差。

   示例 Body：

   ```json
   {"campaignId":1,"cronExpr":"","runAt":"2025-03-10 15:05:00"}
   ```
4. **等待触发**：scheduler 每分钟扫描，到点后会将活动触发消息投递到 RabbitMQ，delivery 消费后生成发送任务并发送。等待约 2 分钟后：
   - 打开 **MailHog Web UI**：`http://localhost:8025`，应能看到一封收件人为 `test@example.com` 的邮件（主题为「SMTP 验证邮件」）。
   - 调用 **投递状态**：`GET /api/delivery/delivery/status/1`（campaignId=1），应看到 `total`、`sent` 等汇总（如 `sent >= 1` 表示成功）。
5. **若未收到**：检查 scheduler 与 delivery 容器/进程日志（如 `docker logs smartmail-delivery-1 --tail 50`）；确认 RabbitMQ 队列 `smartmail.campaign.trigger`、`smartmail.send.task` 有被消费；确认 MailHog 端口 1025 在 delivery 侧可访问（Docker 下 host 为 `mailhog`）。**若专门验证 scheduler 在 Docker 内能否到点触发**，可参考 [SCHEDULER-DOCKER-TEST.md](./SCHEDULER-DOCKER-TEST.md)。

#### 方式 B：用户 SMTP 配置（真实 SMTP）验证

使用真实邮箱服务（如 QQ 邮箱、163、企业 SMTP）发信，验证「用户配置 SMTP → 按活动创建人发信」是否正常。

1. **配置用户 SMTP**：登录后调用 `PUT /api/delivery/smtp-config`，Body 示例（按实际邮箱服务填写）：
   ```json
   {
     "host": "smtp.qq.com",
     "port": 465,
     "username": "your-qq@qq.com",
     "password": "授权码",
     "fromEmail": "your-qq@qq.com",
     "fromName": "SmartMail 测试",
     "useSsl": true
   }
   ```
   QQ 邮箱需在设置中开启 SMTP 并生成授权码；163 类似。生产环境务必配置 `app.smtp.encryption-key`（AES 密钥），密码会加密落库。
2. **准备数据**：同方式 A（创建模板、分组、联系人、将联系人加入分组、创建活动）。创建活动时务必使用**已配置 SMTP 的同一用户**登录（即带该用户的 Token），这样活动的 `createdBy` 会对应到该用户，发信时才会使用其 SMTP 配置。
3. **创建计划并等待**：同方式 A，`runAt` 设为 1～2 分钟后。
4. **验证**：到**真实收件箱**（如 test@example.com 或您填写的联系人邮箱）查收；同时可查 `GET /api/delivery/delivery/status/{campaignId}` 与 delivery 日志确认发送结果。**使用 Postman 操作时**，可参考 [POSTMAN-SMTP-VERIFY.md](./POSTMAN-SMTP-VERIFY.md) 按步骤完成登录、配置 SMTP、创建模板/分组/联系人/活动/计划及查询投递状态。

#### 一键验证脚本（方式 A，PowerShell）

项目在 `docs/scripts/verify-smtp-send.ps1` 提供了脚本：登录 → 创建模板/分组/联系人 → 提示执行一条 SQL 将联系人加入分组 → 创建活动与计划（runAt=当前时间+2 分钟）→ 等待后查询投递状态并提示打开 MailHog。需在项目根目录、网关 8080 可用时执行；默认用户 `admin` / `admin123`。

### 6. 单元/集成测试（可选）

在项目根目录执行：

```bash
.\mvnw.cmd test
```

或只跑部分模块：

```bash
.\mvnw.cmd test -pl iam-service,gateway-service,contact-service,campaign-service
```

通过即表示核心路径（如 IAM、网关、contact、campaign）在当前环境可用。

---

## 五、常见问题

- **登录 401**：检查 IAM 中配置的用户名/密码、以及网关路由是否把 `/api/iam/**` 指到 IAM 端口。  
- **业务接口 401**：确认请求头为 `Authorization: Bearer <accessToken>`，且 Token 未过期。  
- **404 / 连接被拒绝**：确认网关（8080）已启动，且对应后端服务已启动且路由 Path 与文档一致。**发信设置页 404**：若 GET/PUT `/api/delivery/smtp-config` 返回 404，说明当前运行的 delivery-service 未包含 SMTP 配置接口。**Docker 部署**：须先在项目根目录执行 `.\mvnw.cmd package -pl delivery-service -am` 生成最新 JAR，再执行 `docker-compose build delivery` 和 `docker-compose up -d delivery`；**本地进程**：同样先执行上述 Maven 命令再重启 delivery 进程。  
- **发信设置页 500**：若接口返回 500 且提示「smtp_config 表不存在」，请在租户库（如 `tenant_default`）中执行 `docs/sql/tenant_default-smtp_config.sql` 建表后重试。  
- **数据库连接失败**：检查 MySQL 已启动、库与 schema 已创建、各服务配置的 URL/用户名/密码正确；Docker 内用服务名，本机用 localhost。  
- **RabbitMQ / 投递不工作**：确认 RabbitMQ 已启动，delivery、scheduler 等配置的 host/port 正确，队列与交换机已由 RabbitConfig 等创建。  
- **Docker 构建/拉取失败（failed to fetch oauth token、连接 auth.docker.io 超时）**：多为无法访问 Docker Hub。可先执行「不包含 frontend」的启动命令（见方式一上文）；若要完整构建，可配置 Docker 镜像加速：Docker Desktop → Settings → Docker Engine，在 JSON 中为 `registry-mirrors` 添加国内镜像（如 `https://docker.1ms.run` 等），保存后重试 `docker-compose up -d`。

按上述顺序完成环境准备、数据库初始化、服务启动与接口验证后，即完成项目启动与基本功能验证。更多接口说明见 [API-AND-CLASS-REFERENCE.md](./API-AND-CLASS-REFERENCE.md)，发布前检查见 [RELEASE-CHECKLIST.md](./RELEASE-CHECKLIST.md)。
