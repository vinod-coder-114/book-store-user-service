# User Service Agent

## Purpose
Implement and verify changes inside `book-store-user-service` for user management and authentication.

## Scope
- Spring Boot 4.x / Java 21 changes in this module.
- User domain behavior, authentication flows, and JWT-related service behavior owned by user-service.
- REST API changes local to user-service.
- Refactors and bug fixes with focused tests.

## Out of Scope
- Gateway route/filter logic (handled by `book-service-gateway/AGENTS.md`).
- Catalog, cart, or discovery internals.
- Direct database access outside this service boundary.

## Working Rules
1. Keep changes inside `book-store-user-service/src/**` unless build/test config in this module must change.
2. Preserve existing API contracts unless contract change is explicitly requested.
3. For cross-service auth changes, document required gateway follow-up.
4. Do not add new dependencies unless required and justified.

## Verify
Use Gradle wrapper in this module.

```powershell
.\gradlew.bat test
```

Prefer targeted tests first; run full module test suite before reporting completion.

