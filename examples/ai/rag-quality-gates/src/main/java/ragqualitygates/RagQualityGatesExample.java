package ragqualitygates;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragqualitygates.workers.RetrieveWorker;
import ragqualitygates.workers.CheckRelevanceWorker;
import ragqualitygates.workers.GenerateWorker;
import ragqualitygates.workers.CheckFaithfulnessWorker;
import ragqualitygates.workers.RejectWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 157: RAG Quality Gates
 *
 * Implements a RAG pipeline with quality gates for relevance and
 * faithfulness. Retrieved documents are checked for relevance before
 * generation, and generated answers are checked for faithfulness
 * against source documents. Failed gates route to a rejection worker.
 *
 * Run:
 *   java -jar target/rag-quality-gates-1.0.0.jar
 *   java -jar target/rag-quality-gates-1.0.0.jar --workers
 */
public class RagQualityGatesExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 157: RAG Quality Gates ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "qg_retrieve", "qg_check_relevance",
                "qg_generate", "qg_check_faithfulness", "qg_reject"));
        System.out.println("  Registered: qg_retrieve, qg_check_relevance, qg_generate, qg_check_faithfulness, qg_reject\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'rag_quality_gates_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RetrieveWorker(),
                new CheckRelevanceWorker(),
                new GenerateWorker(),
                new CheckFaithfulnessWorker(),
                new RejectWorker()
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

        // Step 4 — Start workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("rag_quality_gates_workflow", 1,
                Map.of("question", "How does Conductor orchestrate microservices?"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("  Waiting for completion...");
        Workflow wf = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Answer: " + wf.getOutput().get("answer"));
        System.out.println("  Relevance Score: " + wf.getOutput().get("relevanceScore"));
        System.out.println("  Faithfulness Score: " + wf.getOutput().get("faithfulnessScore"));

        System.out.println("\n--- RAG Quality Gates Pattern ---");
        System.out.println("  - Retrieve: Fetch relevant documents");
        System.out.println("  - Relevance gate: Check document relevance against threshold");
        System.out.println("  - Generate: Produce answer from question + documents");
        System.out.println("  - Faithfulness gate: Check answer faithfulness against sources");
        System.out.println("  - Reject: Handle failed quality gates with reason");

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
