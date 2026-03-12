# SmartMail 文档中心

本文档目录包含项目的启动与验证、接口说明、发信验证、发布检查及历史问题记录等。**当前所有接口工作正常**，可按需查阅下表。

---

## 文档索引

### 入门与运行

| 文档 | 说明 |
|------|------|
| [STARTUP-AND-VERIFICATION.md](./STARTUP-AND-VERIFICATION.md) | **首选**：环境要求、数据库初始化顺序、Docker/本地启动方式、登录与接口验证、SMTP/MailHog 发信验证步骤 |
| [SHUTDOWN.md](./SHUTDOWN.md) | 关闭服务：Docker Compose 停止/删除、本地 IDE 与按端口结束进程、混合部署关闭顺序 |

### 接口与架构

| 文档 | 说明 |
|------|------|
| [API-AND-CLASS-REFERENCE.md](./API-AND-CLASS-REFERENCE.md) | 对外 REST 接口列表（IAM、contact、template、campaign、scheduler、delivery、tracking、audit）及重要类说明 |
| [BACKEND-SUMMARY.md](./BACKEND-SUMMARY.md) | 后端功能完成情况：已完成模块小结、未完成/占位与可选增强 |

### 发信与调度验证

| 文档 | 说明 |
|------|------|
| [POSTMAN-SMTP-VERIFY.md](./POSTMAN-SMTP-VERIFY.md) | 使用 Postman 逐步完成「用户 SMTP 配置 → 真实邮箱发信」验证 |
| [SMTP-VERIFY-STEPS-DETAIL.md](./SMTP-VERIFY-STEPS-DETAIL.md) | 创建活动与调度、终端/SQL 命令详解（必须带 Token 创建活动以保证 created_by） |
| [SCHEDULER-DOCKER-TEST.md](./SCHEDULER-DOCKER-TEST.md) | Scheduler 在 Docker 内到点触发的专项测试：构建、启动、验证与故障排查 |
| [docs/scripts/verify-smtp-send.ps1](./scripts/verify-smtp-send.ps1) | 一键脚本：登录 → 创建模板/分组/联系人/活动/计划（runAt=当前+2 分钟）→ 等待后查投递状态并提示打开 MailHog |

### 发布与问题追溯

| 文档 | 说明 |
|------|------|
| [RELEASE-CHECKLIST.md](./RELEASE-CHECKLIST.md) | 发布前检查清单：构建测试、代码规范、配置安全、容器化、数据库、契约与日志 |
| [BUGFIX-LOG.md](./BUGFIX-LOG.md) | 问题修复记录：Docker/MyBatis/PowerShell/SQL/发信/created_by 等历史问题与修复说明 |
| [PROJECT-STATUS.md](./PROJECT-STATUS.md) | 项目当前状态汇总：进展、接口状态、待办与文档速查 |
| [AI-开发协作规范.md](./AI-开发协作规范.md) | AI 协作开发总规范：流程、门禁、角色分工、模板与完成定义 |
| [AI-岗位任务替代与协作边界.md](./AI-岗位任务替代与协作边界.md) | UI/前端/后端/移动/测试/运维/DBA 与 AI 的任务边界与职责划分 |
| [AI-后端工程师使用指南.md](./AI-后端工程师使用指南.md) | 后端工程师详细 AI 使用手册：流程、提示词、门禁、排障与落地建议 |
| [AI-前端工程师使用指南.md](./AI-前端工程师使用指南.md) | 前端工程师详细 AI 使用手册：流程、提示词、联调闭环与质量门禁 |

---

## 其他资源

| 文档 | 说明 |
|------|------|
| [GIT-REMOVE-SERVER.md](./GIT-REMOVE-SERVER.md) | 从 Git 仓库中删除已移除的 `server/` 目录的提交与推送步骤（一次性操作） |

### SQL 脚本（docs/sql/）

按 [STARTUP-AND-VERIFICATION.md](./STARTUP-AND-VERIFICATION.md) 第二节「数据库初始化」**顺序**执行：

1. 平台与租户 Schema：`schema-platform.sql`、`schema-tenant-default.sql`
2. 租户业务表：`tenant_default-contact.sql`、`tenant_default-template.sql`、`tenant_default-campaign.sql`、`tenant_default-scheduler.sql`、`tenant_default-delivery.sql`、`tenant_default-smtp_config.sql`、`tenant_default-tracking.sql`、`tenant_default-abtest.sql`、`tenant_default-unsubscribe-blacklist.sql`、`tenant_default-audit.sql`
3. 迁移脚本（按需）：`tenant_default-campaign-migration-created_by.sql`、`tenant_default-delivery-migration-batch_id.sql`

---

## 推荐阅读顺序

- **首次搭建**：STARTUP-AND-VERIFICATION → 执行 sql → 启动服务 → 登录与接口验证  
- **验证发信**：STARTUP-AND-VERIFICATION（方式 A/B）或 POSTMAN-SMTP-VERIFY / verify-smtp-send.ps1  
- **仅验证 Docker 内 Scheduler 触发**：SCHEDULER-DOCKER-TEST  
- **开发/联调**：API-AND-CLASS-REFERENCE、BACKEND-SUMMARY  
- **发布前**：RELEASE-CHECKLIST  
- **排错**：BUGFIX-LOG、对应验证文档中的故障排查小节  
