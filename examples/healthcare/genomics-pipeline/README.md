# Genomics Pipeline in Java Using Conductor : Sequencing, Alignment, Variant Calling, Annotation, and Clinical Reporting

## The Problem

You need to process genomic samples for clinical diagnostics. A DNA sample arrives from the lab and must be sequenced to produce raw reads (FASTQ). Those reads must be aligned to a reference genome (GRCh38) to produce a BAM file. Variant calling identifies SNPs, indels, and structural variants in the aligned data (VCF). Each variant must be annotated with clinical significance from databases like ClinVar, OMIM, and gnomAD. Finally, a clinical report must be generated summarizing pathogenic and likely pathogenic findings for the ordering physician. Each step produces large intermediate files and depends strictly on the previous step's output. you cannot call variants without aligned reads, and you cannot annotate without called variants.

Without orchestration, you'd chain bioinformatics tools (BWA, GATK, VEP) together in a shell script or Nextflow pipeline, handling failures with ad-hoc retry logic. If the variant calling step fails halfway through a multi-hour run, you'd need to figure out where to restart. If the annotation database is temporarily unavailable, the entire pipeline stalls. CAP/CLIA regulations require a complete chain of custody from sample receipt to report delivery for every clinical genomic test.

## The Solution

**You just write the genomics workers. Sequencing, read alignment, variant calling, clinical annotation, and diagnostic reporting. Conductor handles stage dependencies, automatic retries when a bioinformatics service fails mid-run, and a CAP/CLIA-compliant chain of custody from sample to report.**

Each stage of the genomics pipeline is a simple, independent worker. a plain Java class that does one thing. Conductor takes care of running sequencing before alignment, calling variants only after alignment completes, annotating only after variants are called, retrying if any bioinformatics service is temporarily unavailable, and maintaining a CAP/CLIA-compliant audit trail from sample to report. ### What You Write: Workers

Five workers cover the genomics pipeline: SequenceWorker produces raw reads, AlignWorker maps to the reference genome, CallVariantsWorker identifies genetic variants, AnnotateWorker adds clinical significance, and GenomicsReportWorker generates the diagnostic report.

| Worker | Task | What It Does |
|---|---|---|
| **SequenceWorker** | `gen_sequence` | Processes the DNA sample and produces raw sequencing reads (FASTQ) for the specified panel type |
| **AlignWorker** | `gen_align` | Aligns raw reads to the reference genome (GRCh38) and produces a sorted, indexed BAM file |
| **CallVariantsWorker** | `gen_call_variants` | Identifies SNPs, indels, and structural variants from aligned reads and produces a VCF file |
| **AnnotateWorker** | `gen_annotate` | Annotates each variant with clinical significance from ClinVar, OMIM, gnomAD, and functional predictions |
| **GenomicsReportWorker** | `gen_report` | Generates the clinical genomics report with pathogenic findings, gene coverage, and interpretation |

the workflow and compliance logic stay the same.

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

---

> **How to run this example:** See [RUNNING.md](../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
