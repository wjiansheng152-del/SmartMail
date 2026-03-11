# SmartMail 后端功能完成情况总结

本文档概括当前后端**已完成的功能**与**尚未完成或仅为占位/骨架的部分**，便于迭代规划与验收。

---

## 一、已完成功能

### 1. 基础设施与网关

| 模块 | 完成内容 |
|------|----------|
| **网关 (gateway)** | 统一入口 8080；路由到各微服务；JWT 校验（放行登录/刷新）；请求头注入 X-User-Id、X-Tenant-Id、X-Username；限流过滤器（RateLimitFilter） |
| **IAM (iam)** | 登录（username/password）→ accessToken + refreshToken；刷新 Token；JWT 签发与校验；默认用户初始化（admin/admin123，DataInitializer）；平台库用户表 sys_user |
| **公共 (common)** | 统一响应 Result、异常 ErrorResponse/ErrorCode/BizException、全局异常处理 GlobalExceptionHandler；租户上下文 TenantContext + TenantContextFilter（按 X-Tenant-Id 路由） |

### 2. 客户与联系人 (contact)

| 功能 | 说明 |
|------|------|
| 客户 CRUD | 创建、按 ID 查询、分页（支持按 groupId 过滤）、更新（PUT）、删除 |
| 客户分组 | 创建、按 ID 查询、列表、删除（ContactGroup，ruleType 默认 static） |
| 分组成员 | 将客户加入分组（单条/批量）、从分组移出客户（单条/批量）；ContactGroupMember、ContactGroupMemberService |
| 退订 | 添加退订邮箱（幂等）、检查是否已退订、GET /list 返回已退订邮箱列表（供发送前批量过滤） |
| 黑名单 | 添加、检查、列表 |
| 数据与多租户 | 租户数据源路由（TenantRoutingDataSource）；tenant_default 下 contact、contact_group、contact_group_member、tag、contact_tag、unsubscribe、blacklist 等表；MybatisPlusConfig 显式 Mapper 扫描 |

### 3. 邮件模板 (template)

| 功能 | 说明 |
|------|------|
| 模板 CRUD | 创建、按 ID 查询、全量列表、全量更新、删除 |
| 数据 | EmailTemplate（name、subject、bodyHtml、variables、version）；租户库 email_template 表 |

### 4. 营销活动 (campaign)

| 功能 | 说明 |
|------|------|
| 活动管理 | 创建、按 ID 查询、列表、全量更新、删除；删除时应用层先删 campaign_ab_assignment 再删 campaign；Campaign（name、templateId、groupId、status 默认 draft、**created_by** 来自 X-User-Id，供发信时按创建人取 SMTP） |
| A/B 测试 | AbTestService 解析 abConfig、分配 variant（CampaignAbAssignment）；campaign_ab_assignment 表 |
| 数据 | 租户库 campaign、campaign_batch 表 |

### 5. 调度 (scheduler)

| 功能 | 说明 |
|------|------|
| 计划持久化 | POST 创建计划落库 schedule_job（campaignId、cronExpr、runAt），返回计划 ID；GET /list 查询当前租户计划列表 |
| 定时触发 | 每分钟扫描 run_at 已到且 status=pending 的计划，向队列 smartmail.campaign.trigger 投递 CampaignTriggerPayload（campaignId、tenantId、scheduleId），并更新计划状态为 running/failed |
| 数据与依赖 | 租户数据源 + schedule_job 表；RabbitMQ 触发队列与交换机 |

### 6. 投递 (delivery)

| 功能 | 说明 |
|------|------|
| 状态查询 | GET /status/{campaignId} 按 campaign_batch 汇总该活动下各批次的 total、sent、failed |
| **用户 SMTP 配置** | GET /smtp-config：按 X-User-Id 查询当前用户在本租户下的 SMTP 配置（密码脱敏）；PUT /smtp-config：保存/更新当前用户配置（密码 AES 加密落库）。发送时按活动创建人（campaign.created_by）取该用户的 smtp_config，有则用其动态构建 JavaMailSender 发信，无则用默认通道（如 MailHog） |
| 发送能力 | SendTaskConsumer 消费 smartmail.send.task；若 SendTaskPayload 带 smtpConfigUserId 则按用户 SMTP 发信，否则调用 SendStrategy 默认通道；回写 delivery_task 状态与 campaign_batch 的 success_count/fail_count |
| 活动触发消费 | CampaignTriggerConsumer 消费 smartmail.campaign.trigger：拉取活动/模板/分组联系人，过滤退订与黑名单，创建 campaign_batch 与 delivery_task，向发送队列投递 SendTaskPayload（含 deliveryId、batchId、campaignId、smtpConfigUserId 等），邮件正文中注入打开追踪像素 URL |
| 数据 | 租户库 delivery_task（含 batch_id）、campaign_batch、**smtp_config**（按 user_id 存用户 SMTP）；DownstreamClient 调用 contact/template/campaign 服务获取数据；campaign 表含 created_by（活动创建人） |

