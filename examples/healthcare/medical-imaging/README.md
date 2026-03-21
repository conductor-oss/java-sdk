# Medical Imaging in Java Using Conductor :  Acquisition, Processing, AI Analysis, Radiology Reporting, and PACS Storage

A Java Conductor workflow example for medical imaging. acquiring DICOM images from modalities (CT, MRI, X-ray), processing raw images (windowing, reconstruction, de-identification), running AI-assisted analysis for findings detection, generating the radiology report, and archiving to PACS. Uses [Conductor](https://github.com/conductor-oss/conductor) to orchestrate independent services as workers.
## The Problem

You need to manage the lifecycle of a medical imaging study from acquisition through archival. An imaging modality (CT scanner, MRI, X-ray) produces DICOM images for a study. The raw images must be processed. applying window/level adjustments, multi-planar reconstructions, and de-identification for research use. Processed images are then analyzed, potentially with AI-assisted detection (lung nodules, fractures, hemorrhage). A radiologist report must be generated with structured findings, impressions, and follow-up recommendations. Finally, the study and report must be archived to the PACS (Picture Archiving and Communication System) for long-term storage and retrieval. Each step depends on the previous one,  you cannot analyze unprocessed images, and you cannot archive without a finalized report.

Without orchestration, you'd build a monolithic RIS/PACS integration that polls the modality worklist, pulls DICOM images, runs the processing pipeline, calls the AI inference service, and pushes to PACS. all in a single service. If the AI analysis service is temporarily unavailable, you'd need retry logic. If the system crashes after processing but before archiving, the study is lost. ACR accreditation and HIPAA require a complete audit trail of every image from acquisition through storage.

## The Solution

**You just write the imaging workers. DICOM acquisition, image processing, AI-assisted analysis, radiology reporting, and PACS archival. Conductor handles stage sequencing, automatic retries when the AI analysis service is temporarily unavailable, and a complete audit trail for ACR accreditation and HIPAA.**

Each stage of the imaging pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of processing only after acquisition, analyzing only after processing, generating the report only after analysis, archiving to PACS as the final step, and maintaining a complete audit trail for ACR accreditation and HIPAA compliance. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers form the imaging pipeline: AcquireWorker receives DICOM images from modalities, ProcessImageWorker applies windowing and reconstruction, AnalyzeImageWorker runs AI-assisted detection, ReportWorker generates the radiology report, and StoreWorker archives to PACS.

| Worker | Task | What It Does |
|---|---|---|
| **AcquireWorker** | `img_acquire` | Receives DICOM images from the modality (CT, MRI, X-ray) for the specified study and body part |
| **ProcessImageWorker** | `img_process` | Applies image processing. windowing, reconstruction, de-identification, and quality checks |
| **AnalyzeImageWorker** | `img_analyze` | Runs AI-assisted analysis to detect findings (nodules, fractures, hemorrhage) and measure structures |
| **ReportWorker** | `img_report` | Generates the radiology report with structured findings, impressions, and follow-up recommendations |
| **StoreWorker** | `img_store` | Archives the study images and finalized report to the PACS for long-term storage |

Workers implement clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations. the workflow and compliance logic stay the same.

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
java -jar target/medical-imaging-1.0.0.jar

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
java -jar target/medical-imaging-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow medical_imaging_workflow \
  --version 1 \
  --input '{"studyId": "TEST-001", "patientId": "TEST-001", "modality": "sample-modality", "bodyPart": "sample-bodyPart"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w medical_imaging_workflow -s COMPLETED -c 5

```

## How to Extend

Connect AcquireWorker to your DICOM receiver (dcm4chee, Orthanc), AnalyzeImageWorker to your AI inference service (Aidoc, Viz.ai), and StoreWorker to your PACS for long-term archival. The workflow definition stays exactly the same.

- **AcquireWorker** → integrate with your DICOM receiver (dcm4chee, Orthanc) to pull studies from the modality worklist as they complete
- **ProcessImageWorker** → run real image processing with ITK, OpenCV, or your radiology workstation's processing engine
- **AnalyzeImageWorker** → call your FDA-cleared AI analysis service (Aidoc, Viz.ai, Qure.ai) for automated findings detection
- **ReportWorker** → integrate with your RIS for structured reporting using RadLex terminology and BI-RADS/Lung-RADS categories
- **StoreWorker** → send DICOM C-STORE commands to your PACS and attach the report as a DICOM SR (Structured Report)
- Add a **PriorStudyWorker** before analysis to fetch prior studies from PACS for comparison reads

Swap in your DICOM receiver, AI inference service, and PACS store while maintaining the same output contract, and the imaging workflow needs no modifications.

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
medical-imaging/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/medicalimaging/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── MedicalImagingExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AcquireWorker.java
│       ├── AnalyzeImageWorker.java
│       ├── ProcessImageWorker.java
│       ├── ReportWorker.java
│       └── StoreWorker.java
└── src/test/java/medicalimaging/workers/
    ├── AcquireWorkerTest.java        # 2 tests
    ├── AnalyzeImageWorkerTest.java        # 2 tests
    ├── ProcessImageWorkerTest.java        # 2 tests
    ├── ReportWorkerTest.java        # 2 tests
    └── StoreWorkerTest.java        # 2 tests

```
