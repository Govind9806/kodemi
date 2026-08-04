@echo off
echo Starting Payment Service...
cd /d "%~dp0"
mvnw.cmd compile -Dmaven.test.skip=true
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Starting Spring Boot application...
java -cp "target/classes;target/dependency/*" -Dspring.profiles.active=dev com.example.payment_service.PaymentServiceApplication
pause