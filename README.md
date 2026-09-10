# User Service

Part of the **Book-Store microservices ecosystem**. This service is responsible for **customer registration, authentication, and JWT issuance/validation**, and acts as the identity provider for all other Book-Store microservices (`inventory-service`, `order-service`, `payment-service`, `notification-service`) and the API Gateway.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Features](#features)
- [Project Structure](#project-structure)
- [API Endpoints](#api-endpoints)
- [Security Design](#security-design)
- [Database Schema](#database-schema)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Running the Service](#running-the-service)
- [Testing the API](#testing-the-api)
- [Roadmap](#roadmap)

---

## Overview

`user-service` provides a REST API for:

- **User registration** with hashed password storage
- **Sign-in** with credential validation and JWT issuance
- **Stateless authentication** across all microservices via **RS256-signed JWTs**
- **JWKS (JSON Web Key Set) exposure**, so downstream services can validate tokens without ever sharing a private key or calling back into this service
- **Logout / token revocation**, solving the classic "JWTs can't be invalidated" problem via a database-backed blacklist

This service is designed to be consumed by:
- An **API Gateway** (e.g. Spring Cloud Gateway) sitting in front of all microservices
- Other backend microservices that need to validate an incoming `Authorization: Bearer <token>` header
- An **Angular** front-end application

---

## Tech Stack

| Category | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Security | Spring Security, OAuth2 Resource Server |
| Persistence | Spring Data JPA, MySQL |
| JWT | JJWT (`io.jsonwebtoken`) for signing, Nimbus JOSE+JWT for JWKS |
| Build Tool | Gradle |
| Boilerplate reduction | Lombok |

---

## Architecture

```
                     ┌─────────────────────┐
                     │     Angular UI       │
                     └──────────┬───────────┘
                                │ HTTPS
                     ┌──────────▼───────────┐
                     │     API Gateway        │
                     │  (validates JWT via    │
                     │   user-service JWKS)   │
                     └──────────┬───────────┘
        ┌───────────┬───────────┼───────────┬───────────┐
        ▼           ▼           ▼           ▼           ▼
  user-service  inventory-  order-      payment-   notification-
  (this repo)   service     service     service     service
```

**Key principle:** `user-service` is the *only* service that holds the RSA **private** key and issues tokens. Every other service (including the gateway) only ever needs the **public** key, fetched from this service's `/.well-known/jwks.json` endpoint, to independently verify a token's signature — no network call back to `user-service` is required per request.

---

## Features

- [x] User registration with input validation (`jakarta.validation`)
- [x] BCrypt password hashing (passwords are never stored or returned in plaintext)
- [x] Sign-in with email + password, returns a signed JWT
- [x] **RS256 asymmetric JWT signing** (production-safe for multi-service validation)
- [x] **JWKS endpoint** (`/.well-known/jwks.json`) for downstream signature verification
- [x] **Logout / server-side token revocation** using a `jti`-based blacklist
- [x] Scheduled cleanup job to purge expired revoked-token records
- [x] Stateless session management (`SessionCreationPolicy.STATELESS`)
- [x] Centralized exception handling with consistent JSON error responses
- [x] Bean validation on all request DTOs (`@NotBlank`, `@Email`, `@Size`, etc.)

---

## Project Structure

```
src/main/java/com/book_store/user_service/
├── configuration/
│   ├── SecurityBeanConfig.java     # PasswordEncoder bean (BCrypt)
│   ├── WebSecurityConfig.java      # Security filter chain, public/protected routes
│   └── JwtDecoderConfig.java       # Custom JwtDecoder enforcing revocation checks
├── controllers/
│   ├── UserController.java         # register / login / logout / get-user endpoints
│   └── JwksSetController.java      # Exposes public key as JWKS
├── dto/
│   ├── UserDto.java                # Registration payload / response
│   ├── SigninRequest.java          # Login payload
│   └── TokenResponse.java          # Login response (JWT + metadata)
├── entities/
│   ├── User.java                   # Customer entity
│   └── RevokedToken.java           # Blacklisted JWT (jti + expiry)
├── exceptions/
│   └── GlobalExceptionHandler.java # Centralized error responses
├── repository/
│   ├── UserRepository.java
│   └── RevokedTokenRepository.java
├── security/
│   └── RsaKeyProvider.java         # Loads/generates the RSA signing key pair
├── service/
│   ├── UserService.java            # Registration, sign-in, logout orchestration
│   ├── JWTService.java             # Token generation (RS256)
│   └── TokenBlacklistService.java  # Revocation store + scheduled cleanup
└── UserServiceApplication.java
```

---

## API Endpoints

Base path: `/api/user`

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/user/register` | No | Register a new customer account |
| `POST` | `/api/user/login` | No | Authenticate and receive a JWT |
| `POST` | `/api/user/logout` | Yes (Bearer token) | Revoke the presented access token |
| `GET`  | `/api/user/{userId}` | Yes (Bearer token) | Fetch a user profile by ID |
| `GET`  | `/.well-known/jwks.json` | No | Public key set for JWT signature verification |

### Register

```http
POST /api/user/register
Content-Type: application/json

{
  "name": "Kiran Budupula",
  "email": "kiran@example.com",
  "password": "StrongPass123",
  "mobileNumber": "9876543210",
  "gender": "Male"
}
```

**Response `201 Created`**
```json
{
  "id": "b3f1c9e4-...",
  "name": "Kiran Budupula",
  "email": "kiran@example.com",
  "role": "CUSTOMER",
  "mobileNumber": "9876543210",
  "gender": "Male"
}
```

### Login

```http
POST /api/user/login
Content-Type: application/json

{
  "email": "kiran@example.com",
  "password": "StrongPass123"
}
```

**Response `200 OK`**
```json
{
  "token": "eyJhbGciOiJSUzI1NiIsImtpZCI6...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": "b3f1c9e4-...",
  "role": "CUSTOMER",
  "name": "Kiran Budupula",
  "email": "kiran@example.com"
}
```

### Logout

```http
POST /api/user/logout
Authorization: Bearer <token>
```

**Response `200 OK`**
```json
{ "message": "Logged out successfully" }
```

Once revoked, the same token is rejected on any subsequent request (`401 Unauthorized`), even though it hasn't reached its natural expiry.

### Get user by ID

```http
GET /api/user/{userId}
Authorization: Bearer <token>
```

---

## Security Design

### Password storage
Passwords are hashed with **BCrypt** (`PasswordEncoder` bean) before persistence. Plaintext passwords are never stored or returned in any API response (`UserDto.password` is explicitly nulled out before being sent back to clients).

### JWT signing — RS256, not a shared secret
Tokens are signed using an **RSA private key** (`RsaKeyProvider` + `JWTService.generateTokenUsingRSA`). This avoids the security risk of distributing one shared HMAC secret across every microservice — only `user-service` ever holds the private key.

> ⚠️ **Local development note:** if `jwt.rsa.private-key` / `jwt.rsa.public-key` are left blank, `RsaKeyProvider` generates an **ephemeral** in-memory key pair on startup. This is convenient for local testing but means tokens become invalid on every restart, and no other service can validate them. **Configure real, persistent keys via environment variables before deploying or before wiring up other services against this one.**

### JWKS endpoint
`GET /.well-known/jwks.json` exposes only the **public** key, formatted as a standard JSON Web Key Set. Any service using Spring's `spring-boot-starter-oauth2-resource-server` can point `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` at this URL and validate tokens with zero shared secrets and zero network calls per request (the key set is cached).

### Token revocation (logout)
JWTs are stateless by nature and normally can't be "deleted." This service solves that with:
1. Every issued token carries a unique `jti` (JWT ID) claim.
2. `POST /api/user/logout` records that `jti` in the `revoked_tokens` table.
3. A custom `JwtDecoder` bean (`JwtDecoderConfig`) checks every incoming token's `jti` against this table **in addition to** standard expiry validation.
4. An hourly `@Scheduled` job purges rows past their natural expiry, since an expired token would be rejected anyway.

### Stateless sessions
`SessionCreationPolicy.STATELESS` — no server-side HTTP session is created; all authentication state travels in the JWT itself.

### Known items to harden before production
- [ ] Move the leftover `jwt.secret` (HMAC key, currently unused for signing but still loaded by `JWTService`) out of `application.properties` and into an environment variable — **do not commit real secrets to source control**.
- [ ] Persist and inject real RSA keys via `JWT_RSA_PRIVATE_KEY` / `JWT_RSA_PUBLIC_KEY` environment variables in every non-local environment.
- [ ] Add a refresh-token flow so short-lived access tokens don't force frequent re-logins.
- [ ] Add rate limiting on `/login` to mitigate brute-force attempts.

---

## Database Schema

The `users` table is managed by Hibernate. The `revoked_tokens` table currently requires **manual creation** (`spring.jpa.hibernate.ddl-auto` is disabled by default — see `application.properties`):

```sql
CREATE TABLE revoked_tokens (
    jti         VARCHAR(255) NOT NULL PRIMARY KEY,
    expires_at  DATETIME(6)  NOT NULL,
    revoked_at  DATETIME(6)  NOT NULL
);
```

---

## Getting Started

### Prerequisites
- JDK 21
- MySQL 8.x running locally (or update `spring.datasource.url` to point elsewhere)
- Gradle Wrapper (bundled — no local Gradle install required)

### Clone and build

```powershell
git clone https://github.com/vinod-coder-114/book-store-user-service.git
cd book-store-user-service
./gradlew build
```

---

## Configuration

All configuration lives in `src/main/resources/application.properties`. Key properties:

```properties
server.port=8081

spring.datasource.url=jdbc:mysql://localhost:3306/book_store
spring.datasource.username=root
spring.datasource.password=Root

jwt.expiration=3600000
jwt.rsa.private-key=${JWT_RSA_PRIVATE_KEY:}
jwt.rsa.public-key=${JWT_RSA_PUBLIC_KEY:}
jwt.rsa.key-id=${JWT_KEY_ID:user-service-key-1}

spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8081/.well-known/jwks.json
```

### Generating an RSA key pair for local/production use (PowerShell + OpenSSL)

```powershell
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out private_key.pem
openssl pkcs8 -topk8 -nocrypt -in private_key.pem -outform DER -out private_key_pkcs8.der
openssl rsa -pubout -in private_key.pem -outform DER -out public_key.der

$priv = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes("private_key_pkcs8.der"))
$pub  = [Convert]::ToBase64String([System.IO.File]::ReadAllBytes("public_key.der"))

$env:JWT_RSA_PRIVATE_KEY = $priv
$env:JWT_RSA_PUBLIC_KEY  = $pub
```

Without these set, the service will start with a **warning** and use an ephemeral key pair — fine for a quick local smoke test, not fine for anything shared or persistent.

---

## Running the Service

```powershell
./gradlew bootRun
```

The service starts on **`http://localhost:8081`** by default.

---

## Testing the API

Using `curl` from PowerShell:

```powershell
# Register
curl -X POST http://localhost:8081/api/user/register `
  -H "Content-Type: application/json" `
  -d '{"name":"Test User","email":"test@example.com","password":"Password123","mobileNumber":"9999999999"}'

# Login
curl -X POST http://localhost:8081/api/user/login `
  -H "Content-Type: application/json" `
  -d '{"email":"test@example.com","password":"Password123"}'

# Fetch profile (replace <TOKEN> with the token from login response)
curl http://localhost:8081/api/user/<USER_ID> -H "Authorization: Bearer <TOKEN>"

# Logout
curl -X POST http://localhost:8081/api/user/logout -H "Authorization: Bearer <TOKEN>"

# Confirm token is now rejected
curl http://localhost:8081/api/user/<USER_ID> -H "Authorization: Bearer <TOKEN>"
```

---

## Roadmap

- [ ] Refresh token issuance and rotation
- [ ] Role-based authorization (`ADMIN` vs `CUSTOMER`) on protected endpoints
- [ ] Spring Cloud Gateway integration for centralized routing across all Book-Store microservices
- [ ] Service discovery (Eureka) once additional microservices come online
- [ ] Centralized configuration server
- [ ] Docker Compose setup for local multi-service development
- [ ] CI pipeline (build, test, dependency vulnerability scan)

---

## License

This project is part of a personal learning/practice initiative for a microservices-based Book-Store application.
