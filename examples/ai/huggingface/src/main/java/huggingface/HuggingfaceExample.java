package huggingface;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import huggingface.workers.HfFormatResultWorker;
import huggingface.workers.HfInferenceWorker;
import huggingface.workers.HfSelectModelWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 119: Orchestrating Hugging Face Inference API
 *
 * Demonstrates how to orchestrate Hugging Face Inference API calls through
 * Conductor. The worker calls HF's inference endpoint for text generation,
 * showing model selection, parameter configuration, and response parsing.
 *
 * Run:
 *   java -jar target/huggingface-1.0.0.jar
 */
public class HuggingfaceExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 119: Orchestrating Hugging Face Inference API ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("hf_select_model", "hf_inference", "hf_format_result"));
        System.out.println("  Registered: hf_select_model, hf_inference, hf_format_result\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'huggingface_inference_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new HfSelectModelWorker(),
                new HfInferenceWorker(),
                new HfFormatResultWorker()
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
        String workflowId = client.startWorkflow("huggingface_inference_workflow", 1,
                Map.of(
                        "text", "A new study published in the Journal of Organizational Behavior "
                                + "analyzed data from 2,500 remote workers across 12 countries over "
                                + "a 2-year period. The researchers found that hybrid work models "
                                + "where employees spent 3 days in the office and 2 days working "
                                + "remotely produced the highest productivity scores, employee "
                                + "satisfaction ratings, and retention rates. Fully remote workers "
                                + "reported 15% higher work-life balance satisfaction but showed 8% "
                                + "lower collaboration effectiveness scores. Fully in-office workers "
                                + "had the lowest overall satisfaction but the highest scores for "
                                + "spontaneous innovation. The study recommends that organizations "
                                + "adopt flexible hybrid models with clear guidelines for in-office "
                                + "collaboration days to maximize both productivity and employee "
                                + "well-being.",
                        "task", "summarization"
                ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Model: " + workflow.getOutput().get("model"));
        System.out.println("  Task: " + workflow.getOutput().get("task"));
        System.out.println("  Result: " + workflow.getOutput().get("result"));

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
