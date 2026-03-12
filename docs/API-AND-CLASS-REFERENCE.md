# SmartMail 暴露接口与重要类说明

本文档列出所有对外暴露的 REST 接口及重要公共类，并说明其功能与使用方式。  
统一入口为**网关**，端口 **8080**，路径前缀 `/api/{服务简称}/{分类}`，除登录/刷新外均需在请求头携带 `Authorization: Bearer <accessToken>`。

---

## 一、REST API 列表

### 1. IAM 认证（iam）

| 方法 | 路径 | 请求体 | 响应 | 说明 |
|------|------|--------|------|------|
| POST | `/api/iam/auth/login` | `LoginRequest`（username, password） | `Result<TokenResponse>` | 用户登录，校验用户名密码后返回 accessToken、refreshToken 及过期秒数 |
| POST | `/api/iam/auth/refresh` | `RefreshRequest`（refreshToken） | `Result<TokenResponse>` | 使用 refreshToken 换取新的 accessToken 与 refreshToken；Token 无效或过期返回 401 |

- **LoginRequest**：username（必填）、password（必填）  
- **RefreshRequest**：refreshToken（必填）  
- **TokenResponse**：accessToken、refreshToken、accessExpiresIn、refreshExpiresIn  

---

### 2. 客户/联系人（contact）

| 方法 | 路径 | 请求/参数 | 响应 | 说明 |
|------|------|------------|------|------|
| POST | `/api/contact/contact` | Body: `ContactCreateRequest` | `Result<Contact>` | 新建客户，邮箱必填且唯一，姓名、手机可选；返回 **id 为 local_id** |
| GET | `/api/contact/contact/{id}` | Path: id | `Result<Contact>` | 按 ID 查询客户，**id 为 contact 的 local_id**（租户内从 1 连续），不存在 404 |
| PUT | `/api/contact/contact/{id}` | Path: id + Body: `ContactUpdateRequest` | `Result<Contact>` | 更新客户，**路径 id 为 local_id**；email 必填，name/mobile 可选；邮箱冲突 409 |
| GET | `/api/contact/contact/page` | Query: page=1, size=20, groupId? | `Result<IPage<Contact>>` | 分页查询客户，**id 与 groupId 均为 local_id**；groupId 表示分组的 local_id，后端按之解析再按分组过滤 |
| DELETE | `/api/contact/contact/{id}` | Path: id | `Result<Void>` | 按 ID 删除客户，**路径 id 为 local_id**，幂等 |

- **ContactCreateRequest**：email（必填、邮箱格式）、name、mobile  
- **ContactUpdateRequest**：email（必填、邮箱格式）、name、mobile  
- **Contact**：id（local_id）、email、name、mobile、createTime、updateTime  

#### 客户分组（group）

| 方法 | 路径 | 请求/参数 | 响应 | 说明 |
|------|------|------------|------|------|
| POST | `/api/contact/group` | Body: `ContactGroup`（name、ruleType 等） | `Result<ContactGroup>` | 创建分组，ruleType 默认 static |
| GET | `/api/contact/group/{id}` | Path: id | `Result<ContactGroup>` | 按 ID 查询分组，**id 为分组的 local_id**，不存在 404 |
| GET | `/api/contact/group/list` | - | `Result<List<ContactGroup>>` | 当前租户下全部分组列表 |
| POST | `/api/contact/group/{groupId}/member` | Path: groupId + Body: `{ contactId }` | `Result<Void>` | 将客户加入分组；**groupId、contactId 均为对应资源的 local_id**，幂等 |
| POST | `/api/contact/group/{groupId}/member/batch` | Path: groupId + Body: `{ contactIds }` | `Result<Void>` | 批量将客户加入分组，同上 local_id 约定，幂等 |
| DELETE | `/api/contact/group/{groupId}/member/{contactId}` | Path: groupId, contactId | `Result<Void>` | 从分组移出客户，**groupId、contactId 为 local_id**，幂等 |
| DELETE | `/api/contact/group/{groupId}/member/batch` | Path: groupId + Body: `{ contactIds }` | `Result<Void>` | 批量从分组移出客户，同上，幂等 |
| DELETE | `/api/contact/group/{id}` | Path: id | `Result<Void>` | 按 ID 删除分组，**id 为分组的 local_id**，幂等 |

