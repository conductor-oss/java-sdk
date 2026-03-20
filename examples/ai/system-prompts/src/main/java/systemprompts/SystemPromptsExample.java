package systemprompts;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import systemprompts.workers.SpBuildPromptWorker;
import systemprompts.workers.SpCallLlmWorker;
import systemprompts.workers.SpCompareOutputsWorker;

import java.util.List;
import java.util.Map;

/**
 * System Prompts — Demonstrates how different system prompts
 * change LLM output tone while preserving factual content.
 *
 * The workflow runs the same user prompt through two styles (formal, casual)
 * sequentially, then compares the outputs.
 *
 * Run:
 *   java -jar target/system-prompts-1.0.0.jar
 *   java -jar target/system-prompts-1.0.0.jar --workers
 */
public class SystemPromptsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== System Prompts: Formal vs Casual LLM Responses ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("sp_build_prompt", "sp_call_llm", "sp_compare_outputs"));
        System.out.println("  Registered: sp_build_prompt, sp_call_llm, sp_compare_outputs\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'system_prompts_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SpBuildPromptWorker(),
                new SpCallLlmWorker(),
                new SpCompareOutputsWorker()
        );
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("system_prompts_workflow", 1,
                Map.of("userPrompt", "Explain Conductor", "model", "gpt-4"));
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
