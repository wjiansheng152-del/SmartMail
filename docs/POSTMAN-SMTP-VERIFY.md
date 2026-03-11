# 使用 Postman 验证真实 SMTP 发信

本文档按步骤说明如何用 **Postman** 完成「用户 SMTP 配置 → 真实邮箱发信」的验证。所有请求统一发往**网关**：`http://localhost:8080`。

---

## 一、前置条件

- 网关、IAM、contact、template、campaign、scheduler、delivery、MySQL、RabbitMQ 已启动；
- 租户库建表已完成（含 `smtp_config`）；
- 已准备好**真实 SMTP 信息**（如 QQ 邮箱：在 QQ 邮箱网页版 → 设置 → 账户 → 开启 SMTP → 生成授权码；163 同理）。

---

## 二、Postman 基础设置

### 1. 环境变量（推荐）

在 Postman 中新建 Environment，例如命名为 `SmartMail`，添加：

| 变量名 | 初始值 | 说明 |
|--------|--------|------|
| `baseUrl` | `http://localhost:8080` | 网关地址 |
| `token` | （留空） | 登录后从脚本或 Tests 中写入 |

后续请求的 URL 可填：`{{baseUrl}}/api/iam/auth/login`，认证头填：`Bearer {{token}}`。

### 2. 请求头约定

- **登录、刷新**：只需 `Content-Type: application/json`。
- **其他所有接口**：需同时带：
  - `Content-Type: application/json`
  - `Authorization: Bearer <accessToken>`（把 `<accessToken>` 换成步骤 1 登录返回的 `data.accessToken`；若用环境变量则填 `Bearer {{token}}`）。

---

## 三、逐步操作（真实 SMTP 验证）

### 步骤 1：登录获取 Token

**请求**

- **Method**：`POST`
- **URL**：`{{baseUrl}}/api/iam/auth/login` 或 `http://localhost:8080/api/iam/auth/login`
- **Headers**：`Content-Type: application/json`
- **Body**：选择 **raw** → **JSON**，内容：

```json
{
  "username": "admin",
  "password": "admin123"
}
```

**响应**：200 时 Body 中会有 `data.accessToken`、`data.refreshToken`。

**在 Postman 中保存 Token（可选）**：在该请求的 **Tests** 里写：

```javascript
var json = pm.response.json();
if (json.data && json.data.accessToken) {
  pm.environment.set("token", json.data.accessToken);
}
```

发送后，环境变量 `token` 会自动更新，后续请求用 `Authorization: Bearer {{token}}` 即可。

---

### 步骤 2：配置用户 SMTP（真实发信邮箱）

**请求**

- **Method**：`PUT`
- **URL**：`{{baseUrl}}/api/delivery/smtp-config` 或 `http://localhost:8080/api/delivery/smtp-config`
- **Headers**：`Content-Type: application/json`，`Authorization: Bearer {{token}}`（或粘贴步骤 1 的 accessToken）
- **Body**：**raw** → **JSON**，按你的邮箱修改后发送（QQ 示例）：

```json
{
  "host": "smtp.qq.com",
  "port": 465,
  "username": "你的QQ邮箱@qq.com",
  "password": "QQ邮箱SMTP授权码",
  "fromEmail": "你的QQ邮箱@qq.com",
  "fromName": "SmartMail 测试",
  "useSsl": true
}
```

**163 邮箱示例**：

```json
{
  "host": "smtp.163.com",
  "port": 465,
  "username": "你的163邮箱@163.com",
  "password": "163 SMTP授权码",
  "fromEmail": "你的163邮箱@163.com",
  "fromName": "SmartMail 测试",
  "useSsl": true
}
```

**响应**：200 且 `data` 中有配置信息（密码以 `****` 返回）即表示保存成功。**发信时会按「活动创建人」用该用户的这份 SMTP 配置**，因此下面创建活动时务必用同一账号的 Token。

---

### 步骤 3：创建邮件模板

- **Method**：`POST`
- **URL**：`{{baseUrl}}/api/template/template`
- **Headers**：`Content-Type: application/json`，`Authorization: Bearer {{token}}`
- **Body**：**raw** → **JSON**：

```json
{
  "name": "真实SMTP验证",
  "subject": "【SmartMail】真实 SMTP 验证邮件",
  "bodyHtml": "<p>您好，这是一封通过真实 SMTP 发送的验证邮件。</p>",
  "variables": ""
}
```

**记下响应里的 `data.id`**，例如 `1`，后面作为 **templateId**。

---

### 步骤 4：创建客户分组

- **Method**：`POST`
- **URL**：`{{baseUrl}}/api/contact/group`
- **Headers**：同上
- **Body**：**raw** → **JSON**：

```json
{
  "name": "真实SMTP验证分组",
  "ruleType": "static"
}
```

**记下响应里的 `data.id`**，例如 `1`，作为 **groupId**。

---

### 步骤 5：创建联系人（收件人）

- **Method**：`POST`
- **URL**：`{{baseUrl}}/api/contact/contact`
- **Headers**：同上
- **Body**：**raw** → **JSON**（邮箱改成你要收信的地址）：

```json
{
  "email": "你要接收验证邮件的邮箱@xxx.com",
  "name": "测试收件人"
}
```

**记下响应里的 `data.id`**，作为 **contactId**。  
**重要**：真实 SMTP 验证时，这里填的邮箱必须是你**能登录查看的邮箱**，否则无法确认是否收到。

---

### 步骤 6：将联系人加入分组（执行 SQL）

