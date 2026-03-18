package exactlyonce;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import exactlyonce.workers.ExoLockWorker;
import exactlyonce.workers.ExoCheckStateWorker;
import exactlyonce.workers.ExoProcessWorker;
import exactlyonce.workers.ExoCommitWorker;
import exactlyonce.workers.ExoUnlockWorker;

import java.util.List;
import java.util.Map;

/**
 * Exactly-Once Processing Demo
 *
 * Run:
 *   java -jar target/exactlyonce-1.0.0.jar
 */
public class ExactlyOnceExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Exactly-Once Processing Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "exo_lock",
                "exo_check_state",
                "exo_process",
                "exo_commit",
                "exo_unlock"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'exo_exactly_once'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ExoLockWorker(),
                new ExoCheckStateWorker(),
                new ExoProcessWorker(),
                new ExoCommitWorker(),
                new ExoUnlockWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("exo_exactly_once", 1,
                Map.of("messageId", "TXN-2024-5678", "payload", "debit_49.99", "resourceKey", "account:ACC-001"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

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