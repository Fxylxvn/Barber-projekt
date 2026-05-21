# Analiza zależności projektu Barber

> **Data analizy:** 21 maja 2026  
> **Wersja projektu:** 0.0.1-SNAPSHOT  
> **Java:** 17  
> **Build tool:** Maven (wrapper)

---

## 1. Tabela zależności

| # | Nazwa komponentu | Wersja | Opis |
|---|-----------------|--------|------|
| 1 | **Spring Boot Starter Parent** | 4.0.6 | Główny framework aplikacji — zarządza wersjami wszystkich starterów Spring Boot oraz dostarcza domyślną konfigurację budowania projektu. |
| 2 | **spring-boot-starter-data-jpa** | 4.0.6 (zarządzana) | Starter do integracji z bazą danych za pomocą JPA/Hibernate — zapewnia ORM, repozytoria Spring Data i zarządzanie transakcjami. |
| 3 | **spring-boot-starter-security** | 4.0.6 (zarządzana) | Starter bezpieczeństwa — dostarcza uwierzytelnianie, autoryzację, ochronę CSRF i integrację z Spring Security. |
| 4 | **spring-boot-starter-thymeleaf** | 4.0.6 (zarządzana) | Starter silnika szablonów Thymeleaf — umożliwia renderowanie dynamicznych widoków HTML po stronie serwera. |
| 5 | **spring-boot-starter-validation** | 4.0.6 (zarządzana) | Starter walidacji danych — integruje Bean Validation (Hibernate Validator) do automatycznej walidacji formularzy i DTO. |
| 6 | **spring-boot-starter-webmvc** | 4.0.6 (zarządzana) | Starter webowy oparty na Servlet API — dostarcza Spring MVC, embedded Tomcat i obsługę REST/HTTP. |
| 7 | **thymeleaf-extras-springsecurity6** | zarządzana przez BOM | Rozszerzenie Thymeleaf integrujące Spring Security — umożliwia warunkowe wyświetlanie elementów UI w zależności od ról/uprawnień użytkownika. |
| 8 | **jjwt-api** | 0.13.0 | Biblioteka JSON Web Token (API) — definiuje interfejsy do tworzenia, parsowania i walidacji tokenów JWT. |
| 9 | **jjwt-impl** | 0.13.0 | Implementacja JJWT — zawiera logikę budowania i weryfikacji tokenów JWT (zależność runtime). |
| 10 | **jjwt-jackson** | 0.13.0 | Moduł serializacji JJWT oparty na Jacksonie — obsługuje konwersję JSON ↔ obiekty Java w kontekście JWT. |
| 11 | **spring-boot-devtools** | 4.0.6 (zarządzana) | Narzędzia deweloperskie — automatyczny restart aplikacji, LiveReload i uproszczone debugowanie (zależność opcjonalna, runtime). |
| 12 | **H2 Database** | zarządzana przez BOM | Lekka, osadzana baza danych SQL — używana jako baza testowa/deweloperska (in-memory lub plikowa). |
| 13 | **PostgreSQL JDBC Driver** | zarządzana przez BOM | Sterownik JDBC do bazy PostgreSQL — umożliwia połączenie aplikacji z produkcyjną bazą danych PostgreSQL. |
| 14 | **Project Lombok** | zarządzana przez BOM | Biblioteka kompilacyjna — automatycznie generuje gettery, settery, konstruktory, `equals()`, `hashCode()` i inne metody boilerplate. |
| 15 | **spring-boot-h2console** | 4.0.6 (zarządzana) | Moduł konsoli H2 — dostarcza webowy interfejs do przeglądania i zarządzania bazą danych H2 w przeglądarce. |

### Infrastruktura (Docker)

| # | Komponent | Wersja | Opis |
|---|-----------|--------|------|
| 1 | **PostgreSQL (Docker)** | 16-alpine | Kontener bazy danych PostgreSQL w wariancie Alpine Linux — lekki obraz produkcyjny. |
| 2 | **Maven (builder)** | 3.9.6-eclipse-temurin-17 | Obraz budujący — kompiluje projekt Maven z JDK 17 (Eclipse Temurin). |
| 3 | **Eclipse Temurin JRE** | 17-jre-jammy | Obraz runtime — uruchamia skompilowany JAR na JRE 17 (Ubuntu Jammy). |

---

## 2. Trzy przykłady podatności, które występowały wcześniej w projekcie

> Poniższe podatności dotyczyły projektu Barber, gdy korzystał ze **Spring Boot 4.0.2** i **JJWT 0.12.5**.  
> Po aktualizacji do **Spring Boot 4.0.6** i **JJWT 0.13.0** zostały one **naprawione** i nie występują w obecnej wersji projektu.

---

### ✅ Przykład 1: CVE-2026-40976 — Authorization Bypass w Spring Boot (NAPRAWIONE)

| Pole | Wartość |
|------|---------|
| **Dotyczyło** | Spring Boot 4.0.0 – 4.0.5 |
| **Poprzednia wersja w projekcie** | 4.0.2 (**była dotknięta**) |
| **Obecna wersja w projekcie** | 4.0.6 (**naprawiona**) |
| **Typ** | Obejście autoryzacji (Authorization Bypass) |
| **Ważność** | Krytyczna |

