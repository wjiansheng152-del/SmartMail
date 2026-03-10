# SmartMail 后端发布前检查清单

## 1. 构建与测试

- [ ] 根目录执行 `./mvnw.cmd package -DskipTests` 或 `./mvnw.cmd verify` 通过
- [ ] 各模块单元测试通过：`./mvnw.cmd test`
- [ ] 关键路径集成测试通过（IAM 登录、Gateway 转发）

## 2. 代码与规范

- [ ] 符合阿里巴巴 Java 开发手册（嵩山版）
- [ ] SQL/建表/索引符合项目规约（无外键、count(*)、小写等）
- [ ] 接口符合 RESTful 与项目接口规范（/api/{srv}/{apiCategory}、errorCode/errorInfo）

## 3. 配置与安全

- [ ] JWT 密钥与敏感配置不写死，使用环境变量或配置中心
- [ ] 生产环境关闭 H2、关闭 JPA ddl-auto 或使用 Flyway/Liquibase
- [ ] 生产 MySQL/Redis/RabbitMQ 使用独立实例与账号

## 4. 容器化

- [ ] 各服务 Dockerfile 从仓库根目录可成功构建
- [ ] docker-compose 可启动全部依赖与服务
- [ ] 健康检查与就绪探针已配置（可选）

## 5. 数据库

- [ ] 执行 `docs/sql/schema-platform.sql`、`schema-tenant-default.sql` 及各业务表 DDL
- [ ] 租户 schema 与 platform 库初始化完成

## 6. 契约与兼容性

- [ ] 对外 API 变更已记录，必要时做契约测试
- [ ] 前端或调用方已知晓接口变更

## 7. 监控与日志

- [ ] 关键操作有日志（发送失败、登录失败等）
- [ ] 审计日志可追溯（audit-service）

---

完成以上项后即可打 tag 并部署。
