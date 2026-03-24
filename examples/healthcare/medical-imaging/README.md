# Medical Imaging in Java Using Conductor : Acquisition, Processing, AI Analysis, Radiology Reporting, and PACS Storage

## The Problem

You need to manage the lifecycle of a medical imaging study from acquisition through archival. An imaging modality (CT scanner, MRI, X-ray) produces DICOM images for a study. The raw images must be processed. applying window/level adjustments, multi-planar reconstructions, and de-identification for research use. Processed images are then analyzed, potentially with AI-assisted detection (lung nodules, fractures, hemorrhage). A radiologist report must be generated with structured findings, impressions, and follow-up recommendations. Finally, the study and report must be archived to the PACS (Picture Archiving and Communication System) for long-term storage and retrieval. Each step depends on the previous one, you cannot analyze unprocessed images, and you cannot archive without a finalized report.

Without orchestration, you'd build a monolithic RIS/PACS integration that polls the modality worklist, pulls DICOM images, runs the processing pipeline, calls the AI inference service, and pushes to PACS. all in a single service. If the AI analysis service is temporarily unavailable, you'd need retry logic. If the system crashes after processing but before archiving, the study is lost. ACR accreditation and HIPAA require a complete audit trail of every image from acquisition through storage.

## The Solution

**You just write the imaging workers. DICOM acquisition, image processing, AI-assisted analysis, radiology reporting, and PACS archival. Conductor handles stage sequencing, automatic retries when the AI analysis service is temporarily unavailable, and a complete audit trail for ACR accreditation and HIPAA.**

Each stage of the imaging pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of processing only after acquisition, analyzing only after processing, generating the report only after analysis, archiving to PACS as the final step, and maintaining a complete audit trail for ACR accreditation and HIPAA compliance.

### What You Write: Workers

Five workers form the imaging pipeline: AcquireWorker receives DICOM images from modalities, ProcessImageWorker applies windowing and reconstruction, AnalyzeImageWorker runs AI-assisted detection, ReportWorker generates the radiology report, and StoreWorker archives to PACS.

| Worker | Task | What It Does |
|---|---|---|
| **AcquireWorker** | `img_acquire` | Receives DICOM images from the modality (CT, MRI, X-ray) for the specified study and body part |
| **ProcessImageWorker** | `img_process` | Applies image processing. windowing, reconstruction, de-identification, and quality checks |
| **AnalyzeImageWorker** | `img_analyze` | Runs AI-assisted analysis to detect findings (nodules, fractures, hemorrhage) and measure structures |
| **ReportWorker** | `img_report` | Generates the radiology report with structured findings, impressions, and follow-up recommendations |
| **StoreWorker** | `img_store` | Archives the study images and finalized report to the PACS for long-term storage |

the workflow and compliance logic stay the same.

### The Workflow

```
img_acquire
 │
 ▼
img_process
 │
 ▼
img_analyze
 │
 ▼
img_report
 │
 ▼
img_store

```

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
