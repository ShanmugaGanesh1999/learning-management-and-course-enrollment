# SkillTrack Project - Quick Reference & Deployment Guide

## QUICK REFERENCE: WHAT'S IN THIS REPO

This repository includes:

- `README.md` (this file): deployment guide, testing steps, security checklist
- `skilltrack.txt`: requirements + architecture overview

---

## PROJECT STRUCTURE OVERVIEW

```
skilltrack-project/
├── auth-service/              # Spring Boot microservice
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── course-service/            # Spring Boot microservice
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── enrollment-service/        # Spring Boot microservice
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── api-gateway/              # Spring Cloud Gateway
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── skilltrack-ui/            # Angular 17 frontend
│   ├── src/
│   ├── angular.json
│   ├── package.json
│   └── Dockerfile
├── docker-compose.yml        # Docker orchestration
├── .env.example             # Environment template
└── README.md                # Project documentation
```

---

## ENVIRONMENT VARIABLES CHECKLIST

**Before starting, configure all environment variables in `.env` file:**

```bash
# Database Configuration
- MYSQL_ROOT_PASSWORD          (Production: strong random password, min 16 chars)
- MYSQL_DATABASE               (Production: secure db name)
- MYSQL_USER                   (Production: non-root user)
- MYSQL_PASSWORD               (Production: strong random password)

# JWT Security
- JWT_SECRET                   (CRITICAL: 256+ bit random string)
                               # Generate: openssl rand -base64 32
- JWT_EXPIRATION               (Default: 86400000 = 24 hours)
- JWT_REFRESH_EXPIRATION       (Default: 604800000 = 7 days)

# Spring Configuration
- SPRING_PROFILE               (dev | test | prod)
                               # dev: detailed logging, debug enabled
                               # prod: minimal logging, optimized

# CORS Configuration
- CORS_ALLOWED_ORIGINS         (Production: actual domain, not localhost)
                               # Example: https://example.com,https://www.example.com

# Service URLs
- AUTH_SERVICE_URL             (Internal: http://auth-service:8001)
- COURSE_SERVICE_URL           (Internal: http://course-service:8002)
- ENROLLMENT_SERVICE_URL       (Internal: http://enrollment-service:8003)
- API_GATEWAY_URL              (Internal: http://api-gateway:8000)

# Frontend Configuration
- ANGULAR_API_BASE_URL         (Internal: http://api-gateway:8000/api)
                               # Production: actual API domain
```

---

## DEPLOYMENT STEP-BY-STEP GUIDE

### Step 1: Pre-Deployment Verification

```bash
# 1. Verify all required files exist
- docker-compose.yml
- .env file
- Dockerfiles for services and UI
- Maven pom.xml files
- Angular package.json and angular.json

# 2. Verify environment variables are set
cat .env
# Should show all required variables set with production values

# 3. Verify Docker is installed
docker --version
docker-compose --version

# 4. Generate secure JWT secret (if not already done)
openssl rand -base64 32
# Copy output to JWT_SECRET in .env
```

### Step 2: Build and Start Services

```bash
# Navigate to project root
cd learning-management-and-course-enrollment

# Build all services
docker-compose build

# Start all services (detached mode)
docker-compose up -d

# Monitor startup logs
docker-compose logs -f

# Expected output:
# mysql          | ready for connections
# auth-service   | Tomcat started on port(s): 8001
# course-service | Tomcat started on port(s): 8002
# enrollment-service | Tomcat started on port(s): 8003
# api-gateway    | Tomcat started on port(s): 8000
# frontend       | Running on port 80
```

### Step 3: Verify Service Health

```bash
# Check health endpoints
curl http://localhost:8001/api/auth/actuator/health    # Should return 200 OK
curl http://localhost:8002/api/courses/actuator/health # Should return 200 OK
curl http://localhost:8003/api/enrollments/actuator/health
curl http://localhost:8000/actuator/health
curl http://localhost:4200                              # Frontend

# Check container status
docker-compose ps

# All containers should show "Up" status
```

