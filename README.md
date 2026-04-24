# TestForge

Spring Boot QA test data backend for creating test members, health actions, and incentive strategies. Replaces QAUtilities.

## Quick Start

### Prerequisites

- Java 21 (or jenv for automatic version management)
- Oracle database connection
- Valid JWT token (issued by Auth service)

### Build

```bash
./gradlew build
```

### Run

```bash
export ORACLE_DB_URL=your-oracle-host
export DB_USER=your-db-user
export DB_PASS=your-db-password
export DB_SCHEMA=COM
export JWT_SECRET=your-base64-encoded-secret

./gradlew bootRun
```

Server starts on port 9042 at `/testforge/api`.

---

## API Endpoints

### Health Check (Public)

```bash
curl http://localhost:9042/testforge/api/actuator/health
```

Response:
```json
{
  "status": "UP"
}
```

---

### Create Basic EIS Test Setup (Protected)

**Endpoint:** `POST /testforge/api/template/basic-eis`

Creates a test member with:
- Employer incentive strategy (EIS) linked to an existing incentive strategy
- Health action feature flags enabled
- SSO registration enabled (optionally online registration)
- Materialized views refreshed and automatic health processing triggered

**Headers:**
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body:**
```json
{
  "testSession": "PE000.0001",
  "healthActionCodes": ["HA_BP_CHECK", "HA_WEIGHT"],
  "strategyCd": "DEFAULT",
  "shouldOnlineReg": false,
  "contactEmail": null,
  "contactNumber": null,
  "mfaType": null
}
```

**Parameters:**
- `testSession` (required): Unique session identifier for test isolation (e.g., `PE000.0001`, `TEST.001`)
- `healthActionCodes` (required): List of health action codes to enable features for (e.g., `["HA_BP_CHECK", "HA_GLUCOSE_TEST"]`)
- `strategyCd` (required): Incentive strategy code to link to employer group (must exist in database)
- `shouldOnlineReg` (optional, default: false): Enable online registration for the test member
- `contactEmail` (optional): Email for online registration (required if `shouldOnlineReg` is true)
- `contactNumber` (optional): Phone number for online registration
- `mfaType` (optional): MFA type for online registration

**Example cURL:**

```bash
curl -X POST http://localhost:9042/testforge/api/template/basic-eis \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "testSession": "PE000.0001",
    "healthActionCodes": ["HA_BP_CHECK", "HA_WEIGHT"],
    "strategyCd": "DEFAULT",
    "shouldOnlineReg": false
  }'
```

**Response (201 Created):**
```json
{
  "testSession": "PE000.0001",
  "memberId": 123456,
  "employerGroupId": 789,
  "medicalPlanId": 456,
  "incentiveStrategyId": 111
}
```

**Example with online registration:**

```bash
curl -X POST http://localhost:9042/testforge/api/template/basic-eis \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "testSession": "PE000.0002",
    "healthActionCodes": ["HA_BP_CHECK"],
    "strategyCd": "DEFAULT",
    "shouldOnlineReg": true,
    "contactEmail": "testmember@example.com",
    "contactNumber": "555-0100",
    "mfaType": "EMAIL"
  }'
```

---

## Workflow: Getting a JWT Token

TestForge requires a valid JWT token from the Auth service. Obtain a token from Auth:

```bash
# 1. Login to Auth service
curl -X POST http://localhost:9041/auth/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "your-username",
    "password": "your-password"
  }'
```

