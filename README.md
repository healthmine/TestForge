# TestForge

Spring Boot QA test data backend for creating test members, health actions, and incentive strategies. Replaces QAUtilities.

## Quick Start

### Prerequisites

- Java 21 (`jenv local 21` if you use jenv)
- Python 3 with `pyjwt` for the local token generator
- Oracle database access
- Access to the repo's GitHub Actions secrets (for `TEST_MEMBER_PASSWORD`)

### Build

```bash
./gradlew bootJar
```

Produces `build/libs/TestForge.jar`.

### Run

```bash
export JWT_SECRET=$(openssl rand -base64 32)        # or a value shared with Auth
export TEST_MEMBER_PASSWORD='Orbita11!'             # mirrors the GH secret
export ORACLE_DB_URL=oracle.qa.healthmineops.com
export DB_USER=...
export DB_PASS=...
export DB_SCHEMA=DEMOMA1                            # defaults to COM if unset

java -jar build/libs/TestForge.jar
```

Server starts on port 9042 at `/testforge/api`.

---

## API Endpoints

### Health Check (Public)

```bash
curl http://localhost:9042/testforge/api/actuator/health
```

```json
{ "status": "UP" }
```

### Create Basic EIS Test Setup (Protected)

`POST /testforge/api/template/basic-eis`

Creates a fully provisioned test member: employer group, medical plan, EIS row, feature flags for each health action, SSO-enabled username, a hashed password, then refreshes materialized views and runs automatic-health for the session.

**Headers**
```
Authorization: Bearer <JWT>
Content-Type: application/json
```

**Request body**
```json
{
  "testSession": "FORGE_TEST_001",
  "healthActionCodes": ["HA_BP_CHECK", "HA_WEIGHT"],
  "strategyCd": "DEFAULT",
  "shouldOnlineReg": false,
  "contactEmail": null,
  "contactNumber": null,
  "mfaType": null
}
```

| Field | Required | Notes |
|---|---|---|
| `testSession` | yes | Unique per run; used as the member's last name for isolation. |
| `healthActionCodes` | yes | Non-empty list; one `healthaction:<code>` feature flag is upserted per entry. |
| `strategyCd` | yes | Must match an existing row in `incentive_strategy`. |
| `shouldOnlineReg` | no | Triggers `ah_test.online_web_reg` when true. |
| `contactEmail` / `contactNumber` / `mfaType` | no | Forwarded to `online_web_reg` when `shouldOnlineReg` is true. |

**Response (200 OK)**
```json
{
  "testSession": "FORGE_TEST_001",
  "memberId": 5047005,
  "employerGroupId": 1550,
  "medicalPlanId": 1916,
  "incentiveStrategyId": 3,
  "username": "TEST.M5047005@test.healthmineops.com",
  "password": "Orbita11!"
}
```

