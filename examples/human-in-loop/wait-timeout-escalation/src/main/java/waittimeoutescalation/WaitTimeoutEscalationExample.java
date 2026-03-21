package waittimeoutescalation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import waittimeoutescalation.workers.WteEscalateWorker;
import waittimeoutescalation.workers.WtePrepareWorker;
import waittimeoutescalation.workers.WteProcessWorker;

import java.util.List;
import java.util.Map;

/**
 * WAIT with Timeout and Escalation Demo
 *
 * Demonstrates a human-in-the-loop pattern where:
 * 1. A prepare task readies the system
 * 2. A WAIT task pauses for human input
 * 3. If the wait times out, an escalation workflow is triggered
 * 4. If the wait completes, the response is processed
 *
 * Run:
 *   java -jar target/wait-timeout-escalation-1.0.0.jar
 *   java -jar target/wait-timeout-escalation-1.0.0.jar --workers
 */
public class WaitTimeoutEscalationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== WAIT with Timeout and Escalation Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef prepareDef = new TaskDef();
        prepareDef.setName("wte_prepare");
        prepareDef.setTimeoutSeconds(60);
        prepareDef.setResponseTimeoutSeconds(30);
        prepareDef.setOwnerEmail("examples@orkes.io");

        TaskDef processDef = new TaskDef();
        processDef.setName("wte_process");
        processDef.setTimeoutSeconds(60);
        processDef.setResponseTimeoutSeconds(30);
        processDef.setOwnerEmail("examples@orkes.io");

        TaskDef escalateDef = new TaskDef();
        escalateDef.setName("wte_escalate");
        escalateDef.setTimeoutSeconds(60);
        escalateDef.setResponseTimeoutSeconds(30);
        escalateDef.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(prepareDef, processDef, escalateDef));

        System.out.println("  Registered: wte_prepare, wte_process, wte_escalate\n");

        // Step 2 -- Register workflows
        System.out.println("Step 2: Registering workflows...");
        client.registerWorkflow("workflow.json");
        client.registerWorkflow("escalation-workflow.json");
        System.out.println("  Main workflow and escalation workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new WtePrepareWorker(),
                new WteProcessWorker(),
                new WteEscalateWorker()
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

        // Step 4 -- Start the main workflow
        System.out.println("Step 4: Starting main workflow...\n");
        String workflowId = client.startWorkflow("wait_timeout_escalation_demo", 1,
                Map.of("requestId", "req-001"));
        System.out.println("  Workflow ID: " + workflowId);
        System.out.println("  Workflow is now waiting for human input (WAIT task).\n");

        // Step 5 -- Also start the escalation workflow to demonstrate it
        System.out.println("Step 5: Starting escalation workflow (performing timeout)...\n");
        String escalationId = client.startWorkflow("wait_timeout_escalation_escalate", 1,
                Map.of("requestId", "req-001", "reason", "WAIT task timed out"));
        System.out.println("  Escalation Workflow ID: " + escalationId + "\n");

        // Step 6 -- Wait for escalation workflow to complete
        System.out.println("Step 6: Waiting for escalation workflow...");
        Workflow escalation = client.waitForWorkflow(escalationId, "COMPLETED", 30000);
        String escalationStatus = escalation.getStatus().name();
        System.out.println("  Escalation Status: " + escalationStatus);
        System.out.println("  Escalation Output: " + escalation.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(escalationStatus)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
