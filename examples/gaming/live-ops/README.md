# Live Ops in Java Using Conductor

Runs a time-limited live ops event in a game: scheduling the event, configuring rewards and difficulty, deploying to servers across regions, monitoring engagement, and closing with reward distribution. ## The Problem

You need to run a live operations event in your game. a time-limited in-game event with special content, challenges, and rewards. The workflow schedules the event for a start/end date, configures the event parameters (rewards, difficulty, matchmaking rules), deploys the configuration to game servers, monitors player engagement and server health during the event, and closes the event when it ends. Deploying without proper configuration breaks the player experience; not monitoring means missing critical issues during the event.

Without orchestration, you'd manage live ops events through a combination of admin tools, manual server config pushes, monitoring dashboards, and calendar reminders. risking missed deployment times, misconfigured events, and undetected server issues during peak player activity.

## The Solution

**You just write the event scheduling, reward configuration, server deployment, engagement monitoring, and reward distribution logic. Conductor handles deployment retries, event scheduling, and live ops campaign tracking.**

Each live-ops concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (schedule, configure, deploy, monitor, close), retrying if a server deployment fails, tracking every live event's lifecycle, and resuming from the last step if the process crashes. ### What You Write: Workers

Event scheduling, content deployment, player targeting, and metrics collection workers enable live operations through independent, swappable stages.

| Worker | Task | What It Does |
|---|---|---|
| **CloseWorker** | `lop_close` | Closes the event, distributes rewards to participants, and returns final stats |
| **ConfigureWorker** | `lop_configure` | Configures event parameters: rewards (Double XP, Rare Skin), difficulty, and duration |
| **DeployWorker** | `lop_deploy` | Deploys the event configuration to game servers across all regions (NA, EU, APAC) |
| **MonitorWorker** | `lop_monitor` | Monitors the live event tracking participant count, engagement level, and server issues |
| **ScheduleEventWorker** | `lop_schedule_event` | Schedules the event with name and start/end dates, and assigns an event ID |

### The Workflow

```
lop_schedule_event
 │
 ▼
lop_configure
 │
 ▼
lop_deploy
 │
 ▼
lop_monitor
 │
 ▼
lop_close

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
