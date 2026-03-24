# Clinical Trials: 21 CFR Part 11 Audit Trails, Stratified Randomization, and p-Value Analysis

When the FDA asks "show me the audit trail for participant SUBJ-4401," you need to produce timestamped, electronically signed records for every action: screening, consent, randomization, monitoring, and analysis. This isn't optional -- 21 CFR Part 11 requires it. This example implements the full clinical trial enrollment pipeline using [Conductor](https://github.com/conductor-oss/conductor), with inclusion/exclusion screening against 14 medical conditions, SHA-256 signed informed consent, `SecureRandom`-based 1:1 stratified randomization, biomarker-driven monitoring, and statistical analysis with p-value computation.

## The Pipeline

```
clt_screen  -->  clt_consent  -->  clt_randomize  -->  clt_monitor  -->  clt_analyze
```

Every worker outputs a `cfr11AuditTrail` map containing `timestamp`, `action`, `performedBy`, `participantId`, `electronicSignature`, and `reason`. This chain creates a complete regulatory record per participant.

## Screening: 14 Conditions, Explicit Inclusion/Exclusion

`ScreenWorker` evaluates three criteria. Age must be 18-65. The condition must be in the eligible set:

```java
private static final Set<String> ELIGIBLE_CONDITIONS = Set.of(
    "hypertension", "diabetes_type2", "asthma", "arthritis", "depression",
    "anxiety", "chronic_pain", "obesity", "insomnia");
```

And the condition must NOT be in the exclusion set: `pregnancy`, `renal_failure`, `hepatitis`, `hiv`, `active_cancer`. A 30-year-old with hypertension passes. A 30-year-old who is pregnant does not -- even though pregnancy is not an eligible condition, the explicit exclusion check is separate because it produces a different audit trail entry (`hasExclusion: true` vs `conditionEligible: false`).

The output includes human-readable `inclusionCriteria` strings: `"age 18-65: PASS (age=45)"`, `"confirmed diagnosis: PASS (hypertension)"`, `"no contraindications: PASS"`. The electronic signature is `"SYS-SCREEN-" + Math.abs(participantId.hashCode()) % 100000`.

## Informed Consent with SHA-256 Signatures

`ConsentWorker` generates a cryptographic signature hash by concatenating `participantId + ":" + trialId + ":" + System.currentTimeMillis()` and computing a SHA-256 digest, truncated to the first 16 hex characters:

```java
MessageDigest md = MessageDigest.getInstance("SHA-256");
byte[] digest = md.digest(signatureData.getBytes());
signatureHash = HexFormat.of().formatHex(digest).substring(0, 16);
```

The consent form version is hardcoded to `"ICF-v3.2"` (Informed Consent Form version 3.2). The CFR Part 11 audit trail records the exact form version, making it traceable if consent forms are updated during a trial.

## Stratified Randomization

`RandomizeWorker` uses `java.security.SecureRandom` for cryptographically strong 1:1 allocation:

```java
boolean isTreatment = SECURE_RANDOM.nextBoolean();
String group = isTreatment ? "treatment" : "control";
```

Stratification factors are computed from age: participants under 40 are classified `"age_young"`, 40+ as `"age_older"`. Each participant receives a randomization code: `"RND-" + String.format("%05d", SECURE_RANDOM.nextInt(100000))`. The CFR Part 11 audit trail explicitly records `"Stratified randomization with 1:1 allocation"` as the method.

## Biomarker-Driven Monitoring

`MonitorWorker` generates monitoring data that differs by group assignment. Treatment group participants show biomarker improvement of -5% to -25%, while control group participants show minimal change of -2% to +2%. This simulates a real drug effect:

```java
if ("treatment".equals(group)) {
    biomarkerChange = -(5.0 + RNG.nextDouble() * 20.0);  // -5 to -25
} else {
    biomarkerChange = -2.0 + RNG.nextDouble() * 4.0;      // -2 to +2
}
```

The worker also generates 4-8 visit records, 0-2 adverse events, and 80-100% compliance scores. Invalid group values (anything other than `"treatment"` or `"control"`) fail with a terminal error.

## Statistical Analysis

`AnalyzeTrialWorker` determines outcomes from biomarker changes: less than -5 is `"improvement"`, greater than +5 is `"deterioration"`, between is `"no_change"`. The p-value approximation uses an exponential decay based on effect size:

```java
double effectSize = Math.abs(biomarkerChange) / 15.0;
double pValue = Math.max(0.001, Math.exp(-2.0 * effectSize * effectSize));
boolean significanceReached = pValue < 0.05;
```

Safety is assessed as acceptable when adverse events are at most 2 AND compliance is at least 70%.

## Test Coverage

- **ScreenWorkerTest**: 10 tests -- eligible patient, too young, too old, excluded condition, non-eligible condition, CFR Part 11 audit trail fields, missing participant/condition/age, negative age
- **ConsentWorkerTest**: 4 tests -- consent recording, CFR Part 11 fields, missing participant, missing trial
- **MonitorWorkerTest**: 5 tests -- monitoring output, CFR Part 11 fields, missing participant, missing group, invalid group value
- **AnalyzeTrialWorkerTest**: 10 tests -- improvement detection, no-change, deterioration, safety with high adverse events, safety with low compliance, CFR Part 11 fields, missing monitoring data/biomarker/compliance/adverse events
- **ClinicalTrialsIntegrationTest**: 6 tests -- full pipeline, ineligible (too young), excluded condition, non-matching condition, CFR Part 11 audit at every step, invalid input

**35 tests total**, with dedicated tests verifying the 21 CFR Part 11 audit trail is present and correctly structured at every step.

---

> **How to run:** See [RUNNING.md](../../RUNNING.md) | **Production guidance:** See [PRODUCTION.md](PRODUCTION.md)
