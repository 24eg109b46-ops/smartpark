# SmartPark - Backend Service

> **Java 17 | Spring Boot 3.3.4 | Spring Data JPA | Spring Security 6 | MySQL 8 / H2**

The core REST API and business logic backend for SmartPark.

---

## 🚀 Quick Start (Local Development)

### Prerequisites
- **Java 17+** (JDK 17 or higher)
- **Maven 3.8+** (or use system `mvn`)

### 1. Build and Run Tests
```bash
mvn clean test
```
All 17 unit and MockMvc integration tests will execute using an in-memory H2 database.

### 2. Start Application Locally
```bash
mvn spring-boot:run --spring.profiles.active=local
```
The server starts on port **8080**:
- **REST APIs**: `http://localhost:8080/api/*`
- **H2 Console**: `http://localhost:8080/h2-console` (`JDBC URL: jdbc:h2:mem:smartparkdb`)
- **Web App**: `http://localhost:8080/`

---

## 🐳 Docker Build

```bash
docker build -t smartpark-backend .
docker run -p 8080:8080 -e PORT=8080 -e SPRING_PROFILES_ACTIVE=local smartpark-backend
```

---

## ☁️ Deployment to Render

1. Create a **Web Service** on Render pointing to your repository.
2. Configure settings:
   - **Root Directory**: `backend`
   - **Runtime**: `Docker`
   - **Dockerfile Path**: `Dockerfile`
3. Environment variables:
   - `PORT`: `10000`
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `DB_URL`: `jdbc:mysql://<HOST>:<PORT>/smartpark?useSSL=true&serverTimezone=UTC`
   - `DB_USER`: `<MYSQL_USER>`
   - `DB_PASS`: `<MYSQL_PASSWORD>`

---

## 🔑 Default Seeded Accounts

- **Admin**: `admin@smartpark.com` / `Admin@123`
- **Operator**: `operator@smartpark.com` / `Operator@123`
- **Driver**: `driver@smartpark.com` / `Driver@123`