### Step 4: Initialize Database

```bash
# Access MySQL container
docker-compose exec mysql mysql -u root -p$MYSQL_ROOT_PASSWORD

# Inside MySQL shell, verify databases are created:
SHOW DATABASES;
# Should show: skilltrack_auth, skilltrack_courses, skilltrack_enrollments

# Verify tables are created (Spring Data JPA creates them automatically):
USE skilltrack_auth;
SHOW TABLES;
# Should show: users, roles, etc.

# Insert default admin user (optional):
# Prefer creating users through the /api/auth/register endpoint.

# Exit MySQL
exit;
```

### Step 5: Test Core Functionality

#### 5.1 Test Auth-Service

```bash
# 1. Register new user
curl -X POST http://localhost:8001/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123456",
    "email": "test@example.com",
    "fullName": "Test User",
    "role": "STUDENT"
  }'

# Expected response: 201 Created with user details

# 2. Login
curl -X POST http://localhost:8001/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123456"
  }'

# Expected response: 200 OK with an access token
# Copy the access token from the response and pass it as: Authorization: Bearer ACCESS_TOKEN_FROM_LOGIN

# 3. Get current user (secured endpoint)
curl -X GET http://localhost:8001/api/auth/me \
  -H "Authorization: Bearer ACCESS_TOKEN_FROM_LOGIN"

# Expected response: 200 OK with user profile
```

#### 5.2 Test Course-Service

```bash
# Use the access token from the auth login response.

# 1. Get published courses (public endpoint)
curl -X GET "http://localhost:8002/api/courses?page=0&size=10"

# Expected response: 200 OK with PageResponse

# 2. Create course (as instructor)
curl -X POST http://localhost:8002/api/courses \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Learn Angular",
    "description": "Complete Angular tutorial for beginners",
    "category": "Programming",
    "level": "BEGINNER",
    "price": 29.99
  }'

# Expected response: 201 Created with course details
```

#### 5.3 Test Enrollment-Service

```bash
# 1. Enroll in course
curl -X POST http://localhost:8003/api/enrollments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "courseId": 1
  }'

# Expected response: 201 Created with enrollment details

# 2. Get my enrollments
curl -X GET "http://localhost:8003/api/enrollments/my?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# Expected response: 200 OK with student's courses

# 3. Update progress
curl -X PATCH http://localhost:8003/api/enrollments/1/progress \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "progressPercentage": 50
  }'

# Expected response: 200 OK with updated enrollment
```

#### 5.4 Test API Gateway

```bash
# Gateway should route requests to appropriate services

# Test auth endpoint through gateway
curl -X POST http://localhost:8000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123456"
  }'

# Test course endpoint through gateway
curl -X GET "http://localhost:8000/api/courses?page=0&size=10"
```

#### 5.5 Test Angular Frontend

```bash
# Open browser and navigate to:
http://localhost:4200

# You should see:
- Login page
- Register link
- Course catalog (after login)
- Navigation menu
- User profile (after login)
```

### Step 6: Performance Testing

```bash
# Test pagination with large datasets
curl "http://localhost:8002/api/courses?page=0&size=50&sort=createdAt,desc"

# Test filtering
curl "http://localhost:8002/api/courses?category=Programming&level=BEGINNER"

# Test search with debounce
curl "http://localhost:8002/api/courses?keyword=angular"

# Check response time (should be < 500ms)
time curl "http://localhost:8002/api/courses"

# Load test (optional, use Apache JMeter or similar)
# 100 requests to course list endpoint
# Expected: < 5% error rate, avg response time < 500ms
```

---

## SECURITY VERIFICATION CHECKLIST

### Authentication & Authorization

- [ ] JWT secret is strong (256+ bits)
- [ ] JWT secret is NOT hardcoded (use .env)
- [ ] Passwords are hashed with BCrypt (strength 12)
- [ ] CORS is properly configured (not wildcard in prod)
- [ ] Authorization header is properly validated
- [ ] Role-based access control is enforced
- [ ] Ownership verification is implemented (users can only access own data)
- [ ] 401 errors returned for expired/invalid tokens

