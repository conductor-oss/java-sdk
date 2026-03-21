# Data Lake Ingestion in Java Using Conductor : Schema Validation, Date Partitioning, Format Conversion, and Catalog Registration

## The Problem

You need to land data into your data lake in a way that's queryable, discoverable, and doesn't corrupt existing data. That means validating incoming records against the expected schema (rejecting malformed rows before they pollute the lake), partitioning records by date so queries only scan relevant partitions, converting from raw formats (JSON, CSV) into columnar formats (Parquet, ORC) for efficient analytics, writing the converted files to the correct lake path with atomic semantics, and updating the data catalog (Hive Metastore, AWS Glue, Apache Atlas) so tools like Athena, Presto, and Spark can find the new data.

Without orchestration, you'd write a monolithic Spark job or script that does schema validation, partitioning, conversion, writing, and catalog updates in one pass. If the write fails after converting 90% of the data, you'd restart the entire job. If the catalog update fails after a successful write, the data exists in the lake but is invisible to query engines. There's no visibility into which step failed, how many records were valid, or how many partitions were created.

## The Solution

**You just write the schema validation, date partitioning, format conversion, lake writing, and catalog update workers. Conductor handles strict ordering so the catalog is updated only after successful writes, retries when lake storage is unavailable, and tracking of record counts across validate-partition-convert-write stages.**

Each stage of the ingestion pipeline is a simple, independent worker. The schema validator checks incoming records and filters out malformed rows. The partitioner organizes valid records into date-based partition keys. The format converter transforms each partition into the target columnar format. The lake writer places the converted files at the correct paths with proper naming. The catalog updater registers the new partitions so query engines can access the data. Conductor executes them in strict sequence, ensures the catalog is only updated after a successful write, retries if the lake storage is temporarily unavailable, and tracks exactly how many records were validated, partitioned, and written. ### What You Write: Workers

Five workers manage the data lake ingestion pipeline: validating records against a schema, partitioning by date, converting to columnar formats like Parquet, writing to the lake path, and registering partitions in the data catalog.

| Worker | Task | What It Does |
|---|---|---|
| **ConvertFormatWorker** | `li_convert_format` | Convert Format. Computes and returns files |
| **PartitionByDateWorker** | `li_partition_by_date` | Partition By Date. Computes and returns partitions, partition count |
| **UpdateCatalogWorker** | `li_update_catalog` | Updates the catalog |
| **ValidateSchemaWorker** | `li_validate_schema` | Validate Schema. Computes and returns valid records, valid count |
| **WriteToLakeWorker** | `li_write_to_lake` | Write To Lake. Computes and returns files written, total bytes |

Workers implement data processing stages with representative outputs so the pipeline runs end-to-end without external data stores. Swap in real data sources and sinks. the pipeline structure and error handling stay the same.

### The Workflow

```
li_validate_schema
 │
 ▼
li_partition_by_date
 │
 ▼
li_convert_format
 │
 ▼
li_write_to_lake
 │
 ▼
li_update_catalog

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
