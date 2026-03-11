# SmartMail 项目状态汇总

本文档汇总当前项目整体状态、接口与发信链路、待办及文档速查。**当前所有接口工作正常**，Docker 与本地启动、调度触发与发信链路均已验证通过。

---

## 一、当前状态

### 1. 接口与功能

| 领域 | 状态 | 说明 |
|------|------|------|
| 网关 / IAM | ✅ 正常 | 统一入口 8080，JWT 鉴权，登录/刷新，请求头注入 X-User-Id / X-Tenant-Id / X-Username |
| 客户 / 联系人 | ✅ 正常 | 客户 CRUD、分组、退订、黑名单；联系人加入分组需执行 SQL（无独立 REST） |
| 模板 | ✅ 正常 | 模板 CRUD |
| 活动 | ✅ 正常 | 活动 CRUD；创建活动需带 Token，created_by 正确落库，供发信时按用户 SMTP |
| 调度 | ✅ 正常 | 创建/列表计划，定时扫描 schedule_job 投递 MQ；Docker 内触发已验证 |
| 投递 | ✅ 正常 | 状态查询、用户 SMTP 配置 GET/PUT、消费触发与发送任务、按活动创建人 SMTP 发信 |
| 追踪 | ✅ 正常 | 像素、点击、统计 |
| 审计 | ✅ 正常 | 写入、分页查询 |

### 2. 发信与调度链路

- **调度**：scheduler 定时扫描 `schedule_job`（run_at 已到且 status=pending），投递 `smartmail.campaign.trigger`。
- **投递**：delivery 消费触发消息 → 拉取活动/模板/分组联系人 → 过滤退订与黑名单 → 创建 campaign_batch 与 delivery_task → 投递 `smartmail.send.task`；SendTaskConsumer 按活动 `created_by` 取用户 SMTP，有则用用户 SMTP，无则默认通道（如 MailHog）。
- **验证**：MailHog（http://localhost:8025）、真实 SMTP（PUT /api/delivery/smtp-config 后带 Token 创建活动）均可验证。

### 3. 部署与文档

- **Docker Compose**：`docker compose up -d` 可启动全部服务；数据库需预先按 `docs/sql/` 顺序初始化。
- **文档**：入口为 [docs/README.md](./README.md)，内含启动验证、接口说明、发信验证、Scheduler Docker 测试、发布清单与问题记录等索引。

---

## 二、待办与可选增强

### 高优先级

- 无。

### 中 / 低优先级（可选）

1. **联系人加入分组**：当前无 REST 接口，需在租户库执行 SQL 或 `docker exec ... mysql -e "INSERT INTO contact_group_member ..."`；可后续增加 API。
2. **delivery 调用 contact 的 unsubscribe/list**：该接口 404 时 delivery 已容错，可后续在 contact 补充或统一退订数据来源。
3. 活动状态显式流转、Cron 周期触发、多租户扩展、健康检查与补充单测等，见 [BACKEND-SUMMARY.md](./BACKEND-SUMMARY.md)。

---

## 三、文档与关键文件速查

| 用途 | 文档/路径 |
|------|-----------|
| 文档总览 | [docs/README.md](./README.md) |
| 启动与验证 | [STARTUP-AND-VERIFICATION.md](./STARTUP-AND-VERIFICATION.md) |
| 接口列表 | [API-AND-CLASS-REFERENCE.md](./API-AND-CLASS-REFERENCE.md) |
| 发信验证（Postman） | [POSTMAN-SMTP-VERIFY.md](./POSTMAN-SMTP-VERIFY.md) |
| Scheduler Docker 触发 | [SCHEDULER-DOCKER-TEST.md](./SCHEDULER-DOCKER-TEST.md) |
| 关闭服务 | [SHUTDOWN.md](./SHUTDOWN.md) |
| 发布前检查 | [RELEASE-CHECKLIST.md](./RELEASE-CHECKLIST.md) |
| 问题修复记录 | [BUGFIX-LOG.md](./BUGFIX-LOG.md) |
| 网关 JWT 与请求头 | `gateway-service/.../JwtAuthFilter.java` |
| 活动创建与 created_by | `campaign-service/.../CampaignController.java` |
| 发信按用户 SMTP | `delivery-service/.../SendTaskConsumer.java`、`PrepareSendService.java` |

---

*文档更新后，日常以本文件与 [docs/README.md](./README.md) 为入口即可快速定位各说明与脚本。*