### Data Protection

- [ ] SQL injection prevented (parameterized queries)
- [ ] XSS protection enabled (Angular sanitization)
- [ ] CSRF protection configured (if using sessions)
- [ ] Input validation on all endpoints
- [ ] Sensitive data not logged (passwords, tokens, PII)
- [ ] HTTPS enforced in production
- [ ] Database connection is secure

### Infrastructure Security

- [ ] Non-root user in Docker containers
- [ ] Docker network isolation enabled
- [ ] Health checks configured
- [ ] Resource limits set
- [ ] No secrets in Dockerfile
- [ ] .env file not in version control (.gitignore)
- [ ] Database backups configured

### API Security

- [ ] Rate limiting configured (optional but recommended)
- [ ] Request size limits enforced
- [ ] Error messages don't expose stack traces
- [ ] Security headers set (X-Frame-Options, X-Content-Type-Options)
- [ ] File upload validation (if applicable)
- [ ] API versioning strategy in place

---

## LOGGING & MONITORING CHECKLIST

### Logging Configuration

```yaml
# application.yml logging levels

# Production logging (minimal, performance-focused)
logging:
  level:
    root: WARN
    com.skilltrack: INFO
    org.springframework.web: WARN
    org.springframework.security: WARN
  pattern: "[%d{yyyy-MM-dd HH:mm:ss}] %-5p %logger{36} - %msg%n"
  file: /var/log/skilltrack/application.log

# Development logging (detailed, debug-focused)
logging:
  level:
    root: INFO
    com.skilltrack: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
```

### Important Logs to Monitor

- [ ] Authentication failures (security.log)
- [ ] Authorization failures (access denied)
- [ ] Database connection errors
- [ ] Service-to-service communication failures
- [ ] API response times (> 1s)
- [ ] Error rates (> 5% error rate indicates issue)

---

## DATABASE BACKUP STRATEGY

```bash
# Daily automated backup
# Create backup script: backup-db.sh

#!/bin/bash
BACKUP_DIR="/backups/mysql"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/skilltrack_backup_$TIMESTAMP.sql"

docker-compose exec -T mysql mysqldump \
  -u root -p$MYSQL_ROOT_PASSWORD \
  --all-databases > $BACKUP_FILE

# Compress backup
gzip $BACKUP_FILE

# Keep only last 30 days of backups
find $BACKUP_DIR -name "*.sql.gz" -mtime +30 -delete

# Restore from backup
gunzip /backups/mysql/skilltrack_backup_20240101_000000.sql.gz
docker-compose exec -T mysql mysql -u root -p$MYSQL_ROOT_PASSWORD < /backups/mysql/skilltrack_backup_20240101_000000.sql
```

---

## TROUBLESHOOTING GUIDE

### Issue: Services not starting

```bash
# Check logs
docker-compose logs

# Check if ports are already in use
lsof -i :8001
lsof -i :8002
lsof -i :8003
lsof -i :4200

# Kill process using port (if needed)
kill -9 "$(lsof -ti :8001)" 2>/dev/null || true

# Restart services
docker-compose restart
```

### Issue: Database connection errors

```bash
# Check MySQL is running and healthy
docker-compose ps mysql
docker-compose logs mysql

# Verify connection string in application.yml
# Format: jdbc:mysql://hostname:3306/database

# Test connection from application container
docker-compose exec auth-service curl mysql:3306
```

### Issue: JWT token validation failing

```bash
# Verify JWT_SECRET matches in all services
grep JWT_SECRET .env
grep jwt.secret auth-service/src/main/resources/application.yml

# JWT secret must be identical across auth, gateway, and other services
# Generate new secret if mismatch:
openssl rand -base64 32
```

### Issue: CORS errors in browser

