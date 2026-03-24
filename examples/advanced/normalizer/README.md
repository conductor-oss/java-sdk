# Normalizer

A data integration layer receives records from multiple sources with inconsistent formatting: US dates (MM/DD/YYYY) vs. ISO dates, phone numbers with or without country codes, addresses with abbreviations. The normalizer pipeline applies source-specific rules to produce uniformly formatted output records.

## Pipeline

```
[nrm_detect_format]
     |
     v
     <SWITCH>
       |-- json -> [nrm_convert_json]
       |-- xml -> [nrm_convert_xml]
       |-- csv -> [nrm_convert_csv]
       +-- default -> [nrm_convert_json]
     |
     v
[nrm_output]
```

**Workflow inputs:** `rawInput`, `sourceSystem`

## Workers

**NrmConvertCsvWorker** (task: `nrm_convert_csv`)

- Writes `canonical`

**NrmConvertJsonWorker** (task: `nrm_convert_json`)

- Writes `canonical`

**NrmConvertXmlWorker** (task: `nrm_convert_xml`)

- Writes `canonical`

**NrmDetectFormatWorker** (task: `nrm_detect_format`)

- Trims whitespace
- Reads `rawInput`. Writes `detectedFormat`

**NrmOutputWorker** (task: `nrm_output`)

- Writes `normalized`, `originalFormat`, `outputFormat`

---

**20 tests** | Workflow: `nrm_normalizer` | Timeout: 60s

See [RUNNING.md](../../RUNNING.md) for setup and usage.
