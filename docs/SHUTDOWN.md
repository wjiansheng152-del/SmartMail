# SmartMail 服务关闭指南

本文档说明如何关闭通过 **Docker Compose** 或 **本地/IDE** 启动的 SmartMail 相关服务，以及如何按需停止部分服务或清理资源。

---

## 一、通过 Docker Compose 启动时的关闭方式

### 1. 关闭所有服务（推荐）

在**项目根目录**（即包含 `docker-compose.yml` 的目录）执行：

```bash
docker-compose down
```

- 会停止并**删除**本次由 `docker-compose up` 创建的所有容器。
- 默认**不会**删除数据卷（如 MySQL 数据保存在 `mysql_data`），下次 `docker-compose up -d` 时数据仍在。

**PowerShell / CMD 示例：**

```powershell
cd d:\CursorProgram\SmartMail
docker-compose down
```

### 2. 仅停止容器、不删除

若只想停止容器，稍后可能再次 `docker-compose start` 或 `up`，可执行：

```bash
docker-compose stop
```

- 容器仍存在，可用 `docker-compose start` 再次启动。
- 删除容器仍需后续执行 `docker-compose down`。

### 3. 关闭并删除数据卷（慎用）

需要**清空 MySQL、Redis 等持久化数据**时：

```bash
docker-compose down -v
```

- `-v` 会删除 compose 中声明的**命名卷**（如 `mysql_data`）。
- 执行后数据库等内容将丢失，仅在做环境重置时使用。

### 4. 仅关闭业务服务，保留中间件

若只希望关闭网关、IAM、contact 等业务服务，保留 MySQL、Redis、RabbitMQ、MailHog：

```bash
docker-compose stop gateway frontend iam contact template campaign scheduler delivery tracking audit
```

保留的容器可继续供本地 IDE 启动的微服务连接。需要再启动业务服务时：

```bash
docker-compose start gateway iam contact template campaign scheduler delivery tracking audit
```

（若包含 frontend，按需加入 `frontend`。）

### 5. 查看当前运行中的 compose 服务

```bash
docker-compose ps
```

可确认哪些服务在运行、端口映射是否正常。

---

## 二、本地 / IDE 启动时的关闭方式

当各微服务是直接在 IDE 中运行主类，或通过 `java -jar` 在终端启动时，需逐个停止进程。

### 1. 在 IDE 中关闭

- **IDEA / Eclipse / Cursor 等**：在“运行”或“调试”面板中，找到对应服务的运行实例（如 `IamApplication`、`GatewayApplication`），点击 **停止（红色方块）** 即可。
- 建议关闭顺序（与启动顺序相反）：先关 **网关**，再关 **IAM**，最后关 contact、template、campaign、scheduler、delivery、tracking、audit（可任意顺序）。

### 2. 在终端中关闭

若服务是在命令行用 `java -jar` 或 `mvn spring-boot:run` 启动的：

- 在**运行该服务的终端**中按 **Ctrl + C**，可正常结束该进程。
- 若有多个终端分别跑多个服务，需在每个终端中依次 Ctrl + C。

### 3. 按端口结束进程（Windows）

若进程未在可见终端中运行或无法 Ctrl + C，可按端口查找并结束进程。

**步骤一：查占用端口的进程 ID（PID）**

以关闭占用 8080（网关）的进程为例，在 **PowerShell（管理员可选）** 中执行：

```powershell
netstat -ano | findstr :8080
```

输出中最后一列为 PID，例如 `12345`。

**步骤二：结束该进程**

```powershell
taskkill /PID 12345 /F
```

**常用端口与对应服务：**

| 端口 | 服务     |
|------|----------|
| 8080 | gateway  |
| 8081 | iam      |
| 8082 | contact  |
| 8083 | template |
| 8084 | campaign |
| 8085 | scheduler|
| 8086 | delivery |
| 8087 | tracking |
| 8088 | audit    |

可按需对上述端口重复 `netstat -ano | findstr :端口` 与 `taskkill /PID <PID> /F`，关闭所有本地启动的 SmartMail 服务。

### 4. 按端口结束进程（Linux / macOS）

```bash
# 查找占用 8080 的进程
lsof -i :8080

# 或使用
ss -tlnp | grep 8080
```

根据 PID 结束进程：

```bash
kill <PID>
# 若需强制结束
kill -9 <PID>
```

---

## 三、混合部署时的关闭顺序

当**中间件用 Docker**、**业务服务用本地/IDE** 时：

1. 先按 **第二节** 关闭所有本地/IDE 启动的微服务（网关、IAM、contact 等）。
2. 若不再需要中间件，再在项目根目录执行 **第一节** 的 `docker-compose down` 或 `docker-compose stop mysql redis rabbitmq mailhog` 等，按需选择是否保留数据卷。

---

## 四、简要对照表

| 启动方式           | 关闭方式 |
|--------------------|----------|
| `docker-compose up -d` 全部 | 项目根目录执行 `docker-compose down` |
| 仅停止、不删容器   | `docker-compose stop` |
| 停止并删数据卷     | `docker-compose down -v` |
| IDE 运行各服务     | IDE 运行面板中逐个停止，或对对应端口用 `taskkill` / `kill` |
| 命令行 `java -jar` / `mvn` | 对应终端 Ctrl + C，或按端口查 PID 后 `taskkill` / `kill` |

按上述方式即可有序关闭 SmartMail 相关服务；更多启动与验证说明见 [STARTUP-AND-VERIFICATION.md](./STARTUP-AND-VERIFICATION.md)。
