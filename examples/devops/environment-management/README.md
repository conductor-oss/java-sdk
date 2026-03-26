# Provisioning a Test Environment with Seed Data

Developers need isolated environments for testing, but creating one by hand -- spinning up
the infrastructure, applying 23 config settings, seeding 1000 test records, and verifying
health -- takes half a day and is different every time. This workflow creates, configures,
seeds, and verifies an environment from a template.

## Workflow

```
envName, template, ttlHours
          |
          v
+------------------+     +----------------+     +----------------+     +--------------+
| em_create_env    | --> | em_configure   | --> | em_seed_data   | --> | em_verify    |
+------------------+     +----------------+     +----------------+     +--------------+
  ENV-300001               23 settings           1000 test records     healthy=true
  created=true             applied               seeded=true
```

## Workers

**CreateEnv** -- Takes `envName` and `template` inputs. Creates the environment and returns
`envId: "ENV-300001"`, `created: true`.

**ConfigureEnv** -- Applies 23 configuration settings to the new environment. Returns
`configured: true`.

**SeedData** -- Seeds 1000 test records into the environment. Returns `seeded: true`,
`records: 1000`.

**VerifyEnv** -- Checks that the environment is healthy and ready to use. Returns
`healthy: true`.

## Tests

21 unit tests cover environment creation, configuration, data seeding, and health
verification.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
