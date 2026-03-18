package subworkflows;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import subworkflows.workers.CalcTotalWorker;
import subworkflows.workers.ValidatePaymentWorker;
import subworkflows.workers.ChargePaymentWorker;
import subworkflows.workers.ConfirmOrderWorker;

import java.util.List;
import java.util.Map;

/**
 * SUB_WORKFLOW — Break complex workflows into reusable modules.
 *
 * Parent workflow: sub_order_workflow — orchestrates order processing
 * Child workflow: sub_payment_process — handles payment (validate + charge)
 *
 * The parent calls the child via a SUB_WORKFLOW task type, demonstrating
 * how to compose workflows from smaller, reusable building blocks.
 *
 * Run:
 *   java -jar target/sub-workflows-1.0.0.jar
 */
public class SubWorkflowsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Sub-Workflow Pattern: Order Processing with Payment Sub-Workflow ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sub_calc_total", "sub_validate_payment",
                "sub_charge_payment", "sub_confirm_order"));
        System.out.println("  Registered: sub_calc_total, sub_validate_payment, sub_charge_payment, sub_confirm_order\n");

        // Step 2 — Register workflows (child first, then parent)
        System.out.println("Step 2: Registering workflows...");
        client.registerWorkflow("payment-workflow.json");
        System.out.println("  Registered child: sub_payment_process");
        client.registerWorkflow("workflow.json");
        System.out.println("  Registered parent: sub_order_workflow\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CalcTotalWorker(),
                new ValidatePaymentWorker(),
                new ChargePaymentWorker(),
                new ConfirmOrderWorker()
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
        String workflowId = client.startWorkflow("sub_order_workflow", 1,
                Map.of(
                        "orderId", "ORD-001",
                        "items", List.of(
                                Map.of("name", "Widget", "price", 10.0, "qty", 2),
                                Map.of("name", "Gadget", "price", 25.0, "qty", 1),
                                Map.of("name", "Doohickey", "price", 5.0, "qty", 4)
                        ),
                        "paymentMethod", "credit_card"
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
