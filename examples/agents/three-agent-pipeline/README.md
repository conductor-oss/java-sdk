# Three-Agent Pipeline: Researcher, Writer, Reviewer

A team needs a research-backed article reviewed for quality before publication. Having one person do all three tasks -- gather data, write prose, and critically review -- creates blind spots. The writer tends to trust their own sources, and self-review rarely catches structural weaknesses.

This pipeline separates concerns into three specialized agents, each passing structured data forward, with a final assembly worker that compiles the complete report including review metadata.

## Pipeline Architecture

```
subject, audience
       |
       v
thr_researcher_agent    (keyFacts, statistics map, sources)
       |
       v
thr_writer_agent        (draft text, wordCount)
       |
       v
thr_reviewer_agent      (score=8.5/10, verdict, suggestions)
       |
       v
thr_final_output        (finalReport assembly)
```

## Worker: ResearcherAgent (`thr_researcher_agent`)

Produces a `Map<String, Object>` containing `keyFacts` (3 items), `statistics` (a nested map with `marketGrowth: "15.3% CAGR through 2030"`, `regions: "North America leads adoption at 38% market share"`, and `enterprises: "72% of Fortune 500 companies have adopted related solutions"`), and `sources` (3 analyst report citations). Tags its output with `model: "researcher-agent-v1"`.

## Worker: WriterAgent (`thr_writer_agent`)

Receives the research map and extracts `subject`, `keyFacts`, and `statistics`. Pulls `marketGrowth` from the statistics map via `getOrDefault`. Constructs a prose paragraph incorporating the audience name, market growth figure, and each key fact. Computes `wordCount` via `split("\\s+").length`. Tags output as `model: "writer-agent-v1"`.

## Worker: ReviewerAgent (`thr_reviewer_agent`)

Evaluates the draft against the target audience. Returns a review map with `score: 8.5`, `maxScore: 10`, categorical assessments for `accuracy` ("High"), `clarity` ("Good"), and `audienceFit` ("Strong" -- interpolated with the audience name). Provides three concrete `suggestions` including adding a case study, year-over-year comparisons, and an executive summary. Verdict: `"APPROVED_WITH_MINOR_REVISIONS"`.

## Worker: FinalOutput (`thr_final_output`)

Assembles all agent outputs into a `finalReport` map using `Map.of()`. Extracts `sources` from the research map and `score`, `verdict`, `suggestions` from the review map. Includes `agentsPipeline: ["researcher-agent-v1", "writer-agent-v1", "reviewer-agent-v1"]` for traceability.

## Tests

4 tests cover the researcher, writer, reviewer, and final assembly stages.

## Further Reading

- [RUNNING.md](../../RUNNING.md) -- how to build and run this example
