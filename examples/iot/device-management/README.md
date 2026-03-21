# IoT Device Lifecycle Management in Java with Conductor: Registration, Provisioning, and Health Monitoring

You have 10,000 temperature sensors deployed across 200 facilities, all running firmware v2.1. A critical security vulnerability is discovered in v2.1's MQTT authentication. an attacker on the same network can spoof telemetry data. The patch is in v2.3, and it needs to roll out now. But 1,400 of those sensors are on flaky cellular connections in rural warehouses. Push the update too aggressively and you brick sensors that lose connectivity mid-flash. Skip them and you leave 1,400 attack vectors in the field. You need an update pipeline that tracks each device's state, retries on connection drops, and never leaves a sensor half-updated. This example uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the full device lifecycle, registration, provisioning, configuration, health monitoring, and firmware updates, as independent workers.

## Why Device Onboarding Needs Orchestration

Bringing a new IoT device online requires a strict sequence of steps that each depend on the previous one. You register the device in your fleet registry to get a registration ID. You use that ID to provision TLS certificates and an MQTT endpoint. You use those credentials to push telemetry configuration. Reporting intervals, topic subscriptions, device shadow creation. Only then can you start health monitoring (battery level, signal strength, uptime) and check whether the device needs a firmware update.

If provisioning fails halfway through, you need to know exactly which step succeeded so you can resume without re-registering or issuing duplicate certificates. If health monitoring reveals a degraded device, you need that signal to flow into the firmware update step. Without orchestration, you'd build this as a monolithic onboarding script. Manually threading credentials between steps, wrapping every call in retry logic, and writing state-tracking code so you can recover from partial failures. That script becomes the single point of failure for your entire fleet onboarding process.

## How This Workflow Solves It

**You just write the device lifecycle workers. Registration, credential provisioning, telemetry configuration, health monitoring, and firmware updates. Conductor handles strict onboarding sequencing, MQTT broker retries, and durable state tracking so partial failures resume exactly where they stopped.**


### What You Write: Workers

Five workers manage the device lifecycle: RegisterDeviceWorker adds the device to the fleet registry, ProvisionWorker issues TLS credentials, ConfigureWorker sets telemetry parameters, MonitorHealthWorker checks battery and signal strength, and PushUpdateWorker delivers firmware patches.

| Worker | Task | What It Does |
|---|---|---|
| **ConfigureWorker** | `dev_configure` | Configures device settings including reporting interval and telemetry topics. |
| **MonitorHealthWorker** | `dev_monitor_health` | Monitors device health status. |
| **ProvisionWorker** | `dev_provision` | Provisions credentials and connectivity for a device. |
| **PushUpdateWorker** | `dev_push_update` | Checks for and pushes firmware updates to a device. |
| **RegisterDeviceWorker** | `dev_register_device` | Registers a new IoT device in the device registry. |

### The Workflow

```
Input -> ConfigureWorker -> MonitorHealthWorker -> ProvisionWorker -> PushUpdateWorker -> RegisterDeviceWorker -> Output

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
