@echo off
REM ==========================================
REM Docker Compose Quick Commands
REM ==========================================

setlocal enabledelayedexpansion

echo.
echo ==========================================
echo 🐳 BARBER - DOCKER QUICK START
echo ==========================================
echo.

:menu
echo.
echo Wybierz opcję:
echo.
echo [1] Uruchom aplikację (docker-compose up)
echo [2] Uruchom w tle (docker-compose up -d)
echo [3] Zbuduj obraz (docker-compose build)
echo [4] Zbuduj i uruchom (docker-compose up --build)
echo [5] Zatrzymaj kontenery (docker-compose stop)
echo [6] Zatrzymaj i usuń (docker-compose down)
echo [7] Czysty start - usuń wszystko (docker-compose down -v)
echo [8] Pokaż logi aplikacji
echo [9] Pokaż logi PostgreSQL
echo [10] Wejdź do shella aplikacji
echo [11] Wejdź do psql bazy danych
echo [0] Wyjście
echo.

set /p choice="Wybór (0-11): "

if "%choice%"=="1" (
    echo.
    echo ✅ Uruchamianie aplikacji...
    docker-compose up
    goto menu
)

if "%choice%"=="2" (
    echo.
    echo ✅ Uruchamianie w tle...
    docker-compose up -d
    echo Aplikacja uruchomiona na http://localhost:8080
    timeout /t 3 /nobreak
    goto menu
)

if "%choice%"=="3" (
    echo.
    echo ✅ Budowanie obrazu...
    docker-compose build
    goto menu
)

if "%choice%"=="4" (
    echo.
    echo ✅ Budowanie i uruchamianie...
    docker-compose up --build
    goto menu
)

if "%choice%"=="5" (
    echo.
    echo ✅ Zatrzymywanie kontenerów...
    docker-compose stop
    timeout /t 2 /nobreak
    goto menu
)

if "%choice%"=="6" (
    echo.
    echo ✅ Zatrzymywanie i usuwanie...
    docker-compose down
    timeout /t 2 /nobreak
    goto menu
)

if "%choice%"=="7" (
    echo.
    echo ⚠️  Czysty start - usuwanie wszystko...
    docker-compose down -v
    echo ✅ Wszystko usunięte. Następny start zbuduje nowy obraz.
    timeout /t 2 /nobreak
    goto menu
)

if "%choice%"=="8" (
    echo.
    echo 📋 Logi aplikacji:
    echo.
    docker-compose logs barber-app
    pause
    goto menu
)

if "%choice%"=="9" (
    echo.
    echo 📋 Logi PostgreSQL:
    echo.
    docker-compose logs postgres
    pause
    goto menu
)

if "%choice%"=="10" (
    echo.
    echo 💻 Wchodzę do shella aplikacji...
    docker exec -it barber-app sh
    goto menu
)

if "%choice%"=="11" (
    echo.
    echo 🗄️  Wchodzę do psql...
    docker exec -it barber-postgres psql -U barber_user -d barberdb
    goto menu
)

if "%choice%"=="0" (
    echo.
    echo Wychodzę...
    exit /b 0
)

echo ❌ Nieprawidłowy wybór
timeout /t 2 /nobreak
goto menu
