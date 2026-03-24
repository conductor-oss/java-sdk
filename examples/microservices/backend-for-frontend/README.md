# Backend-for-Frontend with Platform-Specific Response Shaping

Mobile and web clients need different response shapes from the same data. Mobile needs a
compact summary; web needs the full payload. This workflow fetches user data (profile,
orders, notifications), then uses a SWITCH on the `platform` input to route through either
a mobile or web transformer.

## Workflow

```
userId, platform
       |
       v
+------------------+
| bff_fetch_data   |   profile: {name: "Alice"}, orders: 5, notifications: 3
+------------------+
       |
       v
  SWITCH on platform
  +-----------------+------------------+
  | "mobile"        | default ("web")  |
  | bff_transform_  | bff_transform_   |
  | mobile          | web              |
  | format: mobile  | format: web      |
  | fields: compact | fields: full     |
  | summary: true   | data: full obj   |
  +-----------------+------------------+
```

## Workers

**FetchDataWorker** -- Loads data for the given `userId`. Returns `profile: {name: "Alice"}`,
`orders: 5`, `notifications: 3`.

**TransformMobileWorker** -- Produces a compact response for small screens. Returns
`format: "mobile"`, `fields: "compact"`, `summary: true`.

**TransformWebWorker** -- Produces a full desktop response with all fields. Returns
`format: "web"`, `fields: "full"`, plus the full data object.

## Tests

6 unit tests cover data fetching, mobile transformation, and web transformation.

## Running

See [../../RUNNING.md](../../RUNNING.md) for setup and execution instructions.
