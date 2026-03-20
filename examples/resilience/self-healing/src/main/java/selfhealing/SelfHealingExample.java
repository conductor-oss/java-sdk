package selfhealing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import selfhealing.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Self-Healing Workflows — automatically detect, diagnose, and fix service issues
 *
 * Demonstrates a workflow that:
 * 1. Runs a health check on a service
 * 2. If healthy (default): processes data normally
 * 3. If unhealthy: diagnoses the problem, remediates it, then retries processing
 *
 * The SWITCH task uses value-param evaluation on the health check's "healthy"
 * output (String "true"/"false") to branch the workflow.
 *
 * Run:
 *   java -jar target/self-healing-1.0.0.jar
 *   java -jar target/self-healing-1.0.0.jar --workers
 */
public class SelfHealingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Self-Healing Workflows Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        List<TaskDef> taskDefs = List.of(
                makeTaskDef("sh_health_check"),
                makeTaskDef("sh_process"),
                makeTaskDef("sh_diagnose"),
                makeTaskDef("sh_remediate"),
                makeTaskDef("sh_retry_process")
        );
        client.registerTaskDefs(taskDefs);
        System.out.println("  Registered 5 task definitions.\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'self_healing_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new HealthCheckWorker(),
                new ProcessWorker(),
                new DiagnoseWorker(),
                new RemediateWorker(),
                new RetryProcessWorker()
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

        // Step 4 — Test with a healthy service
        System.out.println("Step 4: Starting workflow with healthy service...\n");
        String healthyId = client.startWorkflow("self_healing_demo", 1,
                Map.of("service", "healthy-service", "data", "payload-1"));
        System.out.println("  Workflow ID: " + healthyId);

        Workflow healthyResult = client.waitForWorkflow(healthyId, "COMPLETED", 30000);
        System.out.println("  Status: " + healthyResult.getStatus().name());
        System.out.println("  Output: " + healthyResult.getOutput() + "\n");

        // Step 5 — Test with a broken service (triggers self-healing)
        System.out.println("Step 5: Starting workflow with broken service (triggers self-healing)...\n");
        String brokenId = client.startWorkflow("self_healing_demo", 1,
                Map.of("service", "broken-service", "data", "payload-2"));
        System.out.println("  Workflow ID: " + brokenId);

        Workflow brokenResult = client.waitForWorkflow(brokenId, "COMPLETED", 30000);
        System.out.println("  Status: " + brokenResult.getStatus().name());
        System.out.println("  Output: " + brokenResult.getOutput() + "\n");

        client.stopWorkers();

        boolean passed = "COMPLETED".equals(healthyResult.getStatus().name())
                && "COMPLETED".equals(brokenResult.getStatus().name());

        if (passed) {
            System.out.println("Result: PASSED");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }

    private static TaskDef makeTaskDef(String name) {
        TaskDef def = new TaskDef();
        def.setName(name);
        def.setRetryCount(0);
        def.setTimeoutSeconds(60);
        def.setResponseTimeoutSeconds(30);
        def.setOwnerEmail("examples@orkes.io");
        return def;
    }
}
