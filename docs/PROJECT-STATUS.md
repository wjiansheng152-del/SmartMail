# SmartMail 项目状态汇总

本文档汇总当前项目整体状态、接口与发信链路、待办及文档速查。**当前所有接口工作正常**，Docker 与本地启动、调度触发与发信链路均已验证通过。

---

## 一、当前状态

### 1. 接口与功能

| 领域 | 状态 | 说明 |
|------|------|------|
| 网关 / IAM | ✅ 正常 | 统一入口 8080，JWT 鉴权，登录/刷新，请求头注入 X-User-Id / X-Tenant-Id / X-Username |
| 客户 / 联系人 | ✅ 正常 | 客户 CRUD（含更新）、分组、加入/移出分组（单条与批量）、退订、黑名单 |
| 模板 | ✅ 正常 | 模板 CRUD |
| 活动 | ✅ 正常 | 活动 CRUD（含删除）；创建活动需带 Token，created_by 正确落库，供发信时按用户 SMTP |
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

1. **delivery 调用 contact 的 unsubscribe/list**：该接口 404 时 delivery 已容错，可后续在 contact 补充或统一退订数据来源。
2. 活动状态显式流转、Cron 周期触发、多租户扩展、健康检查与补充单测等，见 [BACKEND-SUMMARY.md](./BACKEND-SUMMARY.md)。

---

## 三、近期更新汇总（本会话）

以下为本次会话中完成的新增功能与修复，已纳入当前状态。

### 新增功能

| 领域 | 功能 | 说明 |
|------|------|------|
| **客户 / 模板 / 分组 / 活动** | 对外 ID 统一为 local_id | 同一账号下 id 从 1 连续、与创建时间相关；删除后新记录可填补空缺。接口路径与响应中的 id 均为 local_id（Integer）。 |
| **分组成员** | groupId / contactId 按 local_id | 加入/移出分组接口的 path 与 body 中的 groupId、contactId 均为对应资源的 local_id，后端解析为内部主键再写库。 |
| **客户分页** | 按分组筛选用 local_id | 传 groupId 时表示分组的 local_id；ContactServiceImpl.pageList 内解析为内部 group id 再查 contact_group_member。 |
| **调度 / 投递** | 计划带 createdBy，投递按 local_id 查活动 | 创建计划请求体增加 createdBy；CampaignTriggerPayload 携带 createdBy；delivery 请求 campaign 时带 X-User-Id，campaign 按 local_id + createdBy 查询，确保触发的是前端显示的那条活动。 |
| **定时发送** | 列表按用户隔离 | GET /api/scheduler/schedule/list 若请求头带 X-User-Id 则仅返回该用户创建的计划（created_by = 用户ID）。 |
| **发送计划** | 计划 ID 从 1 开始按用户隔离 | schedule_job 表增加 local_id；创建时分配 nextLocalIdForCreatedBy；列表与创建接口返回的「计划 ID」为 local_id，每用户独立从 1 起、删除后可复用。 |

### 修复的 Bug

| 问题 | 原因 | 修复 |
|------|------|------|
| 登录后客户/模板/分组/活动接口 500 | 数据库未执行 local_id 迁移，SQL 查不到 local_id 列 | 按顺序执行 docs/sql 下 contact、template、contact_group、campaign 的 local_id 迁移脚本并重启服务。 |
| 加入分组返回 200 但分组内看不到该客户 | 客户分页按分组筛选时，前端传的 groupId 为分组的 local_id，后端误当内部主键查询 | ContactServiceImpl.pageList 中先将 groupId（local_id）解析为内部 group id，再调用 selectPageByGroupId。 |
| 立即发送/计划触发到错误活动 | delivery 调 campaign 未带 X-User-Id，campaign 将路径 id 当内部主键查 | schedule_job 增加 created_by；触发 payload 与 delivery 传 createdBy；DownstreamClient.getCampaign 带 X-User-Id。 |
| 定时发送下列表未按用户隔离 | 调度列表接口返回全量计划 | SchedulerController.list 读取 X-User-Id，ScheduleJobService.listByCreatedBy(createdBy) 按 created_by 过滤。 |

### 数据库迁移脚本（local_id / created_by）

- `tenant_default-contact-migration-local_id.sql`
- `tenant_default-template-migration-local_id.sql`
- `tenant_default-contact_group-migration-local_id.sql`
- `tenant_default-campaign-migration-local_id.sql`
- `tenant_default-scheduler-migration-created_by.sql`
- `tenant_default-scheduler-migration-local_id.sql`

执行顺序：在对应表已存在的前提下，先 created_by（若有），再各 local_id 迁移；详见 STARTUP-AND-VERIFICATION.md。

### 后端接口与前端功能（同上会话已完成）

- **contact-service**：PUT `/api/contact/contact/{id}` 更新客户；POST/DELETE 分组成员与批量（groupId/contactId 为 local_id）；ContactGroupMember 及相关 DTO。
- **campaign-service**：DELETE `/api/campaign/campaign/{id}` 删除活动。
- **前端**：客户列表编辑/加入分组/移出分组/排序/搜索；分组管理编辑、当前成员与添加客户批量操作；营销活动删除按钮。

---

## 四、文档与关键文件速查

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
