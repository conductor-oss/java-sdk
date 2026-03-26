package llmcaching;

import com.netflix.conductor.client.worker.Worker;

import java.util.List;
import java.util.Map;

/**
 * Main entry point for the LLM Caching example.
 * Supports --workers mode to run workers that poll the Conductor server.
 */
public class LlmCachingExample {

    public static void main(String[] args) throws Exception {
        ConductorClientHelper helper = new ConductorClientHelper();

        List<Worker> workers = List.of(
                new CacheHashPromptWorker(),
                new CacheLlmCallWorker(),
                new CacheReportWorker()
        );

        if (args.length > 0 && "--workers".equals(args[0])) {
            System.out.println("Starting LLM Caching workers...");
            helper.startWorkers(workers);
            System.out.println("Workers running. Press Ctrl+C to stop.");
            Thread.currentThread().join();
        } else {
            // Register and run workflow
            helper.registerTaskDefs(List.of(
                    "cache_hash_prompt", "cache_llm_call", "cache_report"));
            helper.registerWorkflow("workflow.json");

            helper.startWorkers(workers);

            Map<String, Object> input = Map.of(
                    "prompt", "What is Conductor?",
                    "model", "gpt-4"
            );

            String workflowId = helper.startWorkflow("llm_caching", 1, input);
            System.out.println("Started workflow: " + workflowId);

            var workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.println("Workflow status: " + workflow.getStatus());
            System.out.println("Output: " + workflow.getOutput());

            helper.stopWorkers();
        }
    }
}
