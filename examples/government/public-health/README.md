# Public Health Surveillance in Java with Conductor :  Disease Monitoring, Outbreak Detection, and Response Coordination

A Java Conductor workflow example for public health surveillance. monitoring disease case counts by region, detecting outbreaks against baseline thresholds, routing to alert or continued monitoring, and coordinating the public health response. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to run disease surveillance for a public health department. Case reports come in for a specific disease and region. The system must pull baseline epidemiological data, compare the current case count against expected levels to detect whether an outbreak is occurring, and then take the right action. issue a public health alert if cases exceed the threshold, or schedule continued monitoring if levels are elevated but not yet critical. Regardless of the branch taken, a response plan must be executed. The decision to alert versus monitor must be automatic and auditable.

Without orchestration, you'd build a monolithic surveillance application that queries the case database, runs the outbreak detection algorithm, branches with if/else into alert or monitoring logic, and then triggers the response. If the surveillance data feed is temporarily unavailable, you'd need retry logic. If the system crashes after detecting an outbreak but before issuing the alert, cases could go unreported. Epidemiologists need a complete timeline of every surveillance run for retrospective analysis.

## The Solution

**You just write the disease monitoring, outbreak detection, alert routing, and public health response coordination logic. Conductor handles surveillance retries, intervention routing, and public health audit trails.**

Each stage of the surveillance pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of running surveillance before detection, routing to alert or monitoring via SWITCH based on the detection outcome, always executing the response step regardless of which branch was taken, and maintaining a full audit trail of every surveillance cycle. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Surveillance data collection, outbreak analysis, intervention planning, and public notification workers each address one public health response function.

| Worker | Task | What It Does |
|---|---|---|
| **SurveillanceWorker** | `phw_surveillance` | Pulls baseline epidemiological data for a given disease and region |
| **DetectOutbreakWorker** | `phw_detect_outbreak` | Compares current case count against baseline to determine if an outbreak is occurring; returns "alert" or "monitor" |
| **AlertWorker** | `phw_alert` | Issues a public health alert for the affected region and disease with severity level |
| **MonitorWorker** | `phw_monitor` | Schedules continued surveillance with a next-check date when cases are elevated but below alert threshold |
| **RespondWorker** | `phw_respond` | Executes the public health response plan (contact tracing, resource deployment, public communications) |

Workers implement government operations. application processing, compliance checks, notifications,  with realistic outputs. Replace with real agency system integrations and the workflow stays the same.

### The Workflow

```
phw_surveillance
    │
    ▼
phw_detect_outbreak
    │
    ▼
SWITCH (phw_switch_ref)
    ├── alert: phw_alert
    ├── monitor: phw_monitor
    │
    ▼
phw_respond

```

## Running It

### Prerequisites

- **Java 21+**: verify with `java -version`
- **Maven 3.8+**: verify with `mvn -version`
- **Docker**: to run Conductor

### Option 1: Docker Compose (everything included)

```bash
docker compose up --build

```

Starts Conductor on port 8080 and runs the example automatically.

If port 8080 is already taken:

```bash
CONDUCTOR_PORT=9090 docker compose up --build

```

### Option 2: Run locally

```bash
# Start Conductor
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:1.2.3

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/public-health-1.0.0.jar

```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh

```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/public-health-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow phw_public_health \
  --version 1 \
  --input '{"region": "us-east-1", "disease": "sample-disease", "caseCount": 10}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w phw_public_health -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real public health systems. your disease surveillance platform for case monitoring, your epidemiological tools for outbreak detection, your health department coordination system for response, and the workflow runs identically in production.

- **SurveillanceWorker** → connect to your syndromic surveillance system, CDC BioSense, or state-level NEDSS to pull real case data
- **DetectOutbreakWorker** → implement a statistical detection algorithm (EARS C-algorithm, CUSUM, or Farrington) instead of a simple threshold
- **AlertWorker** → push alerts through the CDC Health Alert Network (HAN), Everbridge, or your jurisdiction's mass notification system
- **RespondWorker** → trigger contact tracing workflows, deploy mobile testing units, or activate emergency operations center (EOC) protocols
- Add a **ReportCDCWorker** to file automated MMWR reports or NNDSS notifications to federal agencies

Change surveillance data sources or intervention strategies and the public health pipeline handles them transparently.

## SDK

Uses [conductor-oss Java SDK v5](https://github.com/conductor-oss/java-sdk):

```xml
<dependency>
    <groupId>org.conductoross</groupId>
    <artifactId>conductor-client</artifactId>
    <version>5.0.1</version>
</dependency>

```

## Project Structure

```
public-health-public-health/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/publichealth/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── PublicHealthExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AlertWorker.java
│       ├── DetectOutbreakWorker.java
│       ├── MonitorWorker.java
│       ├── RespondWorker.java
│       └── SurveillanceWorker.java
└── src/test/java/publichealth/workers/
    ├── AlertWorkerTest.java
    └── DetectOutbreakWorkerTest.java

```
