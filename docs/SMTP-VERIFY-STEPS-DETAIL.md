# 真实 SMTP 验证：创建新活动与调度（详细步骤）

为保证发信走**用户 SMTP** 而不是 MailHog，必须用**已登录用户**创建活动（这样 `created_by` 才有值），再创建调度触发发送。下面给出每一步的 **POST 请求** 和 **终端/SQL 命令**（PowerShell + Docker 环境）。

---

## ID 映射与参数说明（必读）

| 场景 | 需要的 ID 类型 | 说明 |
|------|----------------|------|
| **创建活动** Body 中的 `templateId`、`groupId` | **local_id** | 即「创建模板」「创建分组」接口返回的 `data.id`（租户内从 1 连续）。 |
| **创建活动** 响应里的 `data.id`（campaignId） | **local_id** | 活动的对外 ID，创建计划与查投递状态时使用。 |
| **创建计划** Body 中的 `campaignId` | **local_id** | 即上一步创建活动返回的 `data.id`。 |
| **创建计划** Body 中的 `createdBy` | **用户 ID** | 当前登录用户的主键 id（如 admin 一般为 `1`），须与活动的 `created_by` 一致，投递时据此按 local_id 查活动。 |
| **查投递状态** URL 中的 `campaignId` | **local_id** | 与创建活动返回的 id 一致。 |
| **contact_group_member 表** 的 `group_id`、`contact_id` | **内部主键** | 表中存的是数据库主键，**不是** API 返回的 local_id。若该分组/联系人是通过 API 创建的本租户下第一条，内部主键通常也为 1；否则需查表（见下文「三」）。 |

- **local_id**：客户、模板、分组、活动、计划对外接口中的 id 均为 local_id（同一租户或同一用户下从 1 连续）。
- **用户 ID**：登录用户在主库中的主键，可通过登录响应或 IAM 用户表查询；admin 默认多为 1。

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
- **Body（raw JSON）**：`templateId`、`groupId` 填**接口返回的 local_id**（见上文「ID 映射与参数说明」；没有则先按下面「四、可选：准备模板/分组/联系人」创建）：

```json
{
  "name": "真实SMTP验证活动",
  "templateId": 1,
  "groupId": 1,
  "status": "draft"
}
```

- **记下响应里的 `data.id`**，即 **campaignId**（活动的 **local_id**，例如 `1`）。后面创建调度和查投递状态都用这个 ID。

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
- **Body（raw JSON）**：`campaignId` 填上面创建活动得到的 **campaignId（活动 local_id）**；`createdBy` 填**当前登录用户的用户 ID**（与活动 `created_by` 一致，如 admin 为 `1`）；`runAt` 填上一步算好的 **UTC 时间**：

```json
{
  "campaignId": 1,
  "createdBy": 1,
  "cronExpr": "",
  "runAt": "2026-03-11 02:18:00"
}
```

- **createdBy 必填**：投递时按「campaignId（local_id）+ createdBy」查活动，不传会导致触发错误活动或查不到。
- **runAt 时区约定**：
  - 若是**前端界面**点击「立即发送」或在「定时发送」页创建计划，用户选择/输入的是**本地时间**，前端会自动转换为 **UTC 时间** 传给后端，无需手工换算；
  - 若是 **Postman/终端直接调此接口**，`runAt` 必须使用 **UTC 时间**（上一步通过 `docker exec smartmail-scheduler-1 date` 拿到的时间并加几分钟），不能直接用本地北京时间，否则 Docker 中按 UTC 运行的调度服务可能一直不触发或延后数小时触发。
- 响应 200 且 `data` 为数字（计划 **local_id**）即表示计划创建成功。到点后 scheduler 会投递触发消息，delivery 消费后按活动的 `created_by` 取用户 SMTP 发信。

### 3. 等待约 2 分钟后查投递状态

- **Method**：`GET`
- **URL**：`http://localhost:8080/api/delivery/delivery/status/<campaignId>`
  - 其中 **campaignId 为活动的 local_id**（即创建活动返回的 `data.id`），例如：`http://localhost:8080/api/delivery/delivery/status/1`
- **Headers**：`Authorization: Bearer <你的 accessToken>`（无 Body）

若返回中 `sent >= 1` 表示已发信；再到**真实收件箱**（或 MailHog `http://localhost:8025`）查收。

---

## 三、将联系人加入分组（接口优先，SQL 备选）

**推荐方式**是直接调用已有的 REST 接口：

- 单个加入分组：`POST /api/contact/group/{groupId}/member`  
  - Path `groupId`：分组的 **local_id**（步骤 4 返回的 `data.id`）  
  - Body：`{ "contactId": <联系人的 local_id> }`（步骤 5 返回的 `data.id`）
- 批量加入分组：`POST /api/contact/group/{groupId}/member/batch`  
  - Path `groupId`：分组 **local_id**  
  - Body：`{ "contactIds": [ <contact 的 local_id 列表> ] }`