#### 退订（unsubscribe）

| 方法 | 路径 | 请求/参数 | 响应 | 说明 |
|------|------|------------|------|------|
| POST | `/api/contact/unsubscribe` | Query: email, reason? | `Result<Unsubscribe>` | 添加退订记录，已存在则返回原记录（幂等） |
| GET | `/api/contact/unsubscribe/check` | Query: email | `Result<Boolean>` | 检查邮箱是否已退订，true 表示不应再发 |
| GET | `/api/contact/unsubscribe/list` | - | `Result<List<String>>` | 当前租户下所有已退订邮箱列表，供发送前批量过滤 |

#### 黑名单（blacklist）

| 方法 | 路径 | 请求/参数 | 响应 | 说明 |
|------|------|------------|------|------|
| POST | `/api/contact/blacklist` | Query: email, source? | `Result<Blacklist>` | 添加黑名单邮箱 |
| GET | `/api/contact/blacklist/check` | Query: email | `Result<Boolean>` | 检查是否在黑名单 |
| GET | `/api/contact/blacklist/list` | - | `Result<List<Blacklist>>` | 当前租户黑名单列表 |

---

### 3. 邮件模板（template）

| 方法 | 路径 | 请求/参数 | 响应 | 说明 |
|------|------|------------|------|------|
| POST | `/api/template/template` | Body: `EmailTemplate` | `Result<EmailTemplate>` | 创建模板，自动 set createTime/updateTime，version 默认 1；返回 **id 为 local_id** |
| GET | `/api/template/template/{id}` | Path: id | `Result<EmailTemplate>` | 按 ID 查询，**id 为模板的 local_id**（租户内），不存在 404 |
| GET | `/api/template/template/list` | - | `Result<List<EmailTemplate>>` | 当前租户全部模板，每项 id 为 local_id |
| PUT | `/api/template/template/{id}` | Path: id + Body: `EmailTemplate` | `Result<EmailTemplate>` | 全量更新模板，**路径 id 为 local_id**，不存在 404 |
| DELETE | `/api/template/template/{id}` | Path: id | `Result<Void>` | 按 ID 删除模板，**路径 id 为 local_id**，幂等 |

---

### 4. 营销活动（campaign）

| 方法 | 路径 | 请求/参数 | 响应 | 说明 |
|------|------|------------|------|------|
| POST | `/api/campaign/campaign` | Body: `Campaign` | `Result<Campaign>` | 创建活动，status 默认 draft；需带 Token，网关注入 X-User-Id 写入 created_by；返回 **id 为 local_id**（按 created_by 从 1 连续） |
| GET | `/api/campaign/campaign/{id}` | Path: id | `Result<Campaign>` | 按 ID 查询，**id 为 campaign 的 local_id**。若请求头带 **X-User-Id**，则按 local_id + created_by 查询（投递触发用）；否则按主键查 |
| PUT | `/api/campaign/campaign/{id}` | Path: id + Body: `Campaign` | `Result<Campaign>` | 全量更新活动，**路径 id 为 local_id**，不存在 404 |
| DELETE | `/api/campaign/campaign/{id}` | Path: id | `Result<Void>` | 删除活动，**路径 id 为 local_id**，先删 campaign_ab_assignment 再删 campaign，幂等 |
| GET | `/api/campaign/campaign/list` | - | `Result<List<Campaign>>` | 当前租户全部活动列表，每项 id 为 local_id |

---

### 5. 调度（scheduler）