### 7. 追踪 (tracking)

| 功能 | 说明 |
|------|------|
| 像素 | GET /pixel/{deliveryId}?campaignId=xxx 返回 1x1 透明 GIF，并写入 tracking_event（eventType=open） |
| 点击 | GET /click/{deliveryId}?url=xxx&campaignId=xxx 302 重定向到 url，并写入 tracking_event（eventType=click，linkUrl） |
| 统计 | GET /stats/{campaignId} 从 tracking_event 按 campaignId + eventType 汇总 openCount、clickCount |

### 8. 审计 (audit)

| 功能 | 说明 |
|------|------|
| 审计日志 | 写入单条、分页查询（可按 userId 过滤）；AuditLog（userId、action、resource、resourceId、detail、createTime） |
| 数据 | 租户库 audit_log 表 |

### 9. 数据库与部署

| 项目 | 说明 |
|------|------|
| 库与表 | platform 库（sys_user、tenant_metadata）；租户库 tenant_default（contact、template、campaign、tracking、audit、delivery、scheduler、smtp_config、abtest、unsubscribe、blacklist 等 DDL） |
| Docker | 各服务 Dockerfile；docker-compose 一键启动（MySQL、Redis、RabbitMQ、MailHog、各微服务）；依赖服务与业务服务同属 smartmail 网络；application-docker.yml 使用 mysql:3306 + root |
| 文档 | [docs/README.md](README.md)（文档索引）；API-AND-CLASS-REFERENCE.md（接口与重要类）；STARTUP-AND-VERIFICATION.md（启动与验证）；PROJECT-STATUS.md（项目状态汇总）；BUGFIX-LOG.md（问题修复记录）；RELEASE-CHECKLIST.md（发布检查） |

---

## 二、未完成或占位部分

### 1. 活动状态与发送（可选增强）

- **活动状态流转**：Campaign 有 status（draft/scheduled/sending/done），可增加“提交发送”“取消发送”等状态流转及与 scheduler 的显式联动（当前调度到点即触发，不依赖活动 status）。

### 2. 其他可选增强

- **多租户**：当前仅 default → tenant_default 单租户数据源；多租户元数据（tenant_metadata）与动态 schema 路由可扩展。
- **限流与安全**：网关限流已存在，可补充按用户/租户的限流策略；生产环境 JWT/数据库等敏感配置需环境变量或配置中心。
- **健康检查**：docker-compose 中 MySQL/RabbitMQ 等有 healthcheck，各 Java 服务可增加健康/就绪端点便于编排。
- **单元/集成测试**：部分模块有单测（如 IAM、Campaign AbTest），可补充 contact、template、delivery、tracking 等关键路径测试。

---

## 三、小结表

| 领域 | 接口与基础能力 | 持久化 | 与发送/追踪联动 |
|------|----------------|--------|------------------|
| IAM | ✅ 登录、刷新、JWT | ✅ sys_user | - |
| 客户/联系人 | ✅ CRUD、分组、退订、黑名单、退订列表 | ✅ | - |
| 模板 | ✅ CRUD | ✅ | - |
| 活动 | ✅ CRUD、A/B 分配 | ✅ | ✅ 由调度触发发送 |
| 调度 | ✅ 创建/列表、计划落库、定时触发 MQ | ✅ schedule_job | ✅ 投递 CampaignTriggerPayload |
| 投递 | ✅ 状态汇总、**用户 SMTP 配置 GET/PUT**、MQ 消费、回写状态、触发消费、按用户 SMTP 发信 | ✅ delivery_task、campaign_batch、smtp_config | ✅ 消费触发并生产发送任务 |
| 追踪 | ✅ 像素、点击、统计、pixel/click 落库 | ✅ tracking_event | - |
| 审计 | ✅ 写入、分页查询 | ✅ | - |

**结论**：后端已完成认证、客户、模板、活动、调度、投递、追踪、审计的闭环：调度计划持久化并定时投递活动触发消息，delivery 消费后拉取活动/模板/联系人并过滤退订与黑名单，创建批次与投递任务并投递发送队列，发送结果回写 delivery_task 与 campaign_batch，追踪 pixel/click 写入 tracking_event 供统计。尚未完成的主要为可选增强：活动状态显式流转、Cron 周期触发、多租户扩展等。
