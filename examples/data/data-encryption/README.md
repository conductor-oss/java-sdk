# Data Encryption in Java Using Conductor :  Key Generation, Field-Level Encryption, Vault Storage, and Verification

A Java Conductor workflow example for field-level data encryption. generating an encryption key for the specified algorithm, identifying which fields in each record contain sensitive data, encrypting those fields while leaving non-sensitive fields in plaintext, storing the key reference in a vault, and verifying that all sensitive fields are properly encrypted in the output. Uses [Conductor](https://github.## The Problem

You need to encrypt sensitive fields in your dataset before storing it or sharing it downstream. SSNs, credit card numbers, medical record IDs. While keeping non-sensitive fields (names, categories, timestamps) readable for analytics. That means generating a cryptographic key for the chosen algorithm (AES-256, RSA), scanning records to identify which fields match the configured sensitive field list, encrypting only those fields, securely storing the key reference so authorized consumers can decrypt later, and verifying that every sensitive field in the output is actually encrypted (not accidentally left in plaintext). The order matters: you must generate the key before encrypting, and you must store the key reference before anyone can decrypt.

Without orchestration, you'd generate a key inline, loop through records encrypting fields in-place, and hope nothing crashes between key generation and vault storage. If the encryption step fails on record 5,000 of 10,000, you'd restart from scratch. If the vault write fails after encryption, the encrypted data is unrecoverable. There's no audit trail showing which key was used, which fields were encrypted, or whether verification passed.

## The Solution

**You just write the key generation, field identification, encryption, vault storage, and verification workers. Conductor handles strict sequencing so keys exist before encryption starts, retries when vault services are temporarily unavailable, and a complete audit trail of every encryption decision.**

Each stage of the encryption pipeline is a simple, independent worker. The key generator creates a cryptographic key for the specified algorithm. The field identifier scans records against the configured sensitive field list to determine what needs encrypting. The encryptor applies field-level encryption using the generated key. The vault writer stores the key reference so authorized consumers can decrypt. The verifier checks that every record has the correct encryption key ID tag and that no sensitive field was left in plaintext. Conductor executes them in strict sequence, ensures the key exists before encryption starts and the key is vaulted before the workflow completes, retries if the vault is temporarily unavailable, and tracks every step with full audit visibility. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers implement the encryption pipeline: generating cryptographic keys, identifying sensitive fields in each record, encrypting those fields, storing key references in a vault, and verifying that no plaintext leaks through.

| Worker | Task | What It Does |
|---|---|---|
| **EncryptFieldsWorker** | `dn_encrypt_fields` | Encrypts sensitive fields in records (simulated with Base64 encoding). |
| **GenerateKeyWorker** | `dn_generate_key` | Generates an encryption key. |
| **IdentifyFieldsWorker** | `dn_identify_fields` | Identifies which sensitive fields exist in the records. |
| **StoreKeyRefWorker** | `dn_store_key_ref` | Stores the encryption key reference in a vault. |
| **VerifyEncryptedWorker** | `dn_verify_encrypted` | Verifies that all records are properly tagged with the encryption key ID. |

Workers simulate data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks .  the pipeline structure and error handling stay the same.

### The Workflow

```
dn_generate_key
    │
    ▼
dn_identify_fields
    │
    ▼
dn_encrypt_fields
    │
    ▼
dn_store_key_ref
    │
    ▼
dn_verify_encrypted
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
  --workflow data_encryption \
  --version 1 \
  --input '{"records": "test-value", "algorithm": "test-value", "sensitiveFields": "test-value"}'
```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w data_encryption -s COMPLETED -c 5
```

## How to Extend

Integrate AWS KMS or HashiCorp Vault for key generation, replace Base64 with real AES-256 encryption via javax.crypto, and the field-level encryption workflow runs unchanged.

- **GenerateKeyWorker** → generate real keys using AWS KMS, HashiCorp Vault Transit, or `javax.crypto.KeyGenerator` with AES-256/RSA-2048
- **IdentifyFieldsWorker** → auto-detect sensitive fields using pattern matching (credit card regex, SSN patterns) or a data classification service instead of a static field list
- **EncryptFieldsWorker** → replace Base64 simulation with real `javax.crypto.Cipher` encryption, format-preserving encryption (FPE) for fields that need to maintain their format, or envelope encryption
- **StoreKeyRefWorker** → write key metadata to HashiCorp Vault, AWS Secrets Manager, Azure Key Vault, or Google Secret Manager
- **VerifyEncryptedWorker** → attempt decryption of a sample record to confirm round-trip integrity, and flag any fields that appear to still contain plaintext patterns

Switching from Base64 simulation to real AES-256 encryption or changing the vault provider does not affect the workflow, provided key IDs and encrypted field markers are returned consistently.

**Add new stages** by inserting tasks in `workflow.json`, for example, a key rotation step that re-encrypts with a new key, an access policy writer that controls who can decrypt, or a compliance logger that records encryption events for SOC 2 auditing.

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
data-encryption/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/dataencryption/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── DataEncryptionExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── EncryptFieldsWorker.java
│       ├── GenerateKeyWorker.java
│       ├── IdentifyFieldsWorker.java
│       ├── StoreKeyRefWorker.java
│       └── VerifyEncryptedWorker.java
└── src/test/java/dataencryption/workers/
    ├── EncryptFieldsWorkerTest.java        # 6 tests
    ├── GenerateKeyWorkerTest.java        # 6 tests
    ├── IdentifyFieldsWorkerTest.java        # 6 tests
    ├── StoreKeyRefWorkerTest.java        # 4 tests
    └── VerifyEncryptedWorkerTest.java        # 6 tests
```