Response:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer"
}
```

If MFA is required, you'll get:
```json
{
  "mfaRequired": true,
  "mfaToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

Verify MFA:
```bash
curl -X POST http://localhost:9041/auth/api/auth/verify \
  -H "Content-Type: application/json" \
  -d '{
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "mfaCode": "123456"
  }'
```

2. Use the `accessToken` in subsequent TestForge requests as shown above.

---

## Complete End-to-End Example

```bash
#!/bin/bash

# Get JWT token
AUTH_RESPONSE=$(curl -s -X POST http://localhost:9041/auth/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "testpass"
  }')

TOKEN=$(echo $AUTH_RESPONSE | jq -r '.accessToken')
echo "Token: $TOKEN"

# Create test member with basic EIS
RESPONSE=$(curl -s -X POST http://localhost:9042/testforge/api/template/basic-eis \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "testSession": "CI_TEST_001",
    "healthActionCodes": ["HA_BP_CHECK", "HA_WEIGHT", "HA_GLUCOSE_TEST"],
    "strategyCd": "DEFAULT",
    "shouldOnlineReg": false
  }')

echo "Created test member:"
echo $RESPONSE | jq '.'

MEMBER_ID=$(echo $RESPONSE | jq -r '.memberId')
echo "Member ID: $MEMBER_ID"
```

---

## Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2026-04-24T16:45:00",
  "message": "Validation failed",
  "errors": {
    "testSession": "must not be blank",
    "strategyCd": "must not be blank"
  }
}
```

### 401 Unauthorized
```json
{
  "timestamp": "2026-04-24T16:45:00",
  "message": "Invalid or expired token",
  "details": ""
}
```

### 404 Not Found
```json
{
  "timestamp": "2026-04-24T16:45:00",
  "message": "Incentive strategy not found: INVALID_STRATEGY",
  "details": "uri=/testforge/api/template/basic-eis"
}
```

### 500 Internal Server Error
```json
{
  "timestamp": "2026-04-24T16:45:00",
  "message": "No active compliance period found",
  "details": "uri=/testforge/api/template/basic-eis"
}
```

---

## What Gets Created

When you call `POST /template/basic-eis`, the following happens:

1. **Employer Group** — Test employer created via `ah_test.create_test_employer_group()`
2. **Medical Plan** — Test plan created via `ah_test.create_test_medical_plan()` with type 'UNK'
3. **Test Member** — Member with PHI created via `ah_test.create_test_case()` with:
   - Date of birth: 1985-01-01
   - Gender: Female
   - Coverage dates: Current compliance period
   - SSO enabled with member registration
4. **Employer Incentive Strategy** — Links employer group → incentive strategy → compliance period
5. **Feature Flags** — Health action features enabled for the employer group (one per `healthActionCode`)
6. **Materialized Views** — Refreshed via `ah_test.refresh_ah_mviews()`
7. **Automatic Health Processing** — Triggered via `ah_test.run_ah_for_session()` with 5-second wait for async completion

All operations are committed and test data is isolated by `testSession` parameter.

---

## Database

TestForge requires Oracle database access. Configure via environment variables:

```bash
ORACLE_DB_URL=oracle.example.com       # Oracle host
DB_USER=SVC_TESTFORGE                  # Service account user
DB_PASS=***                            # Service account password
DB_SCHEMA=COM                          # Default schema (optional, defaults to COM)
```

Connection string: `jdbc:oracle:thin:@{ORACLE_DB_URL}:1521:HMINE`

---

## Security

- **JWT Verification**: All protected endpoints require a valid Bearer token signed with the shared JWT secret
- **Stateless**: No session storage; tokens are verified on each request
- **Role-based**: Token roles are extracted and populated in `SecurityContext`
- **CORS**: Enabled for all origins (configurable in production)

---

## Development

### Project Structure

```
TestForge/
├── src/main/kotlin/com/healthmine/testforge/
│   ├── config/             # Security, JWT, logging config
│   ├── jwt/                # JWT service (verifier-only)
│   ├── template/           # Template endpoints
│   │   ├── api/            # Controllers
│   │   ├── dtos/           # Request/response models
│   │   ├── entities/       # JPA entities
│   │   └── repositories/   # Spring Data repositories
│   └── utility/            # Exceptions, logger
├── src/main/resources/
│   ├── application.yml     # Configuration
│   └── logback.xml         # Logging setup
└── build.gradle.kts        # Gradle build config
```

### Key Technologies

- **Spring Boot 4.0.6** — Latest stable
- **Kotlin 2.2.21** — Latest stable
- **Spring Security 7.0.5** — JWT validation
- **Hibernate 7.2.12** — JPA ORM
- **jjwt 0.13.0** — JWT parsing

### Building Locally

```bash
# Compile
./gradlew compileKotlin

# Run tests
./gradlew test

# Build JAR
./gradlew bootJar

# Clean
./gradlew clean
```

---

## Future Templates

Additional template endpoints planned:
- `POST /template/incentive-event-eis` — Employer-level incentive event testing
- `POST /template/incentive-event-cis` — Cohort-level incentive event testing
- `POST /template/health-action-simple` — Health action recommendation scenarios
- `POST /template/member-activity` — Device/biometric data population
- `POST /template/comprehensive` — Full attestation, waiver, lab procedure setup

---

## Support

For issues, questions, or feature requests, please open an issue on GitHub: https://github.com/healthmine/TestForge/issues
