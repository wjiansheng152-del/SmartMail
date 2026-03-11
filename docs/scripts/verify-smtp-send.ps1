# SmartMail 真实 SMTP 发信验证脚本（方式 A：默认通道 MailHog）
# 用途：登录 → 创建模板/分组/联系人 → 提示执行 SQL 将联系人加入分组 → 创建活动与计划 → 等待后查询投递状态并提示打开 MailHog
# 前置：网关 8080 可用，MySQL/ RabbitMQ/delivery/scheduler/contact/template/campaign/iam 已启动，MailHog 已启动，租户库及 contact_group_member 等表已建
# 默认用户：admin / admin123

$ErrorActionPreference = "Stop"
$BaseUrl = "http://localhost:8080"
$Username = "admin"
$Password = "admin123"

Write-Host "=== SmartMail SMTP 发信验证（MailHog 默认通道）===" -ForegroundColor Cyan
Write-Host "网关: $BaseUrl" -ForegroundColor Gray

# 1. 登录获取 Token
Write-Host "`n[1/7] 登录..." -ForegroundColor Yellow
$loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json
$loginResp = curl.exe -s -X POST "$BaseUrl/api/iam/auth/login" -H "Content-Type: application/json" -d $loginBody
$loginJson = $loginResp | ConvertFrom-Json
if (-not $loginJson.data.accessToken) {
    Write-Host "登录失败，响应: $loginResp" -ForegroundColor Red
    exit 1
}
$token = $loginJson.data.accessToken
Write-Host "  登录成功，已获取 Token" -ForegroundColor Green

$headers = @(
    "Authorization: Bearer $token"
    "Content-Type: application/json"
)

# 2. 创建模板
Write-Host "`n[2/7] 创建邮件模板..." -ForegroundColor Yellow
$templateBody = '{"name":"验证发信","subject":"SMTP 验证邮件","bodyHtml":"<p>您好，这是一封验证邮件。</p>","variables":""}'
$templateResp = curl.exe -s -X POST "$BaseUrl/api/template/template" -H $headers[0] -H $headers[1] -d $templateBody
$templateJson = $templateResp | ConvertFrom-Json
$templateId = $templateJson.data.id
if (-not $templateId) {
    Write-Host "  创建模板失败: $templateResp" -ForegroundColor Red
    exit 1
}
Write-Host "  模板 ID: $templateId" -ForegroundColor Green

# 3. 创建分组
Write-Host "`n[3/7] 创建分组..." -ForegroundColor Yellow
$groupBody = '{"name":"验证分组","ruleType":"static"}'
$groupResp = curl.exe -s -X POST "$BaseUrl/api/contact/group" -H $headers[0] -H $headers[1] -d $groupBody
$groupJson = $groupResp | ConvertFrom-Json
$groupId = $groupJson.data.id
if (-not $groupId) {
    Write-Host "  创建分组失败: $groupResp" -ForegroundColor Red
    exit 1
}
Write-Host "  分组 ID: $groupId" -ForegroundColor Green

# 4. 创建联系人
Write-Host "`n[4/7] 创建联系人..." -ForegroundColor Yellow
$contactBody = '{"email":"test@example.com","name":"测试"}'
$contactResp = curl.exe -s -X POST "$BaseUrl/api/contact/contact" -H $headers[0] -H $headers[1] -d $contactBody
$contactJson = $contactResp | ConvertFrom-Json
$contactId = $contactJson.data.id
if (-not $contactId) {
    Write-Host "  创建联系人失败（可能邮箱已存在）: $contactResp" -ForegroundColor Red
    exit 1
}
Write-Host "  联系人 ID: $contactId" -ForegroundColor Green

# 5. 将联系人加入分组（需手动执行 SQL 或下方 Docker 命令）
Write-Host "`n[5/7] 将联系人加入分组（需执行以下 SQL 或命令）..." -ForegroundColor Yellow
$sql = "INSERT INTO tenant_default.contact_group_member (group_id, contact_id, create_time) VALUES ($groupId, $contactId, NOW());"
Write-Host "  SQL: $sql" -ForegroundColor Gray
Write-Host "  若使用 Docker MySQL，可执行:" -ForegroundColor Gray
Write-Host "  docker exec -i smartmail-mysql-1 mysql -uroot -proot -e `"$sql`"" -ForegroundColor Gray
$confirm = Read-Host "  已执行上述 SQL 并回车继续（或输入 n 退出）"
if ($confirm -eq "n") { exit 0 }

# 6. 创建活动与发送计划
Write-Host "`n[6/7] 创建活动与发送计划..." -ForegroundColor Yellow
$campaignBody = "{`"name`":`"验证活动`",`"templateId`":$templateId,`"groupId`":$groupId,`"status`":`"draft`"}"
$campaignResp = curl.exe -s -X POST "$BaseUrl/api/campaign/campaign" -H $headers[0] -H $headers[1] -d $campaignBody
$campaignJson = $campaignResp | ConvertFrom-Json
$campaignId = $campaignJson.data.id
if (-not $campaignId) {
    Write-Host "  创建活动失败: $campaignResp" -ForegroundColor Red
    exit 1
}
Write-Host "  活动 ID: $campaignId" -ForegroundColor Green

$runAt = (Get-Date).AddMinutes(2).ToString("yyyy-MM-dd HH:mm:ss")
$scheduleBody = "{`"campaignId`":$campaignId,`"cronExpr`":`"`",`"runAt`":`"$runAt`"}"
$scheduleResp = curl.exe -s -X POST "$BaseUrl/api/scheduler/schedule" -H $headers[0] -H $headers[1] -d $scheduleBody
$scheduleJson = $scheduleResp | ConvertFrom-Json
$scheduleId = $scheduleJson.data
if ($null -eq $scheduleId -and $scheduleJson.errorCode) {
    Write-Host "  创建计划失败: $scheduleResp" -ForegroundColor Red
    exit 1
}
Write-Host "  计划 runAt: $runAt（约 2 分钟后触发）" -ForegroundColor Green

# 7. 等待后查询投递状态
Write-Host "`n[7/7] 等待 150 秒后查询投递状态..." -ForegroundColor Yellow
Start-Sleep -Seconds 150

$statusResp = curl.exe -s -X GET "$BaseUrl/api/delivery/delivery/status/$campaignId" -H $headers[0] -H $headers[1]
$statusJson = $statusResp | ConvertFrom-Json
if ($statusJson.data) {
    $d = $statusJson.data
    Write-Host "  投递状态: total=$($d.total) sent=$($d.sent) failed=$($d.failed)" -ForegroundColor Green
} else {
    Write-Host "  状态响应: $statusResp" -ForegroundColor Gray
}

Write-Host "`n请打开 MailHog Web UI 查看是否收到验证邮件: http://localhost:8025" -ForegroundColor Cyan
Write-Host "收件人应为 test@example.com，主题为「SMTP 验证邮件」" -ForegroundColor Gray
Write-Host "=== 验证脚本结束 ===" -ForegroundColor Cyan
