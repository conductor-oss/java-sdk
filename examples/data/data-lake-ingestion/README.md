# Data Lake Ingestion

Raw event data arrives from mobile apps, web servers, and IoT devices in different formats (JSON, CSV, Avro). Before landing in the data lake, each payload needs schema detection, format normalization, partitioning by event type and date, and registration in the Hive metastore so Spark jobs can query it immediately.

## Pipeline

```
[li_validate_schema]
     |
     v
[li_partition_by_date]
     |
     v
[li_convert_format]
     |
     v
[li_write_to_lake]
     |
     v
[li_update_catalog]
```

**Workflow inputs:** `records`, `lakePath`, `format`

## Workers

**ConvertFormatWorker** (task: `li_convert_format`)

- Uppercases strings
- Reads `partitions`, `targetFormat`. Writes `files`

**PartitionByDateWorker** (task: `li_partition_by_date`)

- Truncates strings to first 10 character(s)
- Reads `records`, `lakePath`. Writes `partitions`, `partitionCount`

**UpdateCatalogWorker** (task: `li_update_catalog`)

- Reads `lakePath`, `filesWritten`, `totalRecords`. Writes `updated`, `summary`

**ValidateSchemaWorker** (task: `li_validate_schema`)

- Reads `records`. Writes `validRecords`, `validCount`

**WriteToLakeWorker** (task: `li_write_to_lake`)

- Reads `files`, `lakePath`. Writes `filesWritten`, `totalBytes`

---

**0 tests** | Workflow: `data_lake_ingestion` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
