# SmartMail 本地构建并启动（需先 mvn package，再 docker-compose up）
Set-Location $PSScriptRoot\..

Write-Host "Building all modules..."
& .\mvnw.cmd package -DskipTests -q
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Starting infrastructure and services..."
docker-compose up -d mysql redis rabbitmq mailhog
Start-Sleep -Seconds 15

# 初始化 tenant_default（若 MySQL 已存在则忽略）
docker-compose exec -T mysql mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS tenant_default DEFAULT CHARACTER SET utf8mb4; CREATE DATABASE IF NOT EXISTS platform DEFAULT CHARACTER SET utf8mb4;" 2>$null

Write-Host "Building and starting application services..."
docker-compose up -d --build gateway iam contact template campaign scheduler delivery tracking audit

Write-Host "Done. Gateway: http://localhost:8080  MailHog: http://localhost:8025  RabbitMQ: http://localhost:15672"
