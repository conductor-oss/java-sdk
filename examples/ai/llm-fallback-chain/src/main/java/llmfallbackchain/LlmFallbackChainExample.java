package llmfallbackchain;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import llmfallbackchain.workers.FbCallClaudeWorker;
import llmfallbackchain.workers.FbCallGeminiWorker;
import llmfallbackchain.workers.FbCallGpt4Worker;
import llmfallbackchain.workers.FbFormatResultWorker;

import java.util.List;
import java.util.Map;

/**
 * LLM Fallback Chain — Demonstrates multi-model fallback with SWITCH routing:
 * GPT-4 fails -> Claude fails -> Gemini succeeds
 *
 * Run:
 *   java -jar target/llm-fallback-chain-1.0.0.jar
 */
public class LlmFallbackChainExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== LLM Fallback Chain: Multi-Model Fallback ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "fb_call_gpt4", "fb_call_claude", "fb_call_gemini", "fb_format_result"));
        System.out.println("  Registered: fb_call_gpt4, fb_call_claude, fb_call_gemini, fb_format_result\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'llm_fallback_chain_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new FbCallGpt4Worker(),
                new FbCallClaudeWorker(),
                new FbCallGeminiWorker(),
                new FbFormatResultWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("llm_fallback_chain_workflow", 1, Map.of(
                "prompt", "Explain how Conductor handles workflow orchestration"
        ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

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
