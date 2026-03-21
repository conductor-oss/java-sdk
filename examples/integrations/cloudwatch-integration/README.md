# Cloudwatch Integration in Java Using Conductor

A Java Conductor workflow that sets up CloudWatch monitoring. publishing a custom metric, creating an alarm with a threshold, checking the alarm status against the current value, and sending a notification if the alarm triggers. Given a namespace, metric name, value, threshold, and notification email, the pipeline produces a published metric, alarm ARN, alarm state, and notification status. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate the metric-alarm-check-notify pipeline.

## Setting Up Metric Monitoring with Alerting

CloudWatch monitoring involves a chain of dependent steps: publish a metric data point, create an alarm that watches that metric against a threshold, evaluate whether the current value triggers the alarm, and send a notification if it does. Each step depends on the previous one. you cannot create an alarm without a metric namespace, and you cannot notify without knowing the alarm state.

Without orchestration, you would chain CloudWatch API calls manually, manage alarm ARNs between steps, and build custom notification logic. Conductor sequences the four steps and routes metric names, alarm ARNs, and alarm states between them automatically.

## The Solution

**You just write the CloudWatch workers. Metric publishing, alarm creation, status checking, and threshold-based notification. Conductor handles metric-to-notification sequencing, CloudWatch API retries, and alarm state routing between workers.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers build the monitoring pipeline: PutMetricWorker publishes data points, CreateAlarmWorker sets thresholds, CheckStatusWorker evaluates alarm state, and CwNotifyWorker dispatches alerts when thresholds breach.

| Worker | Task | What It Does |
|---|---|---|
| **PutMetricWorker** | `cw_put_metric` | Publishes a metric data point to CloudWatch. writes the value to the specified namespace and metric name, returns published=true |
| **CreateAlarmWorker** | `cw_create_alarm` | Creates a CloudWatch alarm. sets up monitoring for the metric against the specified threshold and returns the alarmName and alarm ARN |
| **CheckStatusWorker** | `cw_check_status` | Checks the alarm status. evaluates the current metric value against the alarm threshold and returns the alarm state (OK or ALARM) |
| **CwNotifyWorker** | `cw_notify` | Sends an alarm notification. dispatches an alert email to the notifyEmail address with the alarm name and state (triggered only when the alarm is in ALARM state) |

Workers implement external API calls with realistic response shapes so you can see the integration flow end-to-end. Replace with real API clients. the workflow orchestration and error handling stay the same.

### The Workflow

```
cw_put_metric
    │
    ▼
cw_create_alarm
    │
    ▼
cw_check_status
    │
    ▼
cw_notify

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
java -jar target/cloudwatch-integration-1.0.0.jar

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
| `AWS_ACCESS_KEY_ID` | _(none)_ | AWS access key ID. Currently unused, all workers run in simulated mode with `[SIMULATED]` output prefix. Swap in AWS SDK for production. |
| `AWS_SECRET_ACCESS_KEY` | _(none)_ | AWS secret access key. Required alongside `AWS_ACCESS_KEY_ID` for production use. |

## Using the Conductor CLI

Start the app in **worker-only mode** so workers keep polling while you use the CLI:

```bash
java -jar target/cloudwatch-integration-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow cloudwatch_integration_450 \
  --version 1 \
  --input '{"namespace": "test", "metricName": "test", "value": "sample-value", "threshold": "sample-threshold", "notifyEmail": "user@example.com"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w cloudwatch_integration_450 -s COMPLETED -c 5

```

## How to Extend

Swap in the AWS SDK calls. CloudWatchClient.putMetricData() in the metric worker, CloudWatchClient.putMetricAlarm() for alarm creation, and SNS or SES for notification delivery. The workflow definition stays exactly the same.

- **CheckStatusWorker** (`cw_check_status`): use the AWS SDK CloudWatchClient.describeAlarms() to check real alarm states
- **CreateAlarmWorker** (`cw_create_alarm`): use the AWS SDK CloudWatchClient.putMetricAlarm() to create real CloudWatch alarms
- **PutMetricWorker** (`cw_put_metric`): use the AWS SDK CloudWatchClient.putMetricData() to publish real metric data points

Connect each worker to the real CloudWatch API while keeping the same return structure, and the metric-alarm-notify pipeline adapts seamlessly.

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
cloudwatch-integration/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/cloudwatchintegration/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── CloudwatchIntegrationExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CheckStatusWorker.java
│       ├── CreateAlarmWorker.java
│       ├── CwNotifyWorker.java
│       └── PutMetricWorker.java
└── src/test/java/cloudwatchintegration/workers/
    ├── CheckStatusWorkerTest.java        # 2 tests
    ├── CreateAlarmWorkerTest.java        # 2 tests
    ├── CwNotifyWorkerTest.java        # 2 tests
    └── PutMetricWorkerTest.java        # 2 tests

```
