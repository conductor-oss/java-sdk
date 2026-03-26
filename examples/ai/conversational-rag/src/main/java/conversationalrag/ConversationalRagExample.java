package conversationalrag;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import conversationalrag.workers.LoadHistoryWorker;
import conversationalrag.workers.EmbedWithContextWorker;
import conversationalrag.workers.RetrieveWorker;
import conversationalrag.workers.GenerateWorker;
import conversationalrag.workers.SaveHistoryWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 144: Conversational RAG — Chat with Memory and Context
 *
 * Maintains conversation history across turns, retrieves context
 * per turn, and generates contextually-aware responses. Workers
 * perform memory management and context-enhanced generation.
 *
 * Run:
 *   java -jar target/conversational-rag-1.0.0.jar
 *   java -jar target/conversational-rag-1.0.0.jar --workers
 */
public class ConversationalRagExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 144: Conversational RAG ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "crag_load_history", "crag_embed_with_context",
                "crag_retrieve", "crag_generate", "crag_save_history"));
        System.out.println("  Registered: crag_load_history, crag_embed_with_context, crag_retrieve, crag_generate, crag_save_history\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'conversational_rag_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LoadHistoryWorker(),
                new EmbedWithContextWorker(),
                new RetrieveWorker(),
                new GenerateWorker(),
                new SaveHistoryWorker()
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

        // Step 4 — Turn 1
        System.out.println("Step 4: Starting workflow — Turn 1...\n");
        String workflowId1 = client.startWorkflow("conversational_rag_workflow", 1,
                Map.of("sessionId", "session-abc123",
                        "userMessage", "What features does Conductor offer?"));
        System.out.println("  Workflow ID: " + workflowId1 + "\n");

        System.out.println("  Waiting for Turn 1 completion...");
        Workflow wf1 = client.waitForWorkflow(workflowId1, "COMPLETED", 30000);
        System.out.println("  Status: " + wf1.getStatus().name());
        System.out.println("  Turn " + wf1.getOutput().get("turnNumber") + ": " + wf1.getOutput().get("response") + "\n");

        // Step 5 — Turn 2 (follow-up)
        System.out.println("Step 5: Starting workflow — Turn 2 (follow-up)...\n");
        String workflowId2 = client.startWorkflow("conversational_rag_workflow", 1,
                Map.of("sessionId", "session-abc123",
                        "userMessage", "Can you tell me more about worker languages?"));
        System.out.println("  Workflow ID: " + workflowId2 + "\n");

        System.out.println("  Waiting for Turn 2 completion...");
        Workflow wf2 = client.waitForWorkflow(workflowId2, "COMPLETED", 30000);
        String status = wf2.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Turn " + wf2.getOutput().get("turnNumber") + ": " + wf2.getOutput().get("response"));

        System.out.println("\n--- Conversational RAG Pattern ---");
        System.out.println("  - Load history: Retrieve prior turns for context");
        System.out.println("  - Contextual embedding: Combine history with current query");
        System.out.println("  - Generate with memory: LLM sees full conversation + retrieved docs");

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
