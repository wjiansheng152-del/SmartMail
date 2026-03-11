# Scheduler 在 Docker 内触发测试指南

本文档说明如何在 Docker 环境中验证 scheduler-service 能否正常扫描 `schedule_job` 表、到点触发并投递消息到 RabbitMQ，由 delivery 消费并发送邮件。

---

## 一、前置条件

1. **数据库已初始化**  
   已按 [STARTUP-AND-VERIFICATION.md](./STARTUP-AND-VERIFICATION.md) 执行平台库与租户库脚本，**至少包含**：
   - `schema-platform.sql`
   - `schema-tenant-default.sql`
   - `tenant_default-scheduler.sql`（scheduler 依赖的 `schedule_job` 表）
   - 以及 contact、template、campaign、delivery、smtp_config 等业务表

2. **Docker 内服务已就绪**  
   以下服务需在 Docker 中运行且健康：
   - MySQL、RabbitMQ（scheduler 依赖）
   - gateway、iam（用于通过 8080 创建计划）
   - contact、template、campaign（准备活动与计划数据）
   - **scheduler**（被验证对象）
   - delivery、mailhog（触发后发信与收信验证）

---

## 二、构建并启动 Scheduler 容器

Scheduler 的 Docker 镜像从**项目根目录**构建，需先编译出 JAR，再构建镜像。

### 1. 编译 scheduler 模块

在项目根目录执行：

```powershell
.\mvnw.cmd package -pl scheduler-service -am -DskipTests
```

确认 `scheduler-service\target\scheduler-service-*.jar` 已生成。

### 2. 构建并启动 scheduler 容器

```powershell
docker-compose build scheduler
docker-compose up -d scheduler
```

若尚未启动其他依赖，可先启动基础设施与调度相关服务：

```powershell
docker-compose up -d mysql redis rabbitmq mailhog
# 等待 MySQL/RabbitMQ 健康后再启动应用
docker-compose up -d gateway iam contact template campaign scheduler delivery
```

### 3. 确认 Scheduler 环境变量（docker-compose）

Scheduler 在 Docker 中通过环境变量覆盖默认配置，无需 `application-docker.yml`：

| 环境变量 | 说明 | 示例值 |
|----------|------|--------|
| `SPRING_RABBITMQ_HOST` | RabbitMQ 主机（容器内用服务名） | `rabbitmq` |
| `APP_TENANT_BASE_URL` | 租户库 JDBC 基础 URL | `jdbc:mysql://mysql:3306` |
| `APP_TENANT_USERNAME` | 数据库用户名 | `root` |
| `APP_TENANT_PASSWORD` | 数据库密码 | `root` |

端口 8085 映射到宿主机，网关将 `/api/scheduler/**` 转发到 `scheduler:8085`。

---

## 三、验证触发链路

整体流程：**创建计划（runAt = 当前时间 + 1～2 分钟）→ Scheduler 定时扫描到点 → 投递到 RabbitMQ → Delivery 消费并发信 → MailHog 收信**。

### 方式 A：使用现有验证脚本（推荐）

在**项目根目录**、且**网关 8080 可用**（Docker 或本地）时执行：

```powershell
.\docs\scripts\verify-smtp-send.ps1
```

脚本会：登录 → 创建模板/分组/联系人 → 提示执行一条 SQL 将联系人加入分组 → 创建活动与计划（runAt = 当前时间 + 2 分钟）→ 等待约 150 秒 → 查询投递状态并提示打开 MailHog。

- 若 **scheduler 在 Docker 内**：创建计划时请求经网关转发到 Docker 中的 scheduler，计划写入 MySQL（宿主机或同一 Docker 网络中的 MySQL）；到点后 **Docker 内的 scheduler** 扫描同一库、投递 MQ，delivery 消费发信。
- 成功标志：MailHog（http://localhost:8025）收到主题为「SMTP 验证邮件」的邮件；`GET /api/delivery/delivery/status/{campaignId}` 返回 `sent >= 1`。

### 方式 B：手动逐步验证

