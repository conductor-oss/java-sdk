package workflowinputoutput;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import workflowinputoutput.workers.ApplyDiscountWorker;
import workflowinputoutput.workers.CalculateTaxWorker;
import workflowinputoutput.workers.FormatInvoiceWorker;
import workflowinputoutput.workers.LookupPriceWorker;

import java.util.List;
import java.util.Map;

/**
 * Price Calculator — Data Flow with Conductor
 *
 * Demonstrates how data flows between tasks using JSONPath expressions.
 * Each task's output becomes the next task's input via ${taskRef.output.field} wiring.
 *
 * Pipeline: lookup_price → apply_discount → calculate_tax → format_invoice
 *
 * Run:
 *   java -jar target/workflow-input-output-1.0.0.jar
 */
public class PriceCalculatorExample {

    private static final List<String> TASK_NAMES = List.of(
            "lookup_price", "apply_discount", "calculate_tax", "format_invoice");

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Price Calculator: Data Flow Between Workflow Tasks ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(TASK_NAMES);
        System.out.println("  Registered: " + String.join(", ", TASK_NAMES) + "\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'price_calculator'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new LookupPriceWorker(),
                new ApplyDiscountWorker(),
                new CalculateTaxWorker(),
                new FormatInvoiceWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow with sample input...");
        Map<String, Object> input = Map.of(
                "productId", "PROD-002",
                "quantity", 2,
                "couponCode", "SAVE20");
        System.out.println("  Input: " + input + "\n");
        String workflowId = client.startWorkflow("price_calculator", 1, input);
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n");

        // Print invoice from output
        Object invoice = workflow.getOutput().get("invoice");
        if (invoice != null) {
            System.out.println(invoice);
            System.out.println();
        }

        System.out.println("  Full output: " + workflow.getOutput());

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
