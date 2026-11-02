# Identify service instructions

- Follow the repository-wide English language rule in the root `AGENTS.md`.
- During implementation, follow the applicable instructions and use the skills, tools, scripts, and references in `.cursor/` and `.claude/` at the repository root. Read each skill's `SKILL.md` before using it.
- Run the complete suite from the repository root with `./identify-service/scripts/test-auth.sh`.
- The script starts and waits for `pg-test-identify-service`, then runs `./mvnw test` by default. Optional arguments are forwarded to the Maven Wrapper.
- Authentication tests use real HTTP and the `auth-test` profile with PostgreSQL and Flyway.
- The test database is `identify_service`, with username/password `postgres`, on port `5433`.
- Never use or clean the primary database on port `5432`.
- Users are explicitly cleaned up before each HTTP scenario.
- If a test fails, fix the cause and rerun the complete command.
- Keep the test container running after validation.
- Test RSA keys are temporary and shared through the test support code.
- Authentication coverage includes registration, login, `/v1/users/me`, and JWT failures.
- Preserve the other tests and their existing database configurations.
- Never adding comments in the code
