# Implementing Data Encryption Pipeline in Java with Conductor : Classification, Key Generation, Encryption, and Verification

## The Problem

You need to encrypt sensitive data; but not all data needs the same level of encryption. PII requires AES-256, internal data needs AES-128, and public data doesn't need encryption at all. You must classify the data first, generate the right encryption key for its classification level, encrypt with the appropriate algorithm, and verify that encryption was applied correctly (the plaintext is gone, the ciphertext is valid).

Without orchestration, encryption is either applied uniformly (wasteful) or ad hoc (inconsistent). Key generation is disconnected from data classification, encryption happens without verification, and there's no audit trail of what was encrypted with which key and when.

## The Solution

**You just write the classification rules and KMS integration. Conductor handles the strict ordering from classification to verification, retries on KMS timeouts, and an audit record of every key generated and field encrypted.**

Each encryption step is an independent worker. data classification, key generation, encryption, and verification. Conductor runs them in sequence: classify the data, generate the appropriate key, encrypt, then verify. Every encryption operation is tracked with classification level, key ID, algorithm used, and verification result. ### What You Write: Workers

The encryption pipeline uses four workers: ClassifyDataWorker determines sensitivity levels, GenerateKeyWorker creates the appropriate AES key, EncryptDataWorker applies the cipher, and VerifyEncryptionWorker confirms no plaintext remains.

| Worker | Task | What It Does |
|---|---|---|
| **ClassifyDataWorker** | `de_classify_data` | Scans data sources for PII and classifies them by sensitivity level (public, internal, sensitive) |
| **EncryptDataWorker** | `de_encrypt_data` | Encrypts identified PII fields at rest using the generated encryption key |
| **GenerateKeyWorker** | `de_generate_key` | Generates an AES-256 encryption key and stores it in the KMS |
| **VerifyEncryptionWorker** | `de_verify_encryption` | Verifies that encryption was applied correctly and no plaintext PII remains accessible |

the workflow logic stays the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
