# Device Management

IoT device lifecycle management: register, provision, configure, monitor health, push updates

**Input:** `deviceId`, `deviceType`, `fleetId`, `firmwareVersion` | **Timeout:** 1800s

## Pipeline

```
dev_register_device
    │
dev_provision
    │
dev_configure
    │
dev_monitor_health
    │
dev_push_update
```

## Workers

**ConfigureWorker** (`dev_configure`): Configures device settings. Real configuration based on device type.

```java
switch (deviceType.toLowerCase()) {
```

Reads `deviceType`. Outputs `configured`, `reportingInterval`, `telemetryTopics`, `shadowCreated`.

**MonitorHealthWorker** (`dev_monitor_health`): Monitors device health. Real metric evaluation with threshold checks.

```java
int batteryLevel = 50 + RNG.nextInt(51);
```

Reads `deviceId`. Outputs `healthStatus`, `batteryLevel`, `signalStrength`, `lastSeen`, `uptime`.

**ProvisionWorker** (`dev_provision`): Provisions credentials for a device. Real certificate ID generation.

```java
String certificateId = "CERT-" + HexFormat.of().formatHex(certBytes).substring(0, 12).toUpperCase();
```

Reads `deviceId`. Outputs `certificateId`, `mqttEndpoint`, `thingName`, `provisionedAt`.

**PushUpdateWorker** (`dev_push_update`): Checks and pushes firmware updates. Real version comparison.

- `comparison >= 0` &rarr; `"up_to_date"`

```java
int len = Math.max(parts1.length, parts2.length);
```

Reads `currentFirmware`, `deviceId`. Outputs `updateStatus`, `currentVersion`, `latestVersion`, `nextCheckAt`.

**RegisterDeviceWorker** (`dev_register_device`): Registers an IoT device. Real validation of device type and fleet assignment.

```java
boolean validType = VALID_TYPES.contains(deviceType.toLowerCase());
```

Reads `deviceId`, `deviceType`, `fleetId`. Outputs `registrationId`, `registeredAt`, `fleetId`, `validType`.
Returns `FAILED` on validation errors.

## Tests

**10 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
