# 真实 SMTP 验证：创建新活动与调度（详细步骤）

为保证发信走**用户 SMTP** 而不是 MailHog，必须用**已登录用户**创建活动（这样 `created_by` 才有值），再创建调度触发发送。下面给出每一步的 **POST 请求** 和 **终端/SQL 命令**（PowerShell + Docker 环境）。

---

## 前置：先登录拿 Token

所有带 `Authorization` 的请求都要用**同一用户**的 Token（你配置 SMTP 的那个用户，如 admin）。

**请求**

- **Method**：`POST`
- **URL**：`http://localhost:8080/api/iam/auth/login`
- **Headers**：`Content-Type: application/json`
- **Body（raw JSON）**：

```json
{
  "username": "admin",
  "password": "admin123"
}
```

响应里记下 `data.accessToken`，后面所有请求的 Header 里加：

`Authorization: Bearer <这里粘贴 accessToken>`

---

## 一、创建新活动（必须带 Token，这样 created_by 才有值）

### 1. POST 创建活动

- **Method**：`POST`
- **URL**：`http://localhost:8080/api/campaign/campaign`
- **Headers**：
  - `Content-Type: application/json`
  - `Authorization: Bearer <你的 accessToken>`
- **Body（raw JSON）**：把 `templateId`、`groupId` 换成你已有的模板 ID、分组 ID（没有则先按下面「可选：准备模板/分组/联系人」创建）：

```json
{
  "name": "真实SMTP验证活动",
  "templateId": 1,
  "groupId": 1,
  "status": "draft"
}
```

- **记下响应里的 `data.id`**，即 **campaignId**（例如 `2`）。后面创建调度和查投递状态都要用这个 ID。

### 2. 用终端确认活动的 created_by 已写入（可选）

在项目根目录（如 `D:\CursorProgram\SmartMail`）执行：

```powershell
docker exec -i smartmail-mysql-1 mysql -uroot -proot -D tenant_default -e "SELECT id, name, created_by, template_id, group_id, status, create_time FROM campaign ORDER BY id DESC LIMIT 3;"
```

确认你刚创建的活动 `created_by` 不为 NULL（例如为 `1`）。若为 NULL，说明创建活动时**没有带**或**没有通过网关带** `Authorization: Bearer <token>`，需用带 Token 的请求重新创建活动。

---

## 二、第 3 步：创建发送计划（POST）并触发发送

### 1. 获取当前 UTC 时间（用于 runAt）

在终端执行：

```powershell
docker exec smartmail-scheduler-1 date "+%Y-%m-%d %H:%M:%S"
```

示例输出：`2026-03-11 02:15:00`。把该时间**加 2～3 分钟**作为 `runAt`，例如 `2026-03-11 02:18:00`。

### 2. POST 创建发送计划

- **Method**：`POST`
- **URL**：`http://localhost:8080/api/scheduler/schedule`
- **Headers**：
  - `Content-Type: application/json`
  - `Authorization: Bearer <你的 accessToken>`
- **Body（raw JSON）**：把 `campaignId` 换成上面创建活动得到的 **campaignId**，`runAt` 换成上一步算好的 **UTC 时间**：

```json
{
  "campaignId": 2,
  "cronExpr": "",
  "runAt": "2026-03-11 02:18:00"
}
```

- **runAt 必须为 UTC**，不能填本地时间，否则调度可能一直不触发。
- 响应 200 且 `data` 为数字即表示计划创建成功。到点后 scheduler 会投递触发消息，delivery 消费后按活动的 `created_by` 取用户 SMTP 发信。

### 3. 等待约 2 分钟后查投递状态

- **Method**：`GET`
- **URL**：`http://localhost:8080/api/delivery/delivery/status/<campaignId>`
  - 例如：`http://localhost:8080/api/delivery/delivery/status/2`
- **Headers**：`Authorization: Bearer <你的 accessToken>`（无 Body）

若返回中 `sent >= 1` 表示已发信；再到**真实收件箱**（或 MailHog `http://localhost:8025`）查收。

---

## 三、终端里要执行的 SQL（将联系人加入分组）

当前没有 REST 接口「把联系人加入分组」，需要在 MySQL 里插入 `contact_group_member`。

### 1. 在租户库插入一条记录（PowerShell，Docker MySQL）

在项目根目录执行（把 `groupId`、`contactId` 换成你实际的分组 ID、联系人 ID）：

```powershell
docker exec -i smartmail-mysql-1 mysql -uroot -proot -D tenant_default -e "INSERT INTO contact_group_member (group_id, contact_id, create_time) VALUES (1, 1, NOW());"
```

- 若已存在同一条 `(group_id, contact_id)` 会报错 `Duplicate entry`，可忽略或先查是否已有：
  ```powershell
  docker exec -i smartmail-mysql-1 mysql -uroot -proot -D tenant_default -e "SELECT * FROM contact_group_member;"
  ```

### 2. 可选：直接执行的 SQL 语句（在任意 MySQL 客户端里对 tenant_default 执行）

```sql
INSERT INTO tenant_default.contact_group_member (group_id, contact_id, create_time)
VALUES (1, 1, NOW());
```

同样把 `1, 1` 换成你的 `groupId`、`contactId`。

---

## 四、可选：准备模板、分组、联系人（若还没有）

若还没有模板、分组、联系人，按下面顺序做（每个请求都带 `Content-Type: application/json` 和 `Authorization: Bearer <accessToken>`）：

| 步骤 | Method | URL | Body 示例 |
|------|--------|-----|-----------|
| 模板 | POST | `http://localhost:8080/api/template/template` | `{"name":"真实SMTP验证","subject":"【SmartMail】真实 SMTP 验证邮件","bodyHtml":"<p>您好，这是一封通过真实 SMTP 发送的验证邮件。</p>","variables":""}` |
| 分组 | POST | `http://localhost:8080/api/contact/group` | `{"name":"真实SMTP验证分组","ruleType":"static"}` |
| 联系人 | POST | `http://localhost:8080/api/contact/contact` | `{"email":"你的收件邮箱@xxx.com","name":"测试收件人"}` |

然后执行上面的 SQL/终端命令，把该联系人加入分组，再创建活动和计划。

---

## 五、终端命令速查（PowerShell）

| 用途 | 命令 |
|------|------|
| 查活动及 created_by | `docker exec -i smartmail-mysql-1 mysql -uroot -proot -D tenant_default -e "SELECT id, name, created_by, template_id, group_id FROM campaign ORDER BY id DESC LIMIT 5;"` |
| 取 UTC 时间（用于 runAt） | `docker exec smartmail-scheduler-1 date "+%Y-%m-%d %H:%M:%S"` |
| 联系人加入分组 | `docker exec -i smartmail-mysql-1 mysql -uroot -proot -D tenant_default -e "INSERT INTO contact_group_member (group_id, contact_id, create_time) VALUES (1, 1, NOW());"` |
| 查计划是否到点 | `docker exec -i smartmail-mysql-1 mysql -uroot -proot -D tenant_default -e "SELECT id, campaign_id, run_at, status FROM schedule_job ORDER BY id DESC LIMIT 5;"` |

---

**总结**：创建新活动时必须带 **Authorization: Bearer &lt;accessToken&gt;**，活动才会有 `created_by`；第 3 步用 **POST /api/scheduler/schedule** 且 **runAt 用 UTC**；联系人入组用上述 **SQL/终端命令** 即可。
