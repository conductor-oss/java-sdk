package enterpriserag;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import enterpriserag.workers.CheckCacheWorker;
import enterpriserag.workers.RateLimitWorker;
import enterpriserag.workers.RetrieveWorker;
import enterpriserag.workers.TokenBudgetWorker;
import enterpriserag.workers.GenerateWorker;
import enterpriserag.workers.CacheResultWorker;
import enterpriserag.workers.AuditLogWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 164: Enterprise RAG
 *
 * Enterprise-grade RAG pipeline with cache checking, rate limiting,
 * token budget management, result caching, and SOC2-compliant audit logging.
 * Uses a SWITCH pattern to serve cached results or generate new answers.
 *
 * Run:
 *   java -jar target/enterprise-rag-1.0.0.jar
 *   java -jar target/enterprise-rag-1.0.0.jar --workers
 */
public class EnterpriseRagExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 164: Enterprise RAG ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "er_check_cache", "er_rate_limit", "er_retrieve",
                "er_token_budget", "er_generate", "er_cache_result", "er_audit_log"));
        System.out.println("  Registered: er_check_cache, er_rate_limit, er_retrieve, er_token_budget, er_generate, er_cache_result, er_audit_log\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'enterprise_rag'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CheckCacheWorker(),
                new RateLimitWorker(),
                new RetrieveWorker(),
                new TokenBudgetWorker(),
                new GenerateWorker(),
                new CacheResultWorker(),
                new AuditLogWorker()
        );
        client.startWorkers(workers);
        System.out.println("  7 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("enterprise_rag", 1, Map.of(
                "question", "What is retrieval-augmented generation?",
                "userId", "user-42",
                "sessionId", "sess-abc-123"
        ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Answer: " + workflow.getOutput().get("answer"));
        System.out.println("  Source: " + workflow.getOutput().get("source"));
        System.out.println("  Tokens used: " + workflow.getOutput().get("tokensUsed"));
        System.out.println("  Model: " + workflow.getOutput().get("model"));

        System.out.println("\n--- Enterprise RAG Pattern ---");
        System.out.println("  - Check cache: Look up previous answers");
        System.out.println("  - SWITCH: Route based on cache hit/miss");
        System.out.println("  - Rate limit: Enforce per-user request limits");
        System.out.println("  - Retrieve: Fetch relevant context documents");
        System.out.println("  - Token budget: Trim context to stay within daily limits");
        System.out.println("  - Generate: Produce answer with LLM");
        System.out.println("  - Cache result: Store answer for future requests");
        System.out.println("  - Audit log: SOC2-compliant logging of all queries");

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