1. **登录并获取 Token**  
   `POST http://localhost:8080/api/iam/auth/login`，Body：`{"username":"admin","password":"admin123"}`，记下 `data.accessToken`。

2. **准备数据**（均需 Header：`Authorization: Bearer <accessToken>`）  
   - 创建模板、分组、联系人（见 STARTUP-AND-VERIFICATION 文档「方式 A」）。  
   - 将联系人加入分组：在租户库执行  
     `INSERT INTO tenant_default.contact_group_member (group_id, contact_id, create_time) VALUES (<groupId>, <contactId>, NOW());`  
   - 创建活动：`POST /api/campaign/campaign`，Body 含 `templateId`、`groupId`、`status: "draft"`，记下 `data.id`（campaignId）。

3. **创建发送计划（关键步骤）**  
   ```http
   POST http://localhost:8080/api/scheduler/schedule
   Authorization: Bearer <accessToken>
   Content-Type: application/json

   {"campaignId": 1, "cronExpr": "", "runAt": "2025-03-11 16:05:00"}
   ```  
   将 `runAt` 设为**当前时间 1～2 分钟后**（格式 `yyyy-MM-dd HH:mm:ss`）。  
   成功则返回 `data: <scheduleId>`。

4. **等待触发**  
   Scheduler 内部为 `@Scheduled(fixedDelay = 60_000, initialDelay = 10_000)`，即约每 60 秒扫描一次、启动后约 10 秒首次执行。  
   等待 1～2 分钟后：

   - **看 Scheduler 容器日志**（应出现触发记录）：
     ```powershell
     docker logs smartmail-scheduler-1 --tail 50
     ```
     成功时会看到类似：`Triggered schedule job id=1, campaignId=1`。

   - **看 MailHog**  
     打开 http://localhost:8025，应收到对应邮件。

   - **看投递状态**  
     `GET http://localhost:8080/api/delivery/delivery/status/{campaignId}`，应看到 `sent >= 1` 等。

---

## 四、故障排查

| 现象 | 可能原因 | 处理建议 |
|------|----------|----------|
| 创建计划返回 404/502 | 网关未将 `/api/scheduler/**` 转到 scheduler，或 scheduler 未启动 | 检查 `docker ps` 中 scheduler 容器在运行；检查 gateway 的 `application-docker.yml` 中 scheduler 的 uri：`http://scheduler:8085` |
| 创建计划 500 | 租户库或 `schedule_job` 表不存在 | 在租户库执行 `docs/sql/tenant_default-scheduler.sql` |
| 到点后无触发、日志无 “Triggered” | ① Scheduler 连不上 MySQL ② 连不上 RabbitMQ ③ 时区/runAt 未到 | 查看 scheduler 容器启动日志是否有 DataSource/Rabbit 连接错误；确认 `APP_TENANT_BASE_URL=jdbc:mysql://mysql:3306`；确认 `SPRING_RABBITMQ_HOST=rabbitmq`；确认 runAt 为当前时间 1～2 分钟后 |
| 有 “Triggered” 但 MailHog 无邮件 | Delivery 未消费或发信失败 | 查看 delivery 容器日志：`docker logs smartmail-delivery-1 --tail 80`；RabbitMQ 管理界面 http://localhost:15672 查看队列 `smartmail.campaign.trigger`、`smartmail.send.task` 是否被消费 |
| Scheduler 连库失败 | 容器内无法解析 `mysql` 或端口不对 | 确保 scheduler 与 mysql 在同一 Docker 网络 `smartmail`；`docker-compose up -d` 时已包含 mysql 与 scheduler |

---

## 五、小结

- **本地 IDEA**：scheduler 连本机 MySQL/ RabbitMQ，已确认正常。  
- **Docker 内**：需保证镜像已用最新 JAR 构建、环境变量指向 `mysql`/`rabbitmq`，且租户库与 `schedule_job` 表已建。  
按上述步骤创建计划并等待 1～2 分钟后，查看 scheduler 日志中的 “Triggered schedule job” 与 MailHog 收信即可确认 Docker 内触发正常。