以上两种方式会由后端自动根据 local_id 解析内部主键并写入 `contact_group_member`，**不需要手动执行 SQL**。

下文仅作为需要**直接在数据库造数或排查问题**时的备选方案，说明如何手工插入 `contact_group_member`。**注意**：该表的 `group_id`、`contact_id` 存的是**内部主键**（表的主键 id），**不是** API 返回的 local_id。

### 1. 确定内部主键（若你通过 API 创建的是该租户下第一个分组、第一个联系人，内部主键通常为 1）

若非第一个，需先查内部 id（在 tenant_default 下执行，将 `1` 换成你通过 API 创建时返回的 local_id）：

```sql
-- 按 local_id 查分组的内部主键
SELECT id FROM tenant_default.contact_group WHERE local_id = 1 LIMIT 1;
-- 按 local_id 查联系人的内部主键
SELECT id FROM tenant_default.contact WHERE local_id = 1 LIMIT 1;
```

记下查到的分组的 `id` 为 **内部 group_id**，联系人的 `id` 为 **内部 contact_id**。

### 2. 在租户库插入一条记录（PowerShell，Docker MySQL）

在项目根目录执行（把 `内部 group_id`、`内部 contact_id` 换成上一步查到的值；若是第一个分组和第一个联系人，通常为 `1, 1`）：

```powershell
docker exec -i smartmail-mysql-1 mysql -uroot -proot -D tenant_default -e "INSERT INTO contact_group_member (group_id, contact_id, create_time) VALUES (1, 1, NOW());"
```

- 若已存在同一条 `(group_id, contact_id)` 会报错 `Duplicate entry`，可忽略或先查是否已有：
  ```powershell
  docker exec -i smartmail-mysql-1 mysql -uroot -proot -D tenant_default -e "SELECT * FROM contact_group_member;"
  ```

### 3. 可选：直接执行的 SQL 语句（在任意 MySQL 客户端里对 tenant_default 执行）

```sql
INSERT INTO tenant_default.contact_group_member (group_id, contact_id, create_time)
VALUES (1, 1, NOW());
```

同样把 `1, 1` 换成**内部主键**（见上文「ID 映射与参数说明」与本节 1）。

---

## 四、可选：准备模板、分组、联系人（若还没有）

若还没有模板、分组、联系人，按下面顺序做（每个请求都带 `Content-Type: application/json` 和 `Authorization: Bearer <accessToken>`）。**响应里的 `data.id` 均为 local_id**，创建活动时 `templateId`、`groupId` 即填这些值；联系人加入分组时需用**内部主键**（见「三」）。

| 步骤 | Method | URL | Body 示例 |
|------|--------|-----|-----------|
| 模板 | POST | `http://localhost:8080/api/template/template` | `{"name":"真实SMTP验证","subject":"【SmartMail】真实 SMTP 验证邮件","bodyHtml":"<p>您好，这是一封通过真实 SMTP 发送的验证邮件。</p>","variables":""}` |
| 分组 | POST | `http://localhost:8080/api/contact/group` | `{"name":"真实SMTP验证分组","ruleType":"static"}` |
| 联系人 | POST | `http://localhost:8080/api/contact/contact` | `{"email":"你的收件邮箱@xxx.com","name":"测试收件人"}` |

然后按「三」执行 SQL/终端命令（contact_group_member 用**内部主键**），再创建活动和计划（活动用 templateId/groupId 的 **local_id**，计划用 campaignId **local_id** + **createdBy** 用户 ID）。

---

## 五、终端命令速查（PowerShell）

| 用途 | 命令 |
|------|------|
| 查活动及 created_by | `docker exec -i smartmail-mysql-1 mysql -uroot -proot -D tenant_default -e "SELECT id, name, created_by, template_id, group_id FROM campaign ORDER BY id DESC LIMIT 5;"` |
| 取 UTC 时间（用于 runAt） | `docker exec smartmail-scheduler-1 date "+%Y-%m-%d %H:%M:%S"` |
| 联系人加入分组 | `docker exec -i smartmail-mysql-1 mysql -uroot -proot -D tenant_default -e "INSERT INTO contact_group_member (group_id, contact_id, create_time) VALUES (1, 1, NOW());"` |
| 查计划是否到点 | `docker exec -i smartmail-mysql-1 mysql -uroot -proot -D tenant_default -e "SELECT id, campaign_id, run_at, status FROM schedule_job ORDER BY id DESC LIMIT 5;"` |

---

**总结**：创建新活动时必须带 **Authorization: Bearer &lt;accessToken&gt;**，活动才会有 `created_by`；创建计划时 Body 必须含 **campaignId**（活动 local_id）和 **createdBy**（用户 ID），且 **runAt 用 UTC**；联系人入组用上述 **SQL/终端命令**，且 `contact_group_member` 的 group_id、contact_id 填**内部主键**（见「ID 映射与参数说明」）。