```bash
# Check CORS configuration in SecurityConfig
# Verify frontend URL is in CORS_ALLOWED_ORIGINS

# Temporarily allow all origins for debugging (NOT for production):
.allowedOrigins("*")

# Then restrict to specific origins:
.allowedOrigins("https://example.com", "https://www.example.com")
```

### Issue: Slow API responses

```bash
# Check database query performance
# Enable query logging:
spring.jpa.properties.hibernate.format_sql: true
logging.level.org.hibernate.SQL: DEBUG

# Check if indexes are missing
# Review slow query log
docker-compose exec mysql mysql -u root -p$MYSQL_ROOT_PASSWORD
SHOW VARIABLES LIKE 'slow_query%';
SELECT * FROM mysql.slow_log;

# Add missing indexes for frequently queried columns
ALTER TABLE courses ADD INDEX idx_status (status);
ALTER TABLE courses ADD INDEX idx_instructor_id (instructor_id);
```

---

## PRODUCTION DEPLOYMENT CHECKLIST

### Pre-Deployment

- [ ] All tests pass locally
- [ ] Code review completed
- [ ] Security scan performed (OWASP Top 10)
- [ ] Performance benchmarks verified
- [ ] Backup strategy tested
- [ ] Disaster recovery plan documented
- [ ] Monitoring and alerting configured

### Deployment

- [ ] .env configured with production values
- [ ] Database migrations run successfully
- [ ] All services started and healthy
- [ ] SSL/HTTPS certificates installed
- [ ] DNS records configured
- [ ] Load balancer configured (if applicable)
- [ ] CDN configured for static assets (optional)

### Post-Deployment

- [ ] All endpoints tested
- [ ] User registration/login tested
- [ ] Course creation/enrollment tested
- [ ] Monitoring dashboards accessible
- [ ] Alert notifications working
- [ ] Backup procedure verified
- [ ] Runbook for common issues documented

### Ongoing Maintenance

- [ ] Monitor application logs daily
- [ ] Review performance metrics weekly
- [ ] Apply security patches monthly
- [ ] Database backup verification weekly
- [ ] Capacity planning reviews quarterly

---

## PERFORMANCE OPTIMIZATION TIPS

### Database

1. **Indexing Strategy**
   ```sql
   -- Course Service
   CREATE INDEX idx_status ON courses(status);
   CREATE INDEX idx_instructor_id ON courses(instructor_id);
   CREATE INDEX idx_category ON courses(category);
   CREATE INDEX idx_title ON courses(title);
   
   -- Enrollment Service
   CREATE INDEX idx_student_id ON enrollments(student_id);
   CREATE INDEX idx_course_id ON enrollments(course_id);
   CREATE INDEX idx_status ON enrollments(status);
   ```

2. **Query Optimization**
   - Use pagination (max 100 items per page)
   - Use eager loading for related entities (@EntityGraph)
   - Avoid N+1 query problem (use JOIN FETCH)
   - Cache frequently accessed data (Redis optional)

3. **Connection Pooling**
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         minimum-idle: 5
         connection-timeout: 20000
   ```

### API

1. **Response Caching**
   - Add Cache-Control headers
   - Use ETags for client-side caching
   - Cache course listing (varies by filters)

2. **Compression**
   - Enable gzip compression for responses
   - Compress static assets (Angular)

3. **Rate Limiting**
  - Optional: enforce limits at the gateway and/or per service

### Frontend

1. **Lazy Loading**
   - Load feature modules on demand
   - Implement virtual scrolling for large lists

2. **Change Detection**
   ```typescript
   @Component({
     changeDetection: ChangeDetectionStrategy.OnPush
   })
   ```

3. **Image Optimization**
   - Use WebP format with fallback
   - Implement lazy loading for images

---

## SUCCESS METRICS

### Uptime

- Target: 99.5% availability
- Monitor: Docker health checks, AWS CloudWatch

### Response Times

- Login: < 100ms
- Course listing: < 300ms
- Course detail: < 500ms
- Enrollment: < 200ms
- Average: < 400ms

### Error Rates

- Target: < 0.5% error rate
- Monitor: Application logs, error tracking

### User Experience

- Page load time: < 2s
- Time to interactive: < 3s
- Cumulative layout shift: < 0.1

---

## FINAL NOTES

This repository provides the project architecture, service responsibilities, and deployment workflow needed to build and deploy the application.

---

## USEFUL COMMANDS QUICK REFERENCE

```bash
# Docker commands
docker-compose up -d                  # Start all services
docker-compose down                   # Stop all services
docker-compose logs -f                # Follow logs
docker-compose ps                     # Show container status
docker-compose build                  # Build all services
docker-compose restart auth-service   # Restart a specific service

