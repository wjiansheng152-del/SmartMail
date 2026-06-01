# SmartMail SMTP send verification (MailHog default channel)
# Flow: login -> template/group/contact -> SQL -> campaign/schedule -> wait -> check status
# Prerequisites: gateway 8080, all services up, MailHog running, tenant DB initialized
# Default user: admin / admin123

$ErrorActionPreference = "Stop"
$BaseUrl = "http://localhost:8080"
$Username = "admin"
$Password = "admin123"

Write-Host "=== SmartMail SMTP verify (MailHog) ===" -ForegroundColor Cyan
Write-Host "Gateway: $BaseUrl" -ForegroundColor Gray

function Invoke-SmartMailApi {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token = "",
        [string]$Body = $null
    )
    $params = @{
        Uri         = "$BaseUrl$Path"
        Method      = $Method
        ContentType = "application/json"
    }
    if ($Token) { $params.Headers = @{ Authorization = "Bearer $Token" } }
    if ($Body) { $params.Body = $Body }
    return Invoke-RestMethod @params
}

Write-Host "`n[1/7] Login..." -ForegroundColor Yellow
$loginJson = Invoke-SmartMailApi -Method POST -Path "/api/iam/auth/login" -Body (@{ username = $Username; password = $Password } | ConvertTo-Json -Compress)
if (-not $loginJson.data.accessToken) {
    Write-Host "Login failed" -ForegroundColor Red
    exit 1
}
$token = $loginJson.data.accessToken
Write-Host "  OK" -ForegroundColor Green

Write-Host "`n[2/7] Create template..." -ForegroundColor Yellow
$templateJson = Invoke-SmartMailApi -Method POST -Path "/api/template/template" -Token $token -Body '{"name":"verify-mail","subject":"SMTP verify mail","bodyHtml":"<p>Hello verify mail.</p>","variables":""}'
$templateId = $templateJson.data.id
if (-not $templateId) { Write-Host "  Failed"; exit 1 }
Write-Host "  templateId=$templateId" -ForegroundColor Green

Write-Host "`n[3/7] Create group..." -ForegroundColor Yellow
$groupJson = Invoke-SmartMailApi -Method POST -Path "/api/contact/group" -Token $token -Body '{"name":"verify-group","ruleType":"static"}'
$groupId = $groupJson.data.id
if (-not $groupId) { Write-Host "  Failed"; exit 1 }
Write-Host "  groupId=$groupId" -ForegroundColor Green

Write-Host "`n[4/7] Create contact..." -ForegroundColor Yellow
try {
    $contactJson = Invoke-SmartMailApi -Method POST -Path "/api/contact/contact" -Token $token -Body '{"email":"test@example.com","name":"test"}'
    $contactId = $contactJson.data.id
} catch {
    Write-Host "  Contact may exist, querying..." -ForegroundColor Gray
    $listJson = Invoke-SmartMailApi -Method GET -Path "/api/contact/contact/page?page=1&size=20" -Token $token
    $contactId = ($listJson.data.records | Where-Object { $_.email -eq 'test@example.com' } | Select-Object -First 1).id
}
if (-not $contactId) { Write-Host "  Failed"; exit 1 }
Write-Host "  contactId=$contactId" -ForegroundColor Green

Write-Host "`n[5/7] Add contact to group..." -ForegroundColor Yellow
$sql = 'INSERT IGNORE INTO tenant_default.contact_group_member (group_id, contact_id, create_time) VALUES (' + $groupId + ', ' + $contactId + ', NOW());'
Write-Host "  SQL: $sql" -ForegroundColor Gray
$ErrorActionPreference = 'Continue'
docker exec smartmail-mysql-1 mysql -uroot -proot -e $sql | Out-Null
$ErrorActionPreference = 'Stop'
Write-Host "  OK" -ForegroundColor Green

Write-Host "`n[6/7] Create campaign and schedule..." -ForegroundColor Yellow
$campaignBody = (@{ name = "verify-campaign"; templateId = $templateId; groupId = $groupId; status = "draft" } | ConvertTo-Json -Compress)
$campaignJson = Invoke-SmartMailApi -Method POST -Path "/api/campaign/campaign" -Token $token -Body $campaignBody
$campaignId = $campaignJson.data.id
if (-not $campaignId) { Write-Host "  Failed"; exit 1 }
Write-Host "  campaignId=$campaignId" -ForegroundColor Green

# runAt in UTC to match scheduler container timezone
$runAt = (Get-Date).ToUniversalTime().AddMinutes(2).ToString("yyyy-MM-dd HH:mm:ss")
Write-Host "  runAt(UTC): $runAt" -ForegroundColor Gray
$scheduleBody = (@{ campaignId = $campaignId; cronExpr = ""; runAt = $runAt } | ConvertTo-Json -Compress)
$scheduleJson = Invoke-SmartMailApi -Method POST -Path "/api/scheduler/schedule" -Token $token -Body $scheduleBody
if ($null -eq $scheduleJson.data -and $scheduleJson.errorCode) { Write-Host "  Failed"; exit 1 }
Write-Host "  schedule created, trigger in ~2 min" -ForegroundColor Green

Write-Host "`n[7/7] Wait 150s then check delivery status..." -ForegroundColor Yellow
Start-Sleep -Seconds 150

$statusJson = Invoke-SmartMailApi -Method GET -Path "/api/delivery/delivery/status/$campaignId" -Token $token
if ($statusJson.data) {
    $d = $statusJson.data
    Write-Host "  total=$($d.total) sent=$($d.sent) failed=$($d.failed)" -ForegroundColor Green
} else {
    Write-Host "  status: $($statusJson | ConvertTo-Json -Compress)" -ForegroundColor Gray
}

Write-Host "`nOpen MailHog: http://localhost:8025" -ForegroundColor Cyan
Write-Host "Expected: test@example.com, subject SMTP verify mail" -ForegroundColor Gray
Write-Host "=== Done ===" -ForegroundColor Cyan