| 方法 | 路径 | 请求/参数 | 响应 | 说明 |
|------|------|------------|------|------|
| POST | `/api/scheduler/schedule` | Body: ScheduleCreateRequest（**campaignId** 活动 local_id、**createdBy** 用户ID、cronExpr、runAt） | `Result<Long>` | 创建发送计划；**createdBy** 用于投递时按 local_id 查活动；返回 **计划 ID 为 local_id**（按 created_by 从 1 连续）；runAt 字符串格式 `yyyy-MM-dd HH:mm:ss`，语义为 **UTC 时间**（前端界面会自动在本地时间与 UTC 之间转换） |
| GET | `/api/scheduler/schedule/list` | 请求头：X-User-Id（可选） | `Result<List<ScheduleJobListItem>>` | 若带 **X-User-Id** 仅返回该用户创建的计划；每项 **id 为计划的 local_id** |

---

### 6. 投递（delivery）

| 方法 | 路径 | 请求/参数 | 响应 | 说明 |
|------|------|------------|------|------|
| GET | `/api/delivery/delivery/status/{campaignId}` | Path: campaignId | `Result<Map>`（含 campaignId, total, sent, failed） | 按 campaign_batch 汇总该活动的投递状态 |
| GET | `/api/delivery/smtp-config` | 请求头：X-User-Id（由网关从 JWT 注入） | `Result<SmtpConfigDto>`，无配置时为 `Result<null>`；密码以占位 `****` 返回 | 查询当前登录用户在本租户下的 SMTP 配置 |
| PUT | `/api/delivery/smtp-config` | 请求头：X-User-Id；Body：SmtpConfigDto（host, port, username, password 可选, fromEmail, fromName, useSsl） | `Result<SmtpConfigDto>` | 保存或更新当前用户的 SMTP 配置；password 留空或不传则不更新密码 |

- **SmtpConfigDto**：host、port、username、password（PUT 时可选，不修改密码可不传）、fromEmail、fromName、useSsl（boolean）。密码入库前经 AES 加密，GET 不返回明文。

---

### 7. 追踪（tracking）

| 方法 | 路径 | 请求/参数 | 响应 | 说明 |
|------|------|------------|------|------|
| GET | `/api/tracking/pixel/{deliveryId}` | Path: deliveryId, Query: campaignId（可选） | 1x1 透明 GIF 图片 | 打开追踪像素；若传 campaignId 则写入 tracking_event（open） |
| GET | `/api/tracking/click/{deliveryId}` | Path: deliveryId, Query: url, campaignId（可选） | 302 重定向到 url | 点击追踪；若传 campaignId 则写入 tracking_event（click） |
| GET | `/api/tracking/stats/{campaignId}` | Path: campaignId | `Result<Map<String,Object>>` | 查询活动的 openCount、clickCount 等统计 |

---

### 8. 审计（audit）

| 方法 | 路径 | 请求/参数 | 响应 | 说明 |
|------|------|------------|------|------|
| POST | `/api/audit/log` | Body: `AuditLog` | `Result<AuditLog>` | 写入一条审计日志 |
| GET | `/api/audit/log/list` | Query: userId?, page=1, size=20 | `Result<List<AuditLog>>` | 分页查询审计日志，可按 userId 过滤 |

---

## 二、统一响应与异常

- **成功**：HTTP 200，响应体为 `Result<T>`，格式：`{ "data": T }`，无数据时为 `{ "data": null }`。  
- **业务异常**：由 `GlobalExceptionHandler` 捕获，返回 HTTP 状态按错误码映射（如 404、401），Body 为 `ErrorResponse`：`errorCode`、`errorInfo`。

---

## 三、重要对外类说明

### 3.1 公共模块（common）

