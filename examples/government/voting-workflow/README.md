# Voting Workflow in Java with Conductor

Processes a voter's participation in an election: confirming registration, verifying identity, casting the ballot, counting the vote, and certifying results. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.

## The Problem

You need to process a voter's participation in an election. from registration verification to vote certification. The voter's registration is confirmed, their identity is verified against the voter roll, they cast their ballot, the vote is counted, and the results are certified. Every step must maintain ballot secrecy while ensuring auditability. Counting a vote from an unverified voter undermines election integrity; failing to count a legitimate vote disenfranchises a citizen.

Without orchestration, you'd manage the voting process through a combination of poll books, voting machines, and tally systems. manually verifying registrations against printed rolls, handling provisional ballots when verification fails, reconciling machine counts with paper trails, and producing certified results under strict legal deadlines.

## The Solution

**You just write the registration confirmation, identity verification, ballot casting, vote counting, and result certification logic. Conductor handles verification retries, ballot sequencing, and election audit trails.**

Each voting concern is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of executing them in order (register, verify identity, cast ballot, count, certify), maintaining a complete audit trail while preserving ballot secrecy, tracking every step with timestamps, and resuming from the last step if the process crashes. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Voter verification, ballot issuance, vote recording, and tally workers handle election processes with independent, auditable steps.

| Worker | Task | What It Does |
|---|---|---|
| **CastBallotWorker** | `vtw_cast_ballot` | Records the ballot casting event for the election and assigns a ballot ID |
| **CertifyWorker** | `vtw_certify` | Certifies the election results with the total vote count and issues a certification ID |
| **CountWorker** | `vtw_count` | Tabulates the ballot and adds it to the running vote total for the election |
| **RegisterWorker** | `vtw_register` | Confirms the voter's registration at their assigned precinct |
| **VerifyIdentityWorker** | `vtw_verify_identity` | Verifies the voter's identity against the voter roll |

Workers implement government operations. application processing, compliance checks, notifications,  with realistic outputs. Replace with real agency system integrations and the workflow stays the same.

### The Workflow

```
vtw_register
    │
    ▼
vtw_verify_identity
    │
    ▼
vtw_cast_ballot
    │
    ▼
vtw_count
    │
    ▼
vtw_certify

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
java -jar target/voting-workflow-1.0.0.jar

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
java -jar target/voting-workflow-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow vtw_voting_workflow \
  --version 1 \
  --input '{"voterId": "TEST-001", "electionId": "TEST-001", "precinct": "sample-precinct"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w vtw_voting_workflow -s COMPLETED -c 5

```

## How to Extend

Connect each worker to your real election systems. your voter registration database for eligibility, your ballot management system for casting, your tabulation platform for counting and certification, and the workflow runs identically in production.

- **Registration verifier**: check voter registration against your state voter registration database (ERIC, state SOS systems) and verify precinct assignment
- **Identity verifier**: verify voter identity per state requirements (photo ID, signature match, affidavit) with provisional ballot fallback
- **Ballot caster**: record ballot casting event (not the vote itself) with timestamp and polling location; maintain ballot secrecy
- **Vote counter**: tabulate votes using certified counting equipment with audit logs; support hand-count verification for audits
- **Results certifier**: aggregate precinct results, run canvassing procedures, and produce certified results per state election code deadlines

Update verification rules or tally methods and the voting pipeline remains structurally unchanged.

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
voting-workflow-voting-workflow/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/votingworkflow/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── VotingWorkflowExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── CastBallotWorker.java
│       ├── CertifyWorker.java
│       ├── CountWorker.java
│       ├── RegisterWorker.java
│       └── VerifyIdentityWorker.java
└── src/test/java/votingworkflow/workers/
    ├── CastBallotWorkerTest.java
    ├── CertifyWorkerTest.java
    └── RegisterWorkerTest.java

```
