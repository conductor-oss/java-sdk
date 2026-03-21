package ragevaluation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragevaluation.workers.RunRagWorker;
import ragevaluation.workers.EvalFaithfulnessWorker;
import ragevaluation.workers.EvalRelevanceWorker;
import ragevaluation.workers.EvalCoherenceWorker;
import ragevaluation.workers.AggregateScoresWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 162: RAG Evaluation Pipeline
 *
 * Runs a RAG pipeline, then evaluates the answer for faithfulness, relevance,
 * and coherence in parallel using a FORK/JOIN pattern, and aggregates the scores
 * into an overall verdict.
 *
 * Run:
 *   java -jar target/rag-evaluation-1.0.0.jar
 *   java -jar target/rag-evaluation-1.0.0.jar --workers
 */
public class RagEvaluationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 162: RAG Evaluation Pipeline ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "re_run_rag", "re_eval_faithfulness", "re_eval_relevance",
                "re_eval_coherence", "re_aggregate_scores"));
        System.out.println("  Registered: re_run_rag, re_eval_faithfulness, re_eval_relevance, re_eval_coherence, re_aggregate_scores\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'rag_evaluation_pipeline'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RunRagWorker(),
                new EvalFaithfulnessWorker(),
                new EvalRelevanceWorker(),
                new EvalCoherenceWorker(),
                new AggregateScoresWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("rag_evaluation_pipeline", 1, Map.of(
                "question", "How do RAG pipelines work?"
        ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Overall score: " + workflow.getOutput().get("overallScore"));
        System.out.println("  Verdict: " + workflow.getOutput().get("verdict"));
        System.out.println("  Breakdown: " + workflow.getOutput().get("breakdown"));

        System.out.println("\n--- RAG Evaluation Pipeline Pattern ---");
        System.out.println("  - Run RAG: Execute retrieval and generation");
        System.out.println("  - FORK: Evaluate faithfulness, relevance, and coherence in parallel");
        System.out.println("  - JOIN: Collect all evaluation results");
        System.out.println("  - Aggregate: Compute overall score and verdict");

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
