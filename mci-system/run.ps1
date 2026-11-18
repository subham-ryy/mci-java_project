# Launcher for MCI System CLI in PowerShell
$env:JAVA_HOME = "C:\Users\rupar\AppData\Local\Programs\Eclipse Adoptium\jdk-25.0.1.8-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:MAVEN_OPTS = "-Djava.net.preferIPv4Stack=true --enable-native-access=ALL-UNNAMED"

.\mvnw.cmd -q exec:java -pl mci-app
