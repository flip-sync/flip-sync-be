# Phase 1 DB Migration Decision

## Decision

FlipSync backend should adopt Flyway for production schema management.

Do not enable Flyway in the current deployment until the existing production schema is exported and reviewed as a baseline migration.

## Why

- Production uses `spring.jpa.hibernate.ddl-auto=none`, so entity changes are not automatically reflected in the server DB.
- The project already had incidents where backend code and DB schema drifted.
- App version policy, room lifecycle, score image, and account deletion features all depend on schema consistency.
- Enabling Flyway without a baseline against the current non-empty production schema can break deployment.

## Rollout Plan

1. Export current production schema only.

```bash
mysqldump --no-data --single-transaction -h <host> -P <port> -u <user> -p flip_sync > V1__baseline_schema.sql
```

2. Review the baseline SQL.

Required checks:

- No production data is included.
- Secrets, credentials, and host-specific options are removed.
- Tables and indexes match current JPA entities.
- Existing manually-created columns such as version policy fields are represented.

3. Add Flyway dependency and baseline migration.

Recommended dependency:

```kotlin
implementation("org.flywaydb:flyway-core")
implementation("org.flywaydb:flyway-mysql")
```

Recommended location:

```text
flip-sync-server/src/main/resources/db/migration/V1__baseline_schema.sql
```

4. Enable Flyway safely in `prod`.

Recommended first-run options:

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    baseline-version: 1
    locations: classpath:db/migration
```

5. After baseline is applied, every schema change must be a new migration.

Examples:

- `V2__create_app_version_policies.sql`
- `V3__add_group_room_password.sql`
- `V4__add_user_profile_image_url.sql`

## Current Phase 1 Action

Flyway adoption is approved, but implementation is deferred until the production baseline SQL is captured.

This avoids making the current GitHub Actions deployment fail on a non-empty database with no Flyway schema history.

