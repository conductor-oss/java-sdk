# Certificate Rotation in Java with Conductor

It's 2 AM on a Saturday. Your wildcard TLS cert expired eleven minutes ago. Every service behind the load balancer is failing TLS handshakes, browsers are showing "Your connection is not private" to every customer, and service-to-service calls across your mesh are throwing `SSLHandshakeException`. The cert was set to expire "sometime in March" and the calendar reminder went to an engineer who left the company in January. Now someone needs to generate a new cert, deploy it to four load balancers and six reverse proxies, and verify handshakes, all while the entire platform is down. This workflow uses [Conductor](https://github.com/conductor-oss/conductor) to automate the full certificate rotation lifecycle, discover expiring certs, generate replacements, deploy to infrastructure, and verify the handshake, before anyone's pager goes off.

## Before the Certificate Expires

A TLS certificate is 15 days from expiration. Someone needs to notice, generate a replacement, deploy it to every load balancer and reverse proxy that serves the domain, and verify the new cert actually works, all without causing a service interruption. Miss this window and your customers see browser warnings or, worse, a hard outage.

Without orchestration, you'd wire all of this together in a single monolithic class. Managing execution order manually, writing try/catch blocks around every step, building retry loops with backoff, and adding logging to understand what happened when things go wrong. That code becomes brittle, hard to test, and impossible to observe at scale.

## The Solution

**You write the certificate lifecycle logic. Conductor handles discovery-to-verification sequencing, retries, and rotation audit trails.**

Each worker automates one operational step. Conductor manages execution sequencing, rollback on failure, timeout enforcement, and full audit logging. Your workers call the infrastructure APIs.

### What You Write: Workers

Four workers manage the certificate lifecycle. Discovering expiring certs, generating replacements, deploying to load balancers, and verifying the TLS handshake.

| Worker | Task | What It Does |
|---|---|---|
| `DiscoverWorker` | `cr_discover` | Scans the given domain to find a certificate expiring within 15 days and returns a discovery ID |
| `GenerateWorker` | `cr_generate` | Issues a new certificate from the certificate authority for the expiring domain |
| `DeployWorker` | `cr_deploy` | Deploys the newly generated certificate to load balancers and reverse proxies |
| `VerifyWorker` | `cr_verify` | Performs a TLS handshake against the domain to confirm the new certificate is active and valid |

Workers simulate infrastructure operations with realistic output so you can see the automation flow without affecting real systems. Replace with real infrastructure API calls, the workflow and rollback logic stay the same.

### What Conductor Gives You For Free

| Capability | How It Works |
|---|---|
| **Retries with backoff** | If a worker fails, Conductor retries automatically. Configurable per task |
| **Durability** | If the process crashes mid-execution, Conductor resumes from exactly where it left off |
| **Observability** | Every task execution is tracked with inputs, outputs, timing, and status.; no logging code needed |
| **Timeout management** | Per-task timeouts prevent hung workers from blocking the pipeline |

### The Workflow

```
cr_discover
    |
    v
cr_generate
    |
    v
cr_deploy
    |
    v
cr_verify
```

## Example Output

```
=== Example 331: Certificate Rotation ===

  [discover] Found expiring cert, expires in 15 days
  [generate] New certificate issued
  [deploy] Certificate deployed to load balancers
  [verify] TLS handshake verified
  discoverResult: {discoverId=DISCOVER-1331, success=true}
  verifyResult: {verify=true}

Result: PASSED
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
docker run -d -p 8080:8080 -p 1234:5000 orkesio/orkes-conductor-standalone:latest

# Wait for Conductor to be ready
until curl -sf http://localhost:8080/health > /dev/null; do sleep 2; done

# Build and run
mvn package -DskipTests
java -jar target/certificate-rotation-1.0.0.jar
```

### Option 3: Use the run script

```bash
./run.sh

# Or on a custom port:
CONDUCTOR_PORT=9090 ./run.sh

# Or pointing at an existing Conductor:
CONDUCTOR_BASE_URL=http://localhost:9090/api ./run.sh
```

### Sample Output

```
=== Example 331: Certificate Rotation ===

  [discover] Found expiring cert, expires in 15 days
  [generate] New certificate issued
  [deploy] Certificate deployed to load balancers
  [verify] TLS handshake verified
  discoverResult: {discoverId=DISCOVER-1331, success=true}
  verifyResult: {verify=true}

Result: PASSED
```

## Configuration

| Environment Variable | Default | Description |
|---|---|---|
| `CONDUCTOR_BASE_URL` | `http://localhost:8080/api` | Conductor server URL |
| `CONDUCTOR_PORT` | `8080` | Host port for Conductor (Docker Compose only) |

## Using the Conductor CLI

```bash
conductor workflow start \
  --workflow certificate_rotation_workflow \
  --version 1 \
  --input '{"domain": "api.example.com", "certType": "RSA-2048"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w certificate_rotation_workflow -s COMPLETED -c 5
```

## How to Extend

Each worker handles one certificate lifecycle step. Replace the simulated calls with Let's Encrypt ACME, AWS Certificate Manager, or HashiCorp Vault PKI, and the rotation workflow runs unchanged.

- **`DiscoverWorker`**: Query AWS Certificate Manager, Let's Encrypt, or scan endpoints with OpenSSL to find certificates approaching expiration.

- **`GenerateWorker`**: Call the Let's Encrypt ACME protocol, AWS ACM `RequestCertificate`, or HashiCorp Vault PKI to issue new certificates.

- **`DeployWorker`**: Push certificates to AWS ACM-backed ALBs, reload NGINX configurations, or update Kubernetes TLS secrets.

- **`VerifyWorker`**: Perform real TLS handshakes with `SSLSocket` or OpenSSL to validate the new certificate chain, expiration date, and hostname matching.

Integrate with Let's Encrypt and your load balancer API; the rotation workflow continues with the same interface.

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
certificate-rotation-certificate-rotation/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/certificaterotation/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java             # Main entry point
│   └── workers/
│       ├── DeployWorker.java        # Deploys new cert to load balancers
│       ├── DiscoverWorker.java      # Finds certificates nearing expiration
│       ├── GenerateWorker.java      # Issues replacement certificate from CA
│       └── VerifyWorker.java        # Validates TLS handshake with new cert
└── src/test/java/certificaterotation/workers/
    ├── DeployWorkerTest.java
    ├── DiscoverWorkerTest.java
    ├── GenerateWorkerTest.java
    └── VerifyWorkerTest.java
```
