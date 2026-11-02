# Identify service

Requires Java 25 and PostgreSQL (the local configuration uses the `identify_service` database).

## JWT keys

Provide a matching RSA key pair. For development, generate the keys in a directory outside the repository:

```sh
mkdir -p /tmp/identify-service-keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out /tmp/identify-service-keys/private.pem
openssl pkey -in /tmp/identify-service-keys/private.pem -pubout -out /tmp/identify-service-keys/public.pem
chmod 600 /tmp/identify-service-keys/private.pem
export JWT_PRIVATE_KEY=file:/tmp/identify-service-keys/private.pem
export JWT_PUBLIC_KEY=file:/tmp/identify-service-keys/public.pem
./mvnw spring-boot:run
```

Do not commit the private key. Set `JWT_PRIVATE_KEY` and `JWT_PUBLIC_KEY` to `file:` or `classpath:` locations. The defaults are `classpath:keys/private.pem` and `classpath:keys/public.pem`; the private key is not distributed. `JWT_EXPIRATION_SECONDS` controls token validity (default: 3600 seconds; must be positive).

## Authentication

Authentication routes use the `/v1/auth` prefix.

`POST /v1/auth/register` accepts:

```json
{"cpf":"12345678901","fullName":"Lucas","email":"lucas@example.com","password":"password123"}
```

Returns HTTP 201. CPF must contain 11 digits, and passwords must contain at least 8 characters. Accounts are created as `CUSTOMER` and `ACTIVE`; duplicate CPF or email values return 409.

`POST /v1/auth/login` accepts:

```json
{"cpf":"12345678901","password":"password123"}
```

Returns HTTP 200. Both routes respond with:

```json
{"accessToken":"<JWT>","tokenType":"Bearer","expiresIn":3600}
```

Send `Authorization: Bearer <JWT>` to the other routes, such as `GET /v1/users/me`. Invalid credentials or a blocked account return 401 with a generic message; invalid input returns 400. Blocking an account prevents new logins; previously issued tokens remain valid until they expire. This version does not support refresh tokens or logout.

Swagger: `/swagger-ui/index.html`.

## Testing

### GitHub Actions CI

The `.github/actions/ci/action.yml` action inside `identify-service` orchestrates
the following steps in order:

1. `validate-pr`: immediately fails if the PR targets `main`.
2. `check-coverage`: runs all Java tests, including authentication with PostgreSQL,
   generates the JaCoCo report, and requires overall line coverage of production Java
   code to be **greater than 70%**. Exactly 70% fails. No tests or production classes
   are excluded. A missing, invalid, or empty report also fails. The percentage
   appears in the CI summary.
3. `run-tests`: after coverage passes, runs all microservice tests, including
   authentication with real HTTP, PostgreSQL, and Flyway.

The coverage and test steps use Java 25 and only run when changes affect the service,
Compose configuration, or workflow. A failure stops subsequent steps.

The `../.github/workflows/identify-service-ci.yml` workflow triggers on PRs from
`feat/identify-service` and calls this local action. GitHub requires workflows to
reside in `.github/workflows` at the repository root. Each microservice will have
its own entry workflow there and its implementation inside the service.
To prevent merges into `main`, configure `Identify service CI` as a required check
in the branch protection settings.

### Local execution

To measure and verify Java code coverage using the complete suite, run from the
repository root:

```sh
./identify-service/scripts/test-auth.sh --batch-mode --no-transfer-progress -Pcoverage clean test jacoco:report
./identify-service/scripts/check-coverage.sh
```

The shell checker uses `xmllint` to read the JaCoCo XML report and `awk` to format
the percentage. On Ubuntu, install `libxml2-utils` if `xmllint` is unavailable;
the CI action installs it automatically when needed.

The HTML report is available at `identify-service/target/site/jacoco/index.html`
relative to the repository root. Measuring coverage requires executing tests to
identify which lines were exercised; the complete suite then runs again as a
separate CI step.

Run the complete suite from the repository root:

```sh
./identify-service/scripts/test-auth.sh
```

The script waits for the `pg-test-identify-service` container to become healthy on port `5433`, then runs `./mvnw test`. Authentication tests use real HTTP, Flyway, and a temporary RSA key pair. They clean up users in the test database before each scenario. Never point the tests at the primary instance on port `5432`; the test container remains running afterward.
