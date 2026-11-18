@echo off
REM Launcher for MCI System Spring Boot Web Application (REST + SSE + Static Web UI)
cd /d "%~dp0"
if not defined JAVA_HOME set "JAVA_HOME=C:\Users\rupar\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.1.8-hotspot"
if exist "%JAVA_HOME%\bin" set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MAVEN_OPTS=-Djava.net.preferIPv4Stack=true --enable-native-access=ALL-UNNAMED"

echo ===================================================
echo   Starting MCI Web Server on http://localhost:8080/
echo ===================================================
call .\mvnw.cmd spring-boot:run -pl mci-web