当前没有「添加成员到分组」的 REST 接口，需要在数据库执行一条 SQL（把下面的 `groupId`、`contactId` 换成你在步骤 4、5 记下的 ID）：

```sql
INSERT INTO tenant_default.contact_group_member (group_id, contact_id, create_time)
VALUES (1, 1, NOW());
```

若用 Docker MySQL，可在终端执行（容器名按实际修改）：

```powershell
docker exec -i smartmail-mysql-1 mysql -uroot -proot -e "INSERT INTO tenant_default.contact_group_member (group_id, contact_id, create_time) VALUES (1, 1, NOW());"
```

执行成功后，再在 Postman 继续下一步。

---

### 步骤 7：创建营销活动

- **Method**：`POST`
- **URL**：`{{baseUrl}}/api/campaign/campaign`
- **Headers**：同上（**必须带步骤 1 同一用户的 Token**，这样活动的 `createdBy` 才会对应用户，发信时才会用你在步骤 2 配置的 SMTP）
- **Body**：**raw** → **JSON**（把 `templateId`、`groupId` 换成 3、4 步的 id）：

```json
{
  "name": "真实SMTP验证活动",
  "templateId": 1,
  "groupId": 1,
  "status": "draft"
}
```

**记下响应里的 `data.id`**，作为 **campaignId**（例如 `1`）。

---

### 步骤 8：创建发送计划（定时触发）

- **Method**：`POST`
- **URL**：`{{baseUrl}}/api/scheduler/schedule`
- **Headers**：同上
- **Body**：**raw** → **JSON**（把 `campaignId` 换成步骤 7 的 id；**runAt 必须填 UTC 时间**，见下）。

**重要：runAt 必须使用 UTC，不能填本地时间。** 调度容器内使用 UTC，若填北京时间等，计划会一直被当成“未到点”而从不触发。

获取当前 UTC 时间（在项目根目录执行）：
```powershell
docker exec smartmail-scheduler-1 date
```
例如输出 `Tue Mar 10 07:38:06 UTC 2026`，则 runAt 填**该时间往后 2～3 分钟**，格式 `yyyy-MM-dd HH:mm:ss`：
```json
{
  "campaignId": 1,
  "cronExpr": "",
  "runAt": "2026-03-10 07:41:00"
}
```
**响应**：200 且返回 `data` 为数字表示计划 ID，创建成功。  
调度服务每分钟扫描，到点后会自动触发该活动并发信。

---

### 步骤 9：等待约 2 分钟后查投递状态

- **Method**：`GET`
- **URL**：`{{baseUrl}}/api/delivery/delivery/status/1`（把最后的 `1` 换成你的 **campaignId**）
- **Headers**：`Authorization: Bearer {{token}}`（无需 Body）

**响应示例**：`{ "data": { "campaignId": 1, "total": 1, "sent": 1, "failed": 0 } }`  
若 `sent >= 1` 表示已发信成功。

---

### 步骤 10：在真实收件箱查收

到步骤 5 填写的**收件人邮箱**（含垃圾箱）查看是否收到主题为「【SmartMail】真实 SMTP 验证邮件」的邮件。  
若未收到：检查 delivery 日志、SMTP 配置（端口/授权码/SSL）、收件邮箱是否写错或进垃圾邮件。

---

## 四、Postman 请求一览表

| 顺序 | 方法 | URL 路径 | 说明 |
|------|------|----------|------|
| 1 | POST | `/api/iam/auth/login` | 登录，取 accessToken |
| 2 | PUT | `/api/delivery/smtp-config` | 配置真实 SMTP |
| 3 | POST | `/api/template/template` | 创建模板，记 templateId |
| 4 | POST | `/api/contact/group` | 创建分组，记 groupId |
| 5 | POST | `/api/contact/contact` | 创建联系人，记 contactId |
| 6 | — | 执行 SQL | 将联系人加入分组 |
| 7 | POST | `/api/campaign/campaign` | 创建活动，记 campaignId |
| 8 | POST | `/api/scheduler/schedule` | 创建计划，runAt 设为 1～2 分钟后 |
| 9 | GET | `/api/delivery/delivery/status/{{campaignId}}` | 查投递状态 |

---

## 五、常见问题

- **登录 401**：检查用户名、密码是否为 `admin` / `admin123`（或你已在 IAM 中配置的账号）。
- **接口 401**：检查 Headers 是否带 `Authorization: Bearer <accessToken>`，且 Token 未过期。
- **发信设置 500**：确认租户库已执行 `tenant_default-smtp_config.sql`；若提示表不存在，先建表再重试。
- **收不到邮件**：确认步骤 2 的 SMTP 与授权码正确、步骤 5 的收件邮箱可登录；到垃圾邮件中查找；查看 delivery 服务日志是否有 SMTP 报错。
- **投递状态一直 total=0**：若步骤 9 始终 total=0，常见原因有二。（1）**runAt 用了本地时间**：必须用 UTC，见步骤 8。（2）**Docker 下 delivery 未配置下游地址**：需在 docker-compose 中为 delivery 配置 `APP_DOWNSTREAM_CONTACT_BASE_URL` 等（见 STARTUP-AND-VERIFICATION.md）。可查库确认计划是否到点：`docker exec -i smartmail-mysql-1 mysql -uroot -proot -e "SELECT id, campaign_id, run_at, status FROM tenant_default.schedule_job ORDER BY id DESC LIMIT 5;"`。若 run_at 为 15:xx 而容器时间为 07:xx UTC，说明填了本地时间，需重新创建计划并填 UTC。

按以上步骤在 Postman 中依次请求，即可完成真实 SMTP 发信验证。
