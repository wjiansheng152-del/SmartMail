# SmartMail

营销邮件与触达平台：网关、IAM、联系人/模板/活动、调度、投递（含用户 SMTP）、追踪、审计。支持 Docker Compose 一键启动与本地 IDE 运行。

---

## 快速开始

1. **环境**：JDK 17+、Maven、Docker（可选）。MySQL 8.0、RabbitMQ、Redis、MailHog 见文档。
2. **数据库**：按顺序执行 `docs/sql/` 下脚本（平台库 → 租户 Schema → 各租户业务表），详见 [docs/STARTUP-AND-VERIFICATION.md](docs/STARTUP-AND-VERIFICATION.md)。
3. **启动**：
   - Docker：`docker compose up -d`（项目根目录）
   - 本地：先启动 IAM、Gateway，再启动各业务服务（见文档）。
4. **验证**：登录 `POST http://localhost:8080/api/iam/auth/login`，带 Token 调用各业务接口；发信验证见文档「方式 A/B」。

---

## 文档入口

**所有说明与脚本集中在 [docs/README.md](docs/README.md)（文档中心）。**

| 用途 | 文档 |
|------|------|
| 启动、验证、发信 | [STARTUP-AND-VERIFICATION.md](docs/STARTUP-AND-VERIFICATION.md) |
| 接口列表与重要类 | [API-AND-CLASS-REFERENCE.md](docs/API-AND-CLASS-REFERENCE.md) |
| 项目状态与待办 | [PROJECT-STATUS.md](docs/PROJECT-STATUS.md) |
| 关闭服务 | [SHUTDOWN.md](docs/SHUTDOWN.md) |
| 发布前检查 | [RELEASE-CHECKLIST.md](docs/RELEASE-CHECKLIST.md) |

当前所有接口工作正常；更多索引见 [docs/README.md](docs/README.md)。