**Opis podatności:**  
Podatność w domyślnej konfiguracji bezpieczeństwa webowego Spring Boot pozwalała na nieautoryzowany dostęp do endpointów aplikacji w deploymentach opartych na Servlet API. Atakujący mógł ominąć mechanizmy autoryzacji i uzyskać dostęp do chronionych zasobów bez odpowiednich uprawnień.

**Status:** ✅ Naprawione — aktualizacja Spring Boot z 4.0.2 → 4.0.6 wyeliminowała tę podatność.

---

### ✅ Przykład 2: CVE-2024-1597 — SQL Injection w PostgreSQL JDBC Driver (NAPRAWIONE)

| Pole | Wartość |
|------|---------|
| **Dotyczyło** | PostgreSQL JDBC Driver (pgjdbc) < 42.7.2 |
| **Poprzednia wersja w projekcie** | zarządzana przez Spring Boot 4.0.2 BOM (potencjalnie podatna) |
| **Obecna wersja w projekcie** | zarządzana przez Spring Boot 4.0.6 BOM (≥ 42.7.2, naprawiona) |
| **Typ** | SQL Injection |
| **Ważność** | Krytyczna (CVSS 9.8) |

**Opis podatności:**  
Krytyczna podatność SQL Injection w sterowniku JDBC PostgreSQL występująca przy konfiguracji `preferQueryMode=SIMPLE` (ustawienie niedomyślne). Sterownik nieprawidłowo obsługiwał parametry zapytań — konkretnie wartości numeryczne poprzedzone znakiem minus i następujące po nich ciągi znaków w tej samej linii. Atakujący mógł obejść ochronę parametryzowanych zapytań i wstrzyknąć złośliwy kod SQL, co potencjalnie prowadziło do wycieku, modyfikacji lub usunięcia danych z bazy.

**Kontekst dla projektu Barber:**  
Domyślna konfiguracja sterownika (`preferQueryMode=extended`) nie była bezpośrednio narażona, jednak używanie starszej wersji sterownika stanowiło ryzyko w przypadku zmiany konfiguracji połączenia.

**Status:** ✅ Naprawione — Spring Boot 4.0.6 BOM dostarcza sterownik PostgreSQL JDBC w wersji ≥ 42.7.2, w której podatność została usunięta.

---

### ✅ Przykład 3: CVE-2026-40972 — Timing Attack w Spring Boot DevTools (NAPRAWIONE)

| Pole | Wartość |
|------|---------|
| **Dotyczyło** | Spring Boot 4.0.0 – 4.0.5 |
| **Poprzednia wersja w projekcie** | 4.0.2 (**była dotknięta**) |
| **Obecna wersja w projekcie** | 4.0.6 (**naprawiona**) |
| **Typ** | Timing Attack → potencjalny Remote Code Execution (RCE) |
| **Ważność** | Wysoka |

**Opis podatności:**  
Mechanizm porównywania sekretu zdalnego w module DevTools był podatny na ataki czasowe (timing attacks). Porównanie sekretów nie było realizowane w czasie stałym (*constant-time comparison*), co pozwalało atakującemu na stopniowe odgadywanie sekretu bajt po bajcie poprzez precyzyjne mierzenie czasu odpowiedzi serwera. W skrajnych przypadkach mogło to prowadzić do zdalnego wykonania kodu (RCE) przez przejęcie sesji DevTools i wstrzyknięcie zmienionego kodu aplikacji.

**Kontekst dla projektu Barber:**  
DevTools jest oznaczony jako zależność `runtime` z flagą `optional=true`, więc nie powinien być dołączany do produkcyjnych buildów JAR. Jednak w środowisku deweloperskim podatność była aktywna i mogła zostać wykorzystana przez atakującego w sieci lokalnej.

**Status:** ✅ Naprawione — aktualizacja Spring Boot z 4.0.2 → 4.0.6 wyeliminowała tę podatność (porównanie sekretu jest teraz realizowane w czasie stałym).

---

## 3. Podsumowanie wykonanych zmian

> ✅ **Wszystkie opisane podatności zostały naprawione dzięki aktualizacji zależności projektu.**

### Wykonane aktualizacje w `pom.xml`:

| Komponent | Poprzednia wersja | Nowa wersja | Naprawione CVE |
|-----------|-------------------|-------------|----------------|
| **Spring Boot** | 4.0.2 | **4.0.6** | CVE-2026-40976, CVE-2026-40972, CVE-2026-40973, CVE-2026-40975, CVE-2026-40977 |
| **JJWT (api, impl, jackson)** | 0.12.5 | **0.13.0** | Poprawa ogólnego bezpieczeństwa, eliminacja spornego CVE-2024-31033 |
| **PostgreSQL JDBC** | zarządzana (starsza) | zarządzana przez BOM 4.0.6 | CVE-2024-1597 |

### Dodatkowe zalecenia:

- Uruchomić `mvn dependency:tree` aby zweryfikować faktyczne wersje zależności tranzytywnych po aktualizacji.
- Rozważyć dodanie pluginu `org.owasp:dependency-check-maven` do automatycznego skanowania podatności w CI/CD.
- Nie wystawiać konsoli H2 na publiczny adres sieciowy (ryzyko RCE — historyczne CVE-2021-42392).
- Dodać jawne wykluczenie DevTools z buildu produkcyjnego w `spring-boot-maven-plugin`.
