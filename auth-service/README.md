# Auth-Service (SkillTrack)

Spring Boot service responsible for user registration, login, and JWT issuance.

## Endpoints

- `POST /api/auth/auth/register`
- `POST /api/auth/auth/login`
- `GET  /api/auth/auth/me`

## Swagger

- `GET /api/auth/swagger-ui.html`

## Environment

Configured via environment variables (see repo root `.env`):

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION`
- `CORS_ALLOWED_ORIGINS`
