# Emergency Response in Java with Conductor : Incident Detection, Severity Classification, Dispatch, and Debrief

## The Problem

You need to manage the lifecycle of an emergency incident from the moment it is reported through post-incident review. A report comes in with an incident type and location. The system must register the incident, classify its severity (fire, medical, hazmat, etc.), dispatch the appropriate response units based on severity and proximity, coordinate multi-agency action on scene, and produce a debrief record when the incident is resolved. Each step feeds the next. you cannot dispatch without a severity classification, and you cannot debrief without knowing which units responded and what the outcome was.

Without orchestration, you'd build a monolithic dispatch system that calls each service in sequence, handles retries when the CAD (computer-aided dispatch) system is slow, and tries to recover gracefully if the system crashes mid-incident. In an emergency, a missed step or lost state can delay response times and cost lives. After-action reviews require a precise timeline of every action taken.

## The Solution

**You just write the incident detection, severity classification, unit dispatch, response coordination, and post-incident debrief logic. Conductor handles dispatch retries, coordination sequencing, and incident response audit trails.**

Each stage of the emergency response is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of running them in sequence, passing the incident ID and severity classification downstream to dispatch and coordination, retrying if the dispatch system is temporarily unavailable, and maintaining a complete timeline of every action for the post-incident debrief. ### What You Write: Workers

Incident detection, resource dispatch, field coordination, and situation reporting workers each handle one phase of the emergency response chain.

| Worker | Task | What It Does |
|---|---|---|
| **DetectWorker** | `emr_detect` | Registers the incoming incident report, assigns an incident ID, and records the location and type |
| **ClassifySeverityWorker** | `emr_classify_severity` | Determines incident severity (low/moderate/high/critical) based on incident type and reported details |
| **DispatchWorker** | `emr_dispatch` | Selects and dispatches the appropriate response units (fire, EMS, police) based on severity and location |
| **CoordinateWorker** | `emr_coordinate` | Manages multi-unit coordination on scene and tracks the incident outcome |
| **DebriefWorker** | `emr_debrief` | Produces the post-incident report with timeline, outcome, and lessons learned |

### The Workflow

```
emr_detect
 │
 ▼
emr_classify_severity
 │
 ▼
emr_dispatch
 │
 ▼
emr_coordinate
 │
 ▼
emr_debrief

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
