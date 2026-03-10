# 仅构建所有模块 JAR（用于后续 docker build）
Set-Location $PSScriptRoot\..
.\mvnw.cmd package -DskipTests -q
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "JARs built in each module target/"
