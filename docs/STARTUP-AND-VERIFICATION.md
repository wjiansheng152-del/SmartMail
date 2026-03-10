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
   - `tenant_default-abtest.sql`  
   - `tenant_default-unsubscribe-blacklist.sql`  
   - `tenant_default-audit.sql`  

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

在项目根目录执行：

```bash
docker-compose up -d
```

将启动：MySQL、Redis、RabbitMQ、MailHog，以及网关、IAM、contact、template、campaign、scheduler、delivery、tracking、audit 各服务。  
首次会构建镜像，需保证各服务 Dockerfile 能从根目录正确构建（构建上下文为项目根目录）。

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

### 5. 单元/集成测试（可选）

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
- **404 / 连接被拒绝**：确认网关（8080）已启动，且对应后端服务已启动且路由 Path 与文档一致。  
- **数据库连接失败**：检查 MySQL 已启动、库与 schema 已创建、各服务配置的 URL/用户名/密码正确；Docker 内用服务名，本机用 localhost。  
- **RabbitMQ / 投递不工作**：确认 RabbitMQ 已启动，delivery、scheduler 等配置的 host/port 正确，队列与交换机已由 RabbitConfig 等创建。  
- **Docker 构建/拉取失败（failed to fetch oauth token、连接 auth.docker.io 超时）**：多为无法访问 Docker Hub。可先执行「不包含 frontend」的启动命令（见方式一上文）；若要完整构建，可配置 Docker 镜像加速：Docker Desktop → Settings → Docker Engine，在 JSON 中为 `registry-mirrors` 添加国内镜像（如 `https://docker.1ms.run` 等），保存后重试 `docker-compose up -d`。

按上述顺序完成环境准备、数据库初始化、服务启动与接口验证后，即完成项目启动与基本功能验证。更多接口说明见 [API-AND-CLASS-REFERENCE.md](./API-AND-CLASS-REFERENCE.md)，发布前检查见 [RELEASE-CHECKLIST.md](./RELEASE-CHECKLIST.md)。
