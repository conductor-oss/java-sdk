# Data Encryption

A customer database contains PII that must be encrypted at rest. The pipeline classifies the data source as sensitive, generates an AES-256 encryption key in KMS, encrypts 4 PII fields, and verifies that no plaintext PII remains accessible.

## Workflow

```
de_classify_data ──> de_generate_key ──> de_encrypt_data ──> de_verify_encryption
```

Workflow `data_encryption_workflow` accepts `dataSource` and `classification`. Times out after `600` seconds.

## Workers

**ClassifyDataWorker** (`de_classify_data`) -- reports `"customer-database: PII detected -- classified as sensitive"`. Returns `classify_dataId` = `"CLASSIFY_DATA-1355"`.

**GenerateKeyWorker** (`de_generate_key`) -- generates an encryption key. Reports `"AES-256 encryption key generated and stored in KMS"`. Returns `generate_key` = `true`.

**EncryptDataWorker** (`de_encrypt_data`) -- encrypts sensitive fields. Reports `"4 PII fields encrypted at rest"`. Returns `encrypt_data` = `true`.

**VerifyEncryptionWorker** (`de_verify_encryption`) -- confirms encryption coverage. Reports `"Encryption verified: no plaintext PII accessible"`. Returns `verify_encryption` = `true`.

## Workflow Output

The workflow produces `classify_dataResult`, `verify_encryptionResult` as output parameters, capturing the result of each pipeline stage for downstream consumers and observability.

## Project Structure

This example contains 4 worker implementations in `src/main/java/*/workers/`, the workflow definition in `src/main/resources/workflow.json`, and integration tests in `src/test/`. The workflow `data_encryption_workflow` defines 4 tasks with input parameters `dataSource`, `classification` and a timeout of `600` seconds.

## Workflow Definition Details

Schema version `2`, workflow version `1`. Owner: `examples@orkes.io`.

## Tests

2 tests verify the end-to-end encryption pipeline from classification through verification.

## Running

See [RUNNING.md](../../RUNNING.md) for setup and execution instructions.