The `username` is read from `member_phi.username` after `ah_test.enable_sso_for_session` populates it (format varies per schema; in DEMOMA1 it's `TEST.M<memberId>@test.healthmineops.com`). The `password` is the plaintext the service just hashed into the DB — use it directly to log in via Auth.

---

## Getting a JWT

### Local development (recommended)

Use the included Python generator; it signs with the base64-decoded secret the same way jjwt does:

```bash
JWT_SECRET=$JWT_SECRET python3 scripts/generate_jwt.py
```

Optional overrides: `JWT_SUB`, `JWT_ROLES` (JSON array), `JWT_TTL` (seconds).

### From Auth

If Auth is running with the same `JWT_SECRET`, log in and use the returned `accessToken`:

```bash
curl -X POST http://localhost:9041/auth/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "...", "password": "..."}'
```

---

## End-to-end example

```bash
TOKEN=$(JWT_SECRET=$JWT_SECRET python3 scripts/generate_jwt.py)

curl -s -X POST http://localhost:9042/testforge/api/template/basic-eis \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "testSession": "CI_TEST_001",
    "healthActionCodes": ["HA_BP_CHECK", "HA_WEIGHT"],
    "strategyCd": "DEFAULT"
  }' | jq .
```

Then log into Auth with the returned `username` / `password` to drive the rest of the e2e flow.

---

## Error responses

| Status | When |
|---|---|
| 400 | Missing / blank required field in the request body. |
| 401 | Missing or invalid `Authorization` header, or expired/mismatched JWT. |
| 500 | Upstream Oracle error, missing compliance period, or unknown `strategyCd` (returned as a `ResponseStatusException` passthrough). |

Body shape:
```json
{
  "timestamp": "2026-04-24T12:00:00",
  "message": "...",
  "details": "uri=/testforge/api/template/basic-eis"
}
```

---

## What the endpoint does

Inside one transaction:

1. `ah_test.create_test_employer_group` → new `employer_group_id`.
2. `ah_test.create_test_medical_plan` (type `UNK`) → new `medical_plan_id`.
3. `ah_test.create_test_case` → new member with DOB 1985-01-01, gender F, coverage starting on the current compliance period.
4. Upsert into `employer_incentive_strategy` (`employer_group_id` × `incentive_strategy_id` × `compliance_period_id`).
5. Upsert `feature_employer_group_xref` for every `healthaction:<code>`.
6. `ah_test.enable_sso_for_session` → populates `member_phi.username`, activates the member, assigns `MemberPortal.User`.
7. Optional `ah_test.online_web_reg` when `shouldOnlineReg` is true.
8. Hash `TEST_MEMBER_PASSWORD` (PBKDF2WithHmacSHA1, 901 iterations, 32-byte output) and write `member_phi.password` + `member_password_salt.pwd_salt`.

Then, outside the transaction:

9. `ah_test.refresh_ah_mviews`.
10. `ah_test.run_ah_for_session`.
11. 5-second sleep so asynchronous processing lands before the caller reads.

---

## Configuration

All configuration is environment-driven. Nothing sensitive is committed.

| Variable | Purpose |
|---|---|
| `JWT_SECRET` | Base64-encoded HMAC-SHA256 key; must match Auth. |
| `TEST_MEMBER_PASSWORD` | Plaintext password hashed into every created member. Stored as a GitHub secret on this repo. |
| `ORACLE_DB_URL` | Oracle host, e.g. `oracle.qa.healthmineops.com`. |
| `DB_USER` / `DB_PASS` | Session credentials. Must have write access to `member_phi` and `member_password_salt` in the target schema. |
| `DB_SCHEMA` | Oracle schema for both entity tables and unqualified PL/SQL (`ah_test.*`). Applied via Hikari `connection-init-sql` → `ALTER SESSION SET CURRENT_SCHEMA`. Defaults to `COM`. |

Connection string: `jdbc:oracle:thin:@${ORACLE_DB_URL}:1521:HMINE`.

---

## Security

- JWT verifier-only. Tokens are issued by Auth (or the local generator); TestForge only validates signature + expiration and pulls `sub` / `roles` into the `SecurityContext`.
- Stateless — no server-side sessions.
- Public routes: `/actuator/health`. Everything else requires a valid Bearer token.

---

## Project layout

```
src/main/kotlin/com/healthmine/testforge/
├── TestForgeApplication.kt
├── config/          Security, JWT filter, Jackson-friendly exception handling, request logging
├── jwt/             JwtService (verifier-only)
├── template/
│   ├── api/         TemplateController
│   ├── dtos/        BasicEisRequest / BasicEisResponse
│   ├── entities/    JPA entities (Client, IncentiveStrategy, CompliancePeriod, EIS, FeatureXref)
│   ├── repositories Spring Data repos
│   └── BasicEisService.kt   Orchestration + password hashing + DB writes
└── utility/         logger + PasswordHasher
scripts/
└── generate_jwt.py  Local JWT generator (base64-decodes the secret so jjwt accepts it)
```

### Stack

- Spring Boot 4.0.6, Kotlin 2.2.21, Java 21
- Spring Security, Hibernate 7.2.12, jjwt 0.13.0
- Oracle JDBC (ojdbc11)

### Gradle

```bash
./gradlew bootJar    # fat jar under build/libs/
./gradlew test       # unit tests
./gradlew clean
```

---

## Future templates

Planned:
- `POST /template/incentive-event-eis`
- `POST /template/incentive-event-cis`
- `POST /template/health-action-simple`
- `POST /template/member-activity`
- `POST /template/comprehensive`

---

## Support

Issues, questions, or requests: https://github.com/healthmine/TestForge/issues
