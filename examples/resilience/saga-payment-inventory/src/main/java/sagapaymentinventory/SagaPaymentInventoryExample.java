package sagapaymentinventory;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import sagapaymentinventory.workers.*;

import java.util.List;
import java.util.Map;

/**
 * E-commerce Order Saga — reserve inventory, charge payment, ship order.
 *
 * If shipping fails, the saga compensates by refunding the payment and
 * releasing the reserved inventory. A SWITCH task inspects the ship worker's
 * output to decide whether to run the compensation branch.
 *
 * Run:
 *   java -jar target/saga-payment-inventory-1.0.0.jar
 *   java -jar target/saga-payment-inventory-1.0.0.jar --workers
 */
public class SagaPaymentInventoryExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== E-commerce Order Saga Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        List<TaskDef> taskDefs = List.of(
                makeTaskDef("spi_reserve_inventory"),
                makeTaskDef("spi_charge_payment"),
                makeTaskDef("spi_ship_order"),
                makeTaskDef("spi_refund_payment"),
                makeTaskDef("spi_release_inventory")
        );
        client.registerTaskDefs(taskDefs);

        System.out.println("  Registered 5 task definitions.\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'saga_payment_inventory'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ReserveInventoryWorker(),
                new ChargePaymentWorker(),
                new ShipOrderWorker(),
                new RefundPaymentWorker(),
                new ReleaseInventoryWorker()
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

        // Step 4a — Start workflow: happy path (shipping succeeds)
        System.out.println("Step 4a: Starting workflow (shouldFail=false — happy path)...\n");
        String happyId = client.startWorkflow("saga_payment_inventory", 1,
                Map.of("orderId", "ORD-100", "amount", 99.99, "shouldFail", false));
        System.out.println("  Workflow ID: " + happyId + "\n");

        System.out.println("  Waiting for completion...");
        Workflow happyResult = client.waitForWorkflow(happyId, "COMPLETED", 30000);
        System.out.println("  Status: " + happyResult.getStatus().name());
        System.out.println("  Output: " + happyResult.getOutput() + "\n");

        // Step 4b — Start workflow: failure path (shipping fails, compensation runs)
        System.out.println("Step 4b: Starting workflow (shouldFail=true — compensation path)...\n");
        String failId = client.startWorkflow("saga_payment_inventory", 1,
                Map.of("orderId", "ORD-200", "amount", 49.99, "shouldFail", true));
        System.out.println("  Workflow ID: " + failId + "\n");

        System.out.println("  Waiting for completion...");
        Workflow failResult = client.waitForWorkflow(failId, "COMPLETED", 30000);
        System.out.println("  Status: " + failResult.getStatus().name());
        System.out.println("  Output: " + failResult.getOutput() + "\n");

        client.stopWorkers();

        boolean allPassed = "COMPLETED".equals(happyResult.getStatus().name())
                && "COMPLETED".equals(failResult.getStatus().name());

        if (allPassed) {
            System.out.println("Result: PASSED — both happy path and compensation path completed.");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }

    private static TaskDef makeTaskDef(String name) {
        TaskDef td = new TaskDef();
        td.setName(name);
        td.setRetryCount(0);
        td.setTimeoutSeconds(60);
        td.setResponseTimeoutSeconds(30);
        td.setOwnerEmail("examples@orkes.io");
        return td;
    }
}
