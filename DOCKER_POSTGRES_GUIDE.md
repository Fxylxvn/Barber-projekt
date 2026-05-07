# 🚀 Instrukcja Uruchomienia + Docker + PostgreSQL

## 📋 Spis Treści

1. [Uruchomienie Bez Dockera (H2)](#bez-dockera)
2. [Uruchomienie Z Dockerem + PostgreSQL](#z-dockerem)
3. [Konfiguracja PostgreSQL Lokalnie](#postgres-lokalnie)
4. [Porównanie H2 vs PostgreSQL](#porownanie)

---

## 🏃 Uruchomienie Bez Dockera {#bez-dockera}

### Wymagania
- Java 17+
- Maven (lub Maven Wrapper wbudowany)

### Krok 1: Otwórz Terminal

```powershell
cd c:\Users\kondz1o\Desktop\Barber\Barber
```

### Krok 2: Uruchom Aplikację

```powershell
.\mvnw.cmd spring-boot:run
```

Lub krótko:
```powershell
mvn clean spring-boot:run
```

### Krok 3: Czekaj na START

```
...
Tomcat started on port(s): 8080 (http) with context path ''
Started BarberApplication in 5.234 seconds (JVM running for 5.945)
```

### Krok 4: Otwórz Aplikację

```
http://localhost:8080
```

### Baza Danych: H2 (In-Memory)

- **URL H2 Console**: `http://localhost:8080/h2-console`
- **Username**: `sa`
- **Password**: `password`
- **JDBC URL**: `jdbc:h2:file:C:/Users/kondz1o/test;AUTO_SERVER=TRUE`

⚠️ **Uwaga**: Przy każdym restarcie aplikacja tworzy nową bazę (profile `ddl-auto=drop-and-create`)

---

## 🐳 Uruchomienie Z Dockerem + PostgreSQL {#z-dockerem}

### Wymagania

1. **Docker** - Pobierz z https://www.docker.com/products/docker-desktop
2. **Docker Compose** - Zwykle jest zainstalowany z Docker Desktop

### Weryfikacja Instalacji

```powershell
docker --version
docker-compose --version
```

Powinno wyświetlić wersje, np:
```
Docker version 24.0.0
Docker Compose version v2.20.0
```

### Krok 1: Przygotowanie Projektu

```powershell
cd c:\Users\kondz1o\Desktop\Barber\Barber
```

### Krok 2: Zbuduj Obraz Dockera

```powershell
docker-compose build
```

**Czeka to zrobi:**
1. Ściągnie Maven i Java
2. Zbuduje JAR aplikacji
3. Utworzy obrazy Dockera

**Czas**: ~5-10 minut (za pierwszym razem)

### Krok 3: Uruchom Kontenery

```powershell
docker-compose up
```

**Dane logowania:**
- **App Port**: `8080`
- **PostgreSQL Port**: `5432`
- **DB User**: `barber_user`
- **DB Password**: `barber_password`
- **Database**: `barberdb`

### Czekaj na START

```
barber-postgres | database system is ready to accept connections
barber-app | Tomcat started on port(s): 8080 (http) with context path ''
barber-app | Started BarberApplication in 12.456 seconds
```

### Krok 4: Otwórz Aplikację

```
http://localhost:8080
```

### Krok 5: Zatrzymaj Kontenery

```powershell
# Naciśnij Ctrl+C

# Lub w innym terminalu:
docker-compose down
```

**Aby usunąć też dane z bazy:**
```powershell
docker-compose down -v
```

---

## 🛢️ Konfiguracja PostgreSQL Lokalnie {#postgres-lokalnie}

Jeśli chcesz używać PostgreSQL bez Dockera (uruchomione lokalnie na systemie):

### Krok 1: Zainstaluj PostgreSQL

Pobierz z: https://www.postgresql.org/download/windows/

Podczas instalacji:
- **Port**: `5432` (domyślnie)
- **Superuser**: `postgres`
- **Password**: Zapamiętaj!

### Krok 2: Utwórz Bazę Danych

Otwórz `pgAdmin` (dołączony z PostgreSQL) lub użyj psql:

```powershell
# Otwórz PostgreSQL shell
psql -U postgres
```

```sql
-- Utwórz użytkownika
CREATE USER barber_user WITH PASSWORD 'barber_password';

-- Utwórz bazę danych
CREATE DATABASE barberdb OWNER barber_user;

-- Daj uprawnienia
ALTER ROLE barber_user WITH CREATEDB;
GRANT ALL PRIVILEGES ON DATABASE barberdb TO barber_user;

-- Wyjdź
\q
```

### Krok 3: Uruchom Aplikację z PostgreSQL

```powershell
cd c:\Users\kondz1o\Desktop\Barber\Barber

# Z profilem PostgreSQL
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=postgres"
```

Lub:
```bash
mvn clean spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=postgres"
```

### Krok 4: Weryfikuj Połączenie

Aplikacja powinna się uruchomić z wiadomością:
```
Using PostgreSQL database at localhost:5432/barberdb
```

---

## 📊 Porównanie H2 vs PostgreSQL {#porownanie}

| Cecha | H2 | PostgreSQL |
|-------|----|----|
| **Installation** | Wbudowana | Wymagane zainstalowanie |
| **Data Persistence** | Plik na dysku | Baza danych serwera |
| **Performance** | Szybko dla devów | Optymalizowana produkcja |
| **Scalability** | Mała | Duża (producja) |
| **Use Case** | Development/Testing | Production |
| **Profile** | `default` | `postgres` lub `docker` |
| **Konfiguracja** | `application.properties` | `application-postgres.properties` |

---

## 📁 Pliki Konfiguracyjne

### `application.properties` (H2 - Development)
```properties
spring.datasource.url=jdbc:h2:file:C:/Users/kondz1o/test
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=drop-and-create
```

### `application-docker.properties` (PostgreSQL - Docker)
```properties
spring.datasource.url=jdbc:postgresql://postgres:5432/barberdb
spring.datasource.username=barber_user
spring.datasource.password=barber_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
```

### `application-postgres.properties` (PostgreSQL - Local)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/barberdb
spring.datasource.username=barber_user
spring.datasource.password=barber_password
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=create-drop
```

---

## 🐳 Polecenia Docker Compose

### Uruchomienie

```powershell
# Uruchom w tle (detached)
docker-compose up -d

# Uruchom z logami
docker-compose up

# Tylko zbuduj obraz
docker-compose build

# Zbuduj i uruchom
docker-compose up --build
```

### Zatrzymanie

```powershell
# Zatrzymaj kontenery
docker-compose stop

# Zatrzymaj i usuń kontenery
docker-compose down

# Usuń też wolumeny (baza danych)
docker-compose down -v

# Usuń wszystko (obrazy + kontenery + wolumeny)
docker-compose down -v --rmi all
```

### Logi

```powershell
# Logi aplikacji
docker-compose logs barber-app

# Logi PostgreSQL
docker-compose logs postgres

# Logi wszystkich usług
docker-compose logs

# Logi w czasie rzeczywistym
docker-compose logs -f

# Tylko ostatnie 100 linii
docker-compose logs --tail=100
```

### Dostęp do Kontenera

```powershell
# Terminal w kontenerze aplikacji
docker exec -it barber-app sh

# Terminal w kontenerze PostgreSQL
docker exec -it barber-postgres bash

# Połączenie do PostgreSQL
docker exec -it barber-postgres psql -U barber_user -d barberdb

# Sql query w PostgreSQL
docker exec -it barber-postgres psql -U barber_user -d barberdb -c "SELECT * FROM users;"
```

---

## 🔍 Troubleshooting

### Problem: Port już w użyciu

```powershell
# Zmień port w docker-compose.yml
# ports:
#   - "8081:8080"  ← Zmień z 8080 na 8081
```

### Problem: Baza nie inicjalizuje się

```powershell
# Czysty start
docker-compose down -v
docker-compose up --build
```

### Problem: Aplikacja nie widzi bazy

```powershell
# Sprawdź network
docker network ls
docker inspect barber_barber-network

# Sprawdź logowanie
docker-compose logs postgres
docker-compose logs barber-app
```

### Problem: Błąd "database does not exist"

```powershell
# Otwórz bash w Postgresie
docker exec -it barber-postgres bash

# Połącz się
psql -U barber_user -d barberdb

# Sprawdź tabele
\dt
```

---

## 📊 Architektura Docker

```
┌─────────────────────────────────────┐
│      docker-compose up              │
└──────────────┬──────────────────────┘
               │
        ┌──────┴──────┐
        ▼              ▼
   ┌────────┐    ┌──────────────┐
   │ Builder│    │ Docker Image │
   │ Maven  │    │  PostgreSQL  │
   │ Java   │    │   Container  │
   └────┬───┘    └──────┬───────┘
        │               │
        ▼               ▼
   ┌───────────────────────────┐
   │  Barber App Container     │
   │  Port: 8080               │
   │  Profile: docker          │
   └────────────┬──────────────┘
                │
        ┌───────┴────────┐
        ▼                 ▼
   ┌─────────────┐ ┌──────────────┐
   │   App JAR   │ │  PostgreSQL  │
   │   Running   │ │  Port: 5432  │
   └─────────────┘ └──────────────┘
```

---

## ✨ Szybkie Komendy

### Development (H2)
```bash
mvn clean spring-boot:run
```

### Production (PostgreSQL Local)
```bash
mvn clean spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=postgres"
```

### Production (Docker)
```bash
docker-compose up --build
```

### Czysty Start (Docker)
```bash
docker-compose down -v && docker-compose up --build
```

---

## 🔑 Konta Testowe (w Bazie)

Po inicjalizacji PostgreSQL dostępne są:

| Username | Password | Role |
|----------|----------|------|
| admin1 | adminpass123 | ADMIN |
| barber1 | barberpass123 | BARBER |
| klient1 | haslo123 | KLIENT |

---

## 📚 Dodatkowe Zasoby

- [Docker Docs](https://docs.docker.com/)
- [Docker Compose](https://docs.docker.com/compose/)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)
- [Spring Boot Profiles](https://spring.io/blog/2015/02/18/enabling-production-grade-features)

---

## ✅ Checklist Uruchomienia

### Przy pierwszym uruchomieniu:
- [ ] Zainstaluj Java 17+
- [ ] Zainstaluj Maven (lub używaj Maven Wrapper)
- [ ] Zainstaluj Docker Desktop (dla Docker)
- [ ] Klonuj/Pobierz projekt

### Przed uruchomieniem:
- [ ] Przejdź do katalogu projektu
- [ ] Sprawdź porty (8080, 5432)
- [ ] Czytaj logi podczas startu

### Po uruchomieniu:
- [ ] Otwórz http://localhost:8080
- [ ] Testuj endpointy z `demo.http`
- [ ] Sprawdzaj logi w razie błędów

---

Gotowe! 🚀
