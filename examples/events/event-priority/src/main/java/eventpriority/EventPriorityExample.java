package eventpriority;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import eventpriority.workers.ClassifyPriorityWorker;
import eventpriority.workers.ProcessUrgentWorker;
import eventpriority.workers.ProcessNormalWorker;
import eventpriority.workers.ProcessBatchWorker;
import eventpriority.workers.RecordProcessingWorker;

import java.util.List;
import java.util.Map;

/**
 * Event Priority Workflow Demo
 *
 * Demonstrates a SWITCH-based event priority routing workflow:
 *   pr_classify_priority -> SWITCH(priority:
 *       high   -> pr_process_urgent,
 *       medium -> pr_process_normal,
 *       default -> pr_process_batch)
 *   -> pr_record_processing
 *
 * Run:
 *   java -jar target/event-priority-1.0.0.jar
 */
public class EventPriorityExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Event Priority Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "pr_classify_priority", "pr_process_urgent",
                "pr_process_normal", "pr_process_batch", "pr_record_processing"));
        System.out.println("  Registered: pr_classify_priority, pr_process_urgent, pr_process_normal, pr_process_batch, pr_record_processing\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'event_priority_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ClassifyPriorityWorker(),
                new ProcessUrgentWorker(),
                new ProcessNormalWorker(),
                new ProcessBatchWorker(),
                new RecordProcessingWorker()
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

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("event_priority_wf", 1,
                Map.of("eventId", "evt-fixed-001",
                        "eventType", "payment.failed"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
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
