package passingoutput;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import passingoutput.workers.EnrichReportWorker;
import passingoutput.workers.GenerateReportWorker;
import passingoutput.workers.SummarizeReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Passing Output from One Task as Input to the Next
 *
 * Demonstrates Conductor's data wiring between tasks:
 * - ${taskRef.output.field} — simple field access
 * - ${taskRef.output.nested.field} — nested object access
 * - Entire objects and arrays passed between tasks
 *
 * Pipeline: generate_report -> enrich_report -> summarize_report
 *
 * Run:
 *   java -jar target/passing-output-to-input-1.0.0.jar
 */
public class PassingOutputExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 12: Passing Output to Input ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("generate_report", "enrich_report", "summarize_report"));
        System.out.println("  Registered: generate_report, enrich_report, summarize_report\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'data_wiring_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new GenerateReportWorker(),
                new EnrichReportWorker(),
                new SummarizeReportWorker()
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
        String workflowId = client.startWorkflow("data_wiring_demo", 1,
                Map.of("region", "US-West", "period", "Q1-2026"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Summary: " + workflow.getOutput().get("summary"));
        System.out.println("  Recommendation: " + workflow.getOutput().get("recommendation"));

        // Show the data flow
        System.out.println("\nData flow through tasks:");
        for (var t : workflow.getTasks()) {
            System.out.println("\n  " + t.getReferenceTaskName() + ":");
            System.out.println("    Input keys: " + String.join(", ", t.getInputData().keySet()));
            System.out.println("    Output keys: " + String.join(", ", t.getOutputData().keySet()));
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
