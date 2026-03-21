# AWS Integration in Java Using Conductor

## Writing to Multiple AWS Services Reliably

When an event arrives, you often need to persist it across multiple AWS services simultaneously. store the raw payload in S3 for archival, write a record to DynamoDB for querying, and publish a notification to SNS for downstream consumers. These three operations are independent of each other and can run in parallel, but all three must succeed for the operation to be considered complete. If any one fails, you need visibility into which service failed and the ability to retry just that step.

Without orchestration, you would manage three async calls manually, implement your own fan-out/join logic, handle partial failures, and build retry loops for each service. Conductor's FORK_JOIN handles all of this declaratively.

## The Solution

**You just write the AWS service workers. S3 upload, DynamoDB write, SNS publish, and cross-service verification. Conductor handles parallel fan-out via FORK_JOIN, per-service retries, and cross-service verification tracking.**

Each worker integrates with one external system. Conductor manages the integration sequence, retry logic, timeout handling, and data transformation between systems.

### What You Write: Workers

Four workers coordinate AWS writes: AwsS3UploadWorker stores objects, AwsDynamoDbWriteWorker persists items, AwsSnsNotifyWorker publishes notifications, and AwsVerifyWorker confirms all three parallel branches completed successfully.

| Worker | Task | What It Does |
|---|---|---|
| **AwsS3UploadWorker** | `aws_s3_upload` | Uploads the payload to S3. stores the object at `data/{id}.json` in the specified bucket and returns the s3Key |
| **AwsDynamoDbWriteWorker** | `aws_dynamodb_write` | Writes the payload as a DynamoDB item. inserts the document into the specified table and returns the itemId |
| **AwsSnsNotifyWorker** | `aws_sns_notify` | Publishes a notification to SNS. sends the payload as a message to the specified topicArn and returns the messageId |
| **AwsVerifyWorker** | `aws_verify` | Verifies all three services completed. checks the s3Key, DynamoDB itemId, and SNS messageId from the parallel branches and confirms the fan-out succeeded |

the workflow orchestration and error handling stay the same.

### The Workflow

```
FORK_JOIN
 ├── aws_s3_upload
 ├── aws_dynamodb_write
 └── aws_sns_notify
 │
 ▼
JOIN (wait for all branches)
aws_verify

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
