# 🏃 QUICK START - URUCHOMIENIE APLIKACJI

## 3 OPCJE DO URUCHOMIENIA

### 1️⃣ **Szybko - Bez Dockera (H2 Database)**

```powershell
cd c:\Users\kondz1o\Desktop\Barber\Barber
.\mvnw.cmd spring-boot:run
```

✅ **Pros**: Szybko, bez dodatkowych zależności  
❌ **Cons**: Baza w pamięci RAM (tracona po restarcie)  
📌 **Kiedy**: Entwicklerskie, testowanie  

**URL**: http://localhost:8080

---

### 2️⃣ **Production - PostgreSQL lokalnie**

**Wymagania**: PostgreSQL zainstalowany

```powershell
# 1. Utwórz bazę (patrz DOCKER_POSTGRES_GUIDE.md)
# 2. Uruchom z profilem
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=postgres"
```

✅ **Pros**: Prawdziwa baza, dane trwałe  
❌ **Cons**: Muszę zainstalować PostgreSQL  
📌 **Kiedy**: Testowanie przed produkcją  

**URL**: http://localhost:8080

---

### 3️⃣ **Najlepsze - Docker + PostgreSQL**

**Wymagania**: Docker Desktop

```powershell
cd c:\Users\kondz1o\Desktop\Barber\Barber
docker-compose up
```

✅ **Pros**: Wszystko w kontenerach, reprodukowalne, production-like  
❌ **Cons**: Potrzebny Docker (~2GB RAM)  
📌 **Kiedy**: Production, CI/CD, zespołowe deployment  

**URL**: http://localhost:8080  
**DB**: PostgreSQL na :5432  

---

## 📊 PORÓWNANIE

| | H2 | PostgreSQL Local | Docker |
|---|----|----|---|
| Speed | ⚡⚡⚡ | ⚡⚡ | ⚡ |
| Setup | ✅ zero | ❌ zainstaluj | ❌ zainstaluj |
| Data Persistence | ❌ nie | ✅ tak | ✅ tak |
| Production Ready | ❌ nie | ⚠️ częściowo | ✅ tak |
| Network | lokalnie | localhost | Docker network |
| Effort | 30 sec | 5 min | 3-10 min |

---

## 🚀 REKOMENDACJE

**1. Pierwsza próba?**
```bash
# Użyj H2 - najszybciej
mvn clean spring-boot:run
```

**2. Chcesz trwałą bazę?**
```bash
# Zainstaluj PostgreSQL i użyj profilu postgres
```

**3. Chcesz production-like environment?**
```bash
# Docker compose - najlepsze dla zespołu i deployment
docker-compose up
```

---

## 📁 WAŻNE PLIKI

- 📖 `DOCKER_POSTGRES_GUIDE.md` - Pełny guide
- 📖 `JWT_MECHANISM_DEMO.md` - JWT tokens
- 💻 `demo.http` - Test API
- 🐳 `Dockerfile` - Docker image
- 🐳 `docker-compose.yml` - Docker setup
- ⚙️ `application.properties` - H2 config
- ⚙️ `application-postgres.properties` - PostgreSQL local
- ⚙️ `application-docker.properties` - Docker config

---

## 🔧 SZYBKIE KOMENDY

### Build
```bash
mvn clean install
mvn clean compile
```

### Test
```bash
mvn clean test
```

### Run
```bash
# Development (H2)
mvn clean spring-boot:run

# Production (PostgreSQL)
mvn clean spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=postgres"
```

### Docker
```bash
# Build & Run
docker-compose up --build

# Stop
docker-compose down

# Clean
docker-compose down -v
```

---

## ✅ CHECKLIST

Przed uruchomieniem:
- [ ] Java 17+ zainstalowana (`java -version`)
- [ ] Maven/mvnw dostępny (`mvn -version`)
- [ ] Port 8080 wolny
- [ ] Port 5432 wolny (dla Docker/PostgreSQL)

Przy uruchomieniu:
- [ ] Czytaj logi
- [ ] Czekaj na "Tomcat started"
- [ ] Otwórz http://localhost:8080

Po uruchomieniu:
- [ ] Przetestuj `demo.http`
- [ ] Zaloguj się jako test user
- [ ] Sprawdzaj JWT tokeny

---

## 🆘 PROBLEMY?

### Port już zajęty
```bash
# Zmień port w docker-compose.yml lub application.properties
```

### Cannot connect to database
```bash
# Sprawdź czy PostgreSQL działa
docker-compose logs postgres
```

### Aplikacja się crasha
```bash
# Czytaj logi
docker-compose logs barber-app

# Lub dla lokalnego uruchomienia
mvn clean spring-boot:run -X  # verbose mode
```

---

## 📞 WIĘCEJ INFO

- **JWT**: Czytaj `JWT_MECHANISM_DEMO.md`
- **Docker**: Czytaj `DOCKER_POSTGRES_GUIDE.md`
- **API**: Czytaj `demo.http`

---

**Gotowe do testowania? 🚀**

Wybierz opcję powyżej i zacznij!
