# Smart Home Automation in Java with Conductor : Event Detection, Rule Evaluation, and Device Actuation

## Why Smart Home Automation Needs Orchestration

A smart home automation rule involves a decision chain: a sensor fires an event, the system evaluates context (is someone home? what mode is active? what is the current temperature?), and based on the matched rule, it actuates the right device. dim the lights to 80% warm white, set the thermostat to a target temperature, or arm the security system for a specific zone. Different event types route to entirely different actuators, and every actuation must be logged for audit and debugging.

This is a natural fit for conditional routing. A motion sensor at the front door should turn on lights when the home is in "home" mode but arm the security system when in "away" mode. Without orchestration, you'd build a monolithic automation engine with a growing if/else tree that handles every sensor-to-actuator mapping, mixing event detection, rule matching, device control, and logging in one tangled class. Adding a new device type (blinds, sprinklers, garage door) means modifying the core engine. Conductor's SWITCH task handles the routing declaratively. add a new case and a new worker, and the existing automation rules are untouched.

## How This Workflow Solves It

**You just write the smart home workers. Event detection, rule evaluation, and device-specific actuators for lights, thermostat, and security. Conductor handles SWITCH-based device routing, device unreachability retries, and logged records for every automation action.**

Each automation concern is an independent worker. detect events, evaluate rules, actuate lights/thermostat/security, log the event. Conductor sequences detection and rule evaluation, then uses a SWITCH task to route to the correct actuator based on the matched rule. If a device is temporarily unreachable, Conductor retries the actuation. Adding a new device type means adding a new SWITCH case and a new worker, no changes to existing code.

### What You Write: Workers

Six workers handle home automation: DetectEventWorker captures sensor events, EvaluateRulesWorker matches automation rules, ActuateLightsWorker controls brightness, ActuateThermostatWorker sets temperature, ActuateSecurityWorker arms zones, and LogEventWorker records every action.

| Worker | Task | What It Does |
|---|---|---|
| **ActuateLightsWorker** | `smh_actuate_lights` | Controls lights. sets brightness, color temperature, and scene for the target room. |
| **ActuateSecurityWorker** | `smh_actuate_security` | Arms or disarms the security system for a specific zone based on the automation rule. |
| **ActuateThermostatWorker** | `smh_actuate_thermostat` | Sets the thermostat to a target temperature and HVAC mode. |
| **DetectEventWorker** | `smh_detect_event` | Detects sensor events (motion, temperature change, occupancy) and enriches with home context. |
| **EvaluateRulesWorker** | `smh_evaluate_rules` | Matches the event against automation rules to determine the action type, target room, and parameters. |
| **LogEventWorker** | `smh_log_event` | Logs the automation event with action type, parameters, and outcome for audit and debugging. |

the workflow and alerting logic stay the same.

### The Workflow

```
smh_detect_event
 │
 ▼
smh_evaluate_rules
 │
 ▼
SWITCH (smh_switch_ref)
 ├── lights: smh_actuate_lights
 ├── thermostat: smh_actuate_thermostat
 ├── security: smh_actuate_security
 │
 ▼
smh_log_event

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
