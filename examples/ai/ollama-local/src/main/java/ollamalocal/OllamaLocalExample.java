package ollamalocal;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ollamalocal.workers.OllamaCheckModelWorker;
import ollamalocal.workers.OllamaGenerateWorker;
import ollamalocal.workers.OllamaPostProcessWorker;

import java.util.List;
import java.util.Map;

/**
 * Ollama Local — Local LLM Code Review with Conductor
 *
 * Demonstrates a three-step workflow using a local Ollama instance:
 * 1. Check model availability
 * 2. Generate a code review using the model
 * 3. Post-process the generated response
 *
 * Run:
 *   java -jar target/ollama-local-1.0.0.jar
 *   java -jar target/ollama-local-1.0.0.jar --workers
 */
public class OllamaLocalExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Ollama Local: Code Review with Local LLM ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ollama_check_model",
                "ollama_generate",
                "ollama_post_process"
        ));
        System.out.println("  Registered: ollama_check_model, ollama_generate, ollama_post_process\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'ollama_local_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new OllamaCheckModelWorker(),
                new OllamaGenerateWorker(),
                new OllamaPostProcessWorker()
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
        String workflowId = client.startWorkflow("ollama_local_workflow", 1,
                Map.of(
                        "prompt", "Review this JavaScript function for potential issues and improvements",
                        "model", "codellama:13b"
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