- **Result&lt;T&gt;**：统一成功响应包装，含 `data` 字段；`Result.ok(T)`、`Result.ok()`。  
- **ErrorResponse**：异常响应体，含 `errorCode`、`errorInfo`。  
- **ErrorCode**：业务错误码枚举（如 UNAUTHORIZED、NOT_FOUND、BAD_REQUEST 等），与 HTTP 状态映射在 GlobalExceptionHandler 中。  
- **BizException**：业务异常，构造时传入 ErrorCode 与可选 errorInfo，由 GlobalExceptionHandler 转为 ErrorResponse。  
- **GlobalExceptionHandler**：`@RestControllerAdvice`，处理 BizException、MethodArgumentNotValidException、通用 Exception，返回统一错误格式。  
- **TenantContext**：线程级租户 ID 存储，setTenantId/getTenantId/clear；与 TenantContextFilter 配合，从请求头 X-Tenant-Id 注入。  
- **TenantContextFilter**：从 Header 读取租户 ID 写入 TenantContext，请求结束后清除。

### 3.2 网关（gateway）

- **JwtAuthFilter**：对除 `/api/iam/auth/login`、`/api/iam/auth/refresh` 外的请求校验 JWT，校验通过后向下游请求头写入 X-User-Id、X-Tenant-Id、X-Username。  
- **GatewayJwtProperties**：网关侧 JWT 密钥等配置，需与 IAM 的 app.jwt.secret 一致。

### 3.3 IAM（iam）

- **AuthController**：暴露登录、刷新接口；内部使用 AuthenticationManager、JwtUtil、JwtProperties。  
- **JwtUtil**：签发 accessToken/refreshToken、解析并校验 Token、判断是否为 refreshToken。  
- **JwtProperties**：access/refresh 过期时间、密钥等。  
- **LoginRequest / RefreshRequest / TokenResponse**：登录请求、刷新请求、Token 响应 DTO。

### 3.4 业务实体与 DTO（各服务）

- **Contact、ContactGroup、Unsubscribe、Blacklist**：客户模块实体。  
- **EmailTemplate**：模板实体。  
- **Campaign**：活动实体。  
- **AuditLog**：审计日志实体。  

以上实体在对应 Controller 中作为请求体或响应 data 使用，字段含义以各表建表说明为准。

### 3.5 投递相关（delivery）

- **EmailSender**：邮件发送接口，实现类有 SmtpEmailSender、ApiEmailSender 等，由 SendStrategy 按配置选择。  
- **SendRequest / SendResult**：发送请求与结果模型，在 MQ 消费与发送逻辑中使用。  
- **SendTaskConsumer**：消费发送队列，调用 EmailSender 执行发送。  
- **RabbitConfig**：队列与交换机定义，供投递与调度使用。

---

## 四、调用约定

1. **Base URL**：通过网关访问时为 `http://<网关地址>:8080`，例如 `http://localhost:8080`。  
2. **认证**：除登录、刷新外，请求头需带 `Authorization: Bearer <accessToken>`；网关校验通过后会将用户与租户信息通过请求头传给下游。  
3. **租户**：若多租户，请求头可带 `X-Tenant-Id`（网关会从 JWT 中解析并传递），各业务服务按租户 Schema 隔离数据。  
4. **日期时间**：接口规范要求为 `yyyy-MM-dd HH:mm:ss`（北京标准时间 UTC+8）。  
5. **Content-Type**：请求与响应均为 `application/json`；追踪像素接口为 `image/gif`。  
6. **对外 ID 与 local_id**：客户、模板、分组、营销活动、发送计划对外接口中的 **id** 均为 **local_id**（同一租户或同一用户下从 1 连续，删除后可复用）。路径参数与响应体中的 id 均指 local_id；分组成员接口的 groupId、contactId 也为对应资源的 local_id。创建计划时需传 **createdBy**（用户ID），以便投递时按 local_id + created_by 正确查询活动。  
7. **用户 SMTP 密码加密**：delivery-service 中用户 SMTP 密码使用 `app.smtp.encryption-key` 配置项进行 AES 加密后落库。**生产环境必须配置该密钥**（建议 16 字节或 Base64 编码），并通过环境变量或配置中心注入，切勿提交到代码仓库。

完成以上接口与类的实现即可满足“暴露方法与类”的文档化需求；具体字段以代码与建表 DDL 为准。
