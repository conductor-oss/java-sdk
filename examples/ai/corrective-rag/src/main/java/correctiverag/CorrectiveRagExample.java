package correctiverag;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import correctiverag.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 148: Corrective RAG — Detect and Fix Bad Retrievals
 *
 * Retrieve documents, grade their relevance, and if grades are too low,
 * fall back to web search for better context before generating the final answer.
 *
 * Pattern:
 *   retrieve -> grade_relevance -> SWITCH
 *     |-> "relevant"   -> generate_answer
 *     |-> default       -> web_search -> generate_from_web
 *
 * Run:
 *   java -jar target/corrective-rag-1.0.0.jar
 */
public class CorrectiveRagExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 148: Corrective RAG ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "cr_retrieve_docs", "cr_grade_relevance", "cr_generate_answer",
                "cr_web_search", "cr_generate_from_web"
        ));
        System.out.println("  Registered 5 tasks.\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'corrective_rag'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RetrieveDocsWorker(),
                new GradeRelevanceWorker(),
                new GenerateAnswerWorker(),
                new WebSearchWorker(),
                new GenerateFromWebWorker()
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
        String workflowId = client.startWorkflow("corrective_rag", 1,
                Map.of("question", "What is the pricing for Conductor?"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);

        System.out.println("\n  Question: " + workflow.getOutput().get("question"));
        System.out.println("  Relevance verdict: " + workflow.getOutput().get("verdict")
                + " (avg: " + workflow.getOutput().get("avgRelevance") + ")");
        System.out.println("  Action: Low relevance detected -> fell back to web search");

        // Find the answer from whichever branch ran
        var genTask = workflow.getTasks().stream()
                .filter(t -> "gen_web_ref".equals(t.getReferenceTaskName())
                        || "gen_ref".equals(t.getReferenceTaskName()))
                .findFirst();
        if (genTask.isPresent()) {
            System.out.println("  Source: " + genTask.get().getOutputData().get("source"));
            System.out.println("  Answer: " + genTask.get().getOutputData().get("answer"));
        }

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
