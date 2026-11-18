@echo off
REM Launcher for MCI System CLI
set "JAVA_HOME=C:\Users\rupar\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.1.8-hotspot"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MAVEN_OPTS=-Djava.net.preferIPv4Stack=true --enable-native-access=ALL-UNNAMED"

.\mvnw.cmd -q exec:java -pl mci-app
