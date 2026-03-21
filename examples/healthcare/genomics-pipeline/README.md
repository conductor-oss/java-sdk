# Genomics Pipeline in Java Using Conductor :  Sequencing, Alignment, Variant Calling, Annotation, and Clinical Reporting

A Java Conductor workflow example for a clinical genomics pipeline .  processing DNA samples through sequencing, aligning reads to a reference genome, calling genetic variants, annotating variants with clinical significance, and generating a diagnostic report. Uses [Conductor](https://github.

## The Problem

You need to process genomic samples for clinical diagnostics. A DNA sample arrives from the lab and must be sequenced to produce raw reads (FASTQ). Those reads must be aligned to a reference genome (GRCh38) to produce a BAM file. Variant calling identifies SNPs, indels, and structural variants in the aligned data (VCF). Each variant must be annotated with clinical significance from databases like ClinVar, OMIM, and gnomAD. Finally, a clinical report must be generated summarizing pathogenic and likely pathogenic findings for the ordering physician. Each step produces large intermediate files and depends strictly on the previous step's output .  you cannot call variants without aligned reads, and you cannot annotate without called variants.

Without orchestration, you'd chain bioinformatics tools (BWA, GATK, VEP) together in a shell script or Nextflow pipeline, handling failures with ad-hoc retry logic. If the variant calling step fails halfway through a multi-hour run, you'd need to figure out where to restart. If the annotation database is temporarily unavailable, the entire pipeline stalls. CAP/CLIA regulations require a complete chain of custody from sample receipt to report delivery for every clinical genomic test.

## The Solution

**You just write the genomics workers. Sequencing, read alignment, variant calling, clinical annotation, and diagnostic reporting. Conductor handles stage dependencies, automatic retries when a bioinformatics service fails mid-run, and a CAP/CLIA-compliant chain of custody from sample to report.**

Each stage of the genomics pipeline is a simple, independent worker .  a plain Java class that does one thing. Conductor takes care of running sequencing before alignment, calling variants only after alignment completes, annotating only after variants are called, retrying if any bioinformatics service is temporarily unavailable, and maintaining a CAP/CLIA-compliant audit trail from sample to report. You get all of that, without writing a single line of orchestration code.

### What You Write: Workers

Five workers cover the genomics pipeline: SequenceWorker produces raw reads, AlignWorker maps to the reference genome, CallVariantsWorker identifies genetic variants, AnnotateWorker adds clinical significance, and GenomicsReportWorker generates the diagnostic report.

| Worker | Task | What It Does |
|---|---|---|
| **SequenceWorker** | `gen_sequence` | Processes the DNA sample and produces raw sequencing reads (FASTQ) for the specified panel type |
| **AlignWorker** | `gen_align` | Aligns raw reads to the reference genome (GRCh38) and produces a sorted, indexed BAM file |
| **CallVariantsWorker** | `gen_call_variants` | Identifies SNPs, indels, and structural variants from aligned reads and produces a VCF file |
| **AnnotateWorker** | `gen_annotate` | Annotates each variant with clinical significance from ClinVar, OMIM, gnomAD, and functional predictions |
| **GenomicsReportWorker** | `gen_report` | Generates the clinical genomics report with pathogenic findings, gene coverage, and interpretation |

Workers simulate clinical and administrative operations with realistic outputs so you can see the care workflow end-to-end. Replace with real EHR and system integrations .  the workflow and compliance logic stay the same.

### The Workflow

```
gen_sequence
    │
    ▼
gen_align
    │
    ▼
gen_call_variants
    │
    ▼
gen_annotate
    │
    ▼
gen_report

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
java -jar target/genomics-pipeline-1.0.0.jar

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
java -jar target/genomics-pipeline-1.0.0.jar --workers

```

Then in a separate terminal:

```bash
conductor workflow start \
  --workflow genomics_pipeline_workflow \
  --version 1 \
  --input '{"sampleId": "TEST-001", "patientId": "TEST-001", "panelType": "standard"}'

```

### Check workflow status

```bash
conductor workflow status <workflow_id>
conductor workflow get-execution <workflow_id> -c
conductor workflow search -w genomics_pipeline_workflow -s COMPLETED -c 5

```

## How to Extend

Connect SequenceWorker to your sequencer (Illumina BaseSpace), AlignWorker to BWA-MEM2 on your HPC cluster, and AnnotateWorker to Ensembl VEP with ClinVar and gnomAD databases. The workflow definition stays exactly the same.

- **SequenceWorker** → integrate with Illumina BaseSpace or your LIMS to pull raw FASTQ files when a sequencing run completes
- **AlignWorker** → submit BWA-MEM2 jobs to your HPC cluster (Slurm, PBS) or cloud compute (AWS Batch, Google Life Sciences)
- **CallVariantsWorker** → run GATK HaplotypeCaller or DeepVariant with your validated calling parameters and BED file panel definitions
- **AnnotateWorker** → query ClinVar, gnomAD, and OMIM via VEP or SnpEff with your institution's custom annotation sources
- **GenomicsReportWorker** → generate HL7v2 ORU messages or FHIR DiagnosticReport resources and deliver to the ordering provider's EHR
- Add a **QualityControlWorker** after sequencing to check coverage depth, duplication rate, and contamination before proceeding to alignment

Swap in your real bioinformatics tools (BWA, GATK, VEP) and LIMS reporting module while maintaining the same output fields, and the genomics workflow operates without modification.

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
genomics-pipeline/
├── pom.xml                          # Maven build (Java 21, conductor-client 5.0.1)
├── Dockerfile                       # Multi-stage build
├── docker-compose.yml               # Conductor + workers
├── run.sh                           # Smart launcher
├── src/main/resources/
│   └── workflow.json                # Workflow definition
├── src/main/java/genomicspipeline/
│   ├── ConductorClientHelper.java   # SDK v5 client setup
│   ├── GenomicsPipelineExample.java          # Main entry point (supports --workers mode)
│   └── workers/
│       ├── AlignWorker.java
│       ├── AnnotateWorker.java
│       ├── CallVariantsWorker.java
│       ├── GenomicsReportWorker.java
│       └── SequenceWorker.java
└── src/test/java/genomicspipeline/workers/
    ├── AlignWorkerTest.java        # 2 tests
    ├── AnnotateWorkerTest.java        # 2 tests
    ├── CallVariantsWorkerTest.java        # 2 tests
    ├── GenomicsReportWorkerTest.java        # 2 tests
    └── SequenceWorkerTest.java        # 2 tests

```