# Maven commands (Java services)
mvn clean package                     # Build project
mvn spring-boot:run                   # Run locally
mvn test                             # Run tests

# Angular commands (Frontend)
ng serve                             # Development server
ng build --prod                      # Production build
ng test                             # Run tests

# MySQL commands
mysql -u root -p -h 127.0.0.1       # Connect to MySQL
SHOW DATABASES;                      # List databases
USE skilltrack;                      # Select database
SHOW TABLES;                         # List tables
DESCRIBE users;                      # Show table schema

# Curl commands (API testing)
curl -X GET http://localhost:8001/api/auth/actuator/health
curl -X POST -H "Content-Type: application/json" \
  -d '{"key": "value"}' \
  http://localhost:8001/api/endpoint
```

---

## FINAL IMPLEMENTATION NOTES

### Security Best Practices (Implemented)

Authentication:

- JWT access tokens using a configurable secret (`JWT_SECRET`, recommend 256+ bits)
- Password hashing with BCrypt (strength 12)
- Token expiration (default 24 hours via `JWT_EXPIRATION`)
- Refresh token mechanism (default 7 days via `JWT_REFRESH_EXPIRATION`, `/api/auth/refresh-token`)

Authorization:

- Role-based access control (RBAC)
- Ownership verification patterns in service layer (e.g., instructors can manage only their courses)
- Admin-only operations protected

Data Protection:

- Parameterized queries via Spring Data JPA (prevents SQL injection)
- Input validation via `@Valid` + bean validation constraints
- Standardized error responses via global exception handling
- Secrets are provided via environment variables (`.env`), not hard-coded in Dockerfiles

API / Edge Security:

- CORS configured via `CORS_ALLOWED_ORIGINS`
- HTTPS enforcement supported at the edge (nginx config redirects when `X-Forwarded-Proto` is set)

Infrastructure Security:

- Non-root users in runtime Docker images
- Network isolation via `skilltrack-network`
- Health checks configured (Actuator endpoints)
- Containers run read-only with `tmpfs` where needed

### Security Best Practices (Recommended / Optional)

- Rate limiting (e.g., gateway-level or per-service)
- Request size limits (e.g., Tomcat max post size; nginx `client_max_body_size`)
- CSRF protection (only applicable when using cookie-based sessions; JWT Bearer APIs typically keep CSRF disabled)
- Production secret management (Docker secrets / Vault / cloud secret stores)

### Performance Optimizations (Implemented)

- Pagination for list endpoints (server caps page size at 100)
- Filtering/sorting/searching performed in the database where applicable
- N+1 query prevention for course graphs using `@EntityGraph`
- Frontend optimizations: debounced search, proper RxJS unsubscription, auth interceptor refresh flow

### Performance Optimizations (Recommended)

- Add/verify DB indexes for the most common filters/sorts (status, instructorId, studentId, courseId)
- Response caching strategy (ETags / CDN) depending on traffic profile

### Scalability Considerations (Roadmap)

- Microservices enable independent scaling
- Database replication strategy for reads (if needed)
- Caching layer (Redis) for hot reads
- Message queue (RabbitMQ/Kafka) for async workflows (notifications, certificates)

---

