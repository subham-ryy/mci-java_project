# Launcher for MCI System Spring Boot Web Application in PowerShell
Set-Location -Path $PSScriptRoot

if (-not $env:JAVA_HOME) {
    $env:JAVA_HOME = "C:\Users\rupar\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.1.8-hotspot"
}
if (Test-Path "$env:JAVA_HOME\bin") {
    $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
}
$env:MAVEN_OPTS = "-Djava.net.preferIPv4Stack=true --enable-native-access=ALL-UNNAMED"

# Clean up port 8080 if occupied
$conn = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
if ($conn) {
    Write-Host "[i] Port 8080 in use by PID $($conn.OwningProcess), stopping previous instance..." -ForegroundColor Yellow
    Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

Write-Host "===================================================" -ForegroundColor Cyan
Write-Host "  Starting MCI Web Server on http://localhost:8080/" -ForegroundColor Green
Write-Host "===================================================" -ForegroundColor Cyan
.\mvnw.cmd spring-boot:run -pl mci-web
