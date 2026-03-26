# Aws Integration

Orchestrates aws integration through a multi-stage Conductor workflow.

**Input:** `bucket`, `tableName`, `topicArn`, `payload` | **Timeout:** 60s

## Pipeline

```
    ┌───────────────┬────────────────────┬────────────────┐
    │ aws_s3_upload │ aws_dynamodb_write │ aws_sns_notify │
    └───────────────┴────────────────────┴────────────────┘
aws_verify
```

## Workers

**AwsDynamoDbWriteWorker** (`aws_dynamodb_write`): Writes an item to Amazon DynamoDB.

Reads `item`, `tableName`. Outputs `itemId`, `tableName`, `consumedCapacity`.

**AwsS3UploadWorker** (`aws_s3_upload`): Uploads an object to Amazon S3.

Reads `body`, `bucket`. Outputs `s3Key`, `bucket`, `etag`.

**AwsSnsNotifyWorker** (`aws_sns_notify`): perform  publishing a notification to Amazon SNS.

Reads `topicArn`. Outputs `messageId`, `topicArn`.

**AwsVerifyWorker** (`aws_verify`): Verifies that all AWS services completed successfully.

```java
boolean allPresent = s3Result != null && !s3Result.isEmpty()
```

Reads `dynamoResult`, `s3Result`, `snsResult`. Outputs `verified`.

## Tests

**32 tests** cover valid inputs, boundary values, null handling, and error paths.

```bash
mvn test
```

---

> **Run this example:** see [RUNNING.md](../../RUNNING.md) for setup, build, and CLI instructions.
