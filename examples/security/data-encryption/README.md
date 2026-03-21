# Implementing Data Encryption Pipeline in Java with Conductor :  Classification, Key Generation, Encryption, and Verification

A Java Conductor workflow example for data encryption .  classifying data sensitivity, generating appropriate encryption keys, encrypting the data, and verifying that encryption was applied correctly.

## The Problem

You need to encrypt sensitive data; but not all data needs the same level of encryption. PII requires AES-256, internal data needs AES-128, and public data doesn't need encryption at all. You must classify the data first, generate the right encryption key for its classification level, encrypt with the appropriate algorithm, and verify that encryption was applied correctly (the plaintext is gone, the ciphertext is valid).

Without orchestration, encryption is either applied uniformly (wasteful) or ad hoc (inconsistent). Key generation is disconnected from data classification, encryption happens without verification, and there's no audit trail of what was encrypted with which key and when.

## The Solution

**You just write the classification rules and KMS integration. Conductor handles the strict ordering from classification to verification, retries on KMS timeouts, and an audit record of every key generated and field encrypted.**

Each encryption step is an independent worker .  data classification, key generation, encryption, and verification. Conductor runs them in sequence: classify the data, generate the appropriate key, encrypt, then verify. Every encryption operation is tracked with classification level, key ID, algorithm used, and verification result. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

The encryption pipeline uses four workers: ClassifyDataWorker determines sensitivity levels, GenerateKeyWorker creates the appropriate AES key, EncryptDataWorker applies the cipher, and VerifyEncryptionWorker confirms no plaintext remains.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyDataWorker** | `de_classify_data` | Scans data sources for PII and classifies them by sensitivity level (public, internal, sensitive) |
| **EncryptDataWorker** | `de_encrypt_data` | Encrypts identified PII fields at rest using the generated encryption key |
| **GenerateKeyWorker** | `de_generate_key` | Generates an AES-256 encryption key and stores it in the KMS |
| **VerifyEncryptionWorker** | `de_verify_encryption` | Verifies that encryption was applied correctly and no plaintext PII remains accessible |

Workers simulate security checks and remediation actions with realistic findings so you can see the response flow without live security tools. Replace with real scanner and SIEM integrations .  the workflow logic stays the same.

### The Workflow

```
de_classify_data
    │
    ▼
de_generate_key
    │
    ▼
de_encrypt_data
    │
    ▼
de_verify_encryption

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
java -jar target/data-encryption-1.0.0.jar

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
java -jar target/data-encryption-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow data_encryption_workflow \
  --version 1 \
  --input '{"dataSource": "api", "classification": "sample-classification"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_encryption_workflow -s COMPLETED -c 5

```

## How to Extend

Each worker owns one encryption concern .  connect ClassifyDataWorker to Google Cloud DLP or Amazon Macie, GenerateKeyWorker to AWS KMS or Azure Key Vault, and the classify-encrypt-verify workflow stays the same.

- **ClassifyDataWorker** (`de_classify_data`): classify data using DLP tools (Google Cloud DLP, Amazon Macie) or regex-based PII detection for sensitivity levels
- **EncryptDataWorker** (`de_encrypt_data`): encrypt data using JCE with the generated key, or use envelope encryption via cloud KMS
- **GenerateKeyWorker** (`de_generate_key`): generate real encryption keys via AWS KMS, Azure Key Vault, or HashiCorp Vault Transit engine

Wire in your real KMS provider and the classify-encrypt-verify flow transfers seamlessly.

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
data-encryption-data-encryption/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dataencryption/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MainExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── ClassifyDataWorker.java
│       ├── EncryptDataWorker.java
│       ├── GenerateKeyWorker.java
│       └── VerifyEncryptionWorker.java
└── src/test/java/dataencryption/
    └── MainExampleTest.java        # 2 tests .  workflow resource loading, worker instantiation

```
