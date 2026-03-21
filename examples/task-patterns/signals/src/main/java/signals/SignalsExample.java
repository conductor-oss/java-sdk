package signals;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.run.Workflow;
import signals.workers.SigPrepareWorker;
import signals.workers.SigProcessShippingWorker;
import signals.workers.SigCompleteWorker;

import java.util.List;
import java.util.Map;

/**
 * Signals — Send data to running workflows via WAIT task completion.
 *
 * Demonstrates how external systems can signal a running workflow by
 * completing WAIT tasks with data via the Task API. The workflow pauses
 * at each WAIT task until an external signal arrives with the required data.
 *
 * Workflow: sig_prepare -> WAIT(shipping) -> sig_process_shipping -> WAIT(delivery) -> sig_complete
 *
 * Run:
 *   java -jar target/signals-1.0.0.jar
 */
public class SignalsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Signals: Send Data to Running Workflows via WAIT Task Completion ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("sig_prepare", "sig_process_shipping", "sig_complete"));
        System.out.println("  Registered: sig_prepare, sig_process_shipping, sig_complete\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'signal_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SigPrepareWorker(),
                new SigProcessShippingWorker(),
                new SigCompleteWorker()
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
        String workflowId = client.startWorkflow("signal_demo", 1,
                Map.of("orderId", "ORD-12345"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for workflow to reach the first WAIT task (shipping)
        System.out.println("Step 5: Waiting for workflow to reach WAIT(shipping)...");
        Thread.sleep(3000);

        // Find the IN_PROGRESS WAIT task for shipping and complete it with signal data
        System.out.println("  Sending shipping signal...");
        Workflow workflow = client.getWorkflow(workflowId);
        var shippingTask = workflow.getTasks().stream()
                .filter(t -> "wait_shipping_ref".equals(t.getReferenceTaskName()))
                .filter(t -> t.getStatus() == com.netflix.conductor.common.metadata.tasks.Task.Status.IN_PROGRESS)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("WAIT(shipping) task not found in IN_PROGRESS state"));

        TaskResult shippingResult = new TaskResult();
        shippingResult.setWorkflowInstanceId(workflowId);
        shippingResult.setTaskId(shippingTask.getTaskId());
        shippingResult.setStatus(TaskResult.Status.COMPLETED);
        shippingResult.getOutputData().put("trackingNumber", "TRK-98321");
        shippingResult.getOutputData().put("carrier", "FedEx");
        client.getTaskClient().updateTask(shippingResult);
        System.out.println("  Shipping signal sent: tracking=TRK-98321, carrier=FedEx\n");

        // Step 6 — Wait for workflow to reach the second WAIT task (delivery)
        System.out.println("Step 6: Waiting for workflow to reach WAIT(delivery)...");
        Thread.sleep(3000);

        // Find the IN_PROGRESS WAIT task for delivery and complete it with signal data
        System.out.println("  Sending delivery signal...");
        workflow = client.getWorkflow(workflowId);
        var deliveryTask = workflow.getTasks().stream()
                .filter(t -> "wait_delivery_ref".equals(t.getReferenceTaskName()))
                .filter(t -> t.getStatus() == com.netflix.conductor.common.metadata.tasks.Task.Status.IN_PROGRESS)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("WAIT(delivery) task not found in IN_PROGRESS state"));

        TaskResult deliveryResult = new TaskResult();
        deliveryResult.setWorkflowInstanceId(workflowId);
        deliveryResult.setTaskId(deliveryTask.getTaskId());
        deliveryResult.setStatus(TaskResult.Status.COMPLETED);
        deliveryResult.getOutputData().put("deliveredAt", "2026-03-14T10:30:00Z");
        deliveryResult.getOutputData().put("signature", "J. Smith");
        client.getTaskClient().updateTask(deliveryResult);
        System.out.println("  Delivery signal sent: deliveredAt=2026-03-14T10:30:00Z, signature=J. Smith\n");

        // Step 7 — Wait for completion
        System.out.println("Step 7: Waiting for completion...");
        workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
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
