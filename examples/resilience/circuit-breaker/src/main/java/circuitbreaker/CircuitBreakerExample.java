package circuitbreaker;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import circuitbreaker.workers.CallServiceWorker;
import circuitbreaker.workers.CheckCircuitWorker;
import circuitbreaker.workers.FallbackWorker;

import java.util.List;
import java.util.Map;

/**
 * Circuit Breaker Pattern -- Stop Calling Failed Services
 *
 * Demonstrates the circuit breaker pattern using Conductor:
 *   CLOSED  -- normal operation, service calls go through
 *   OPEN    -- service is failing, use fallback data
 *   HALF_OPEN -- testing if service has recovered
 *
 * Workflow: check_circuit -> SWITCH on state:
 *   OPEN -> fallback (cached data)
 *   default -> call_service (live call)
 *
 * Run:
 *   java -jar target/circuit-breaker-1.0.0.jar
 *   java -jar target/circuit-breaker-1.0.0.jar --workers
 */
public class CircuitBreakerExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Circuit Breaker Pattern: Stop Calling Failed Services ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef checkCircuitTask = new TaskDef();
        checkCircuitTask.setName("cb_check_circuit");
        checkCircuitTask.setTimeoutSeconds(60);
        checkCircuitTask.setResponseTimeoutSeconds(30);
        checkCircuitTask.setOwnerEmail("examples@orkes.io");

        TaskDef callServiceTask = new TaskDef();
        callServiceTask.setName("cb_call_service");
        callServiceTask.setTimeoutSeconds(60);
        callServiceTask.setResponseTimeoutSeconds(30);
        callServiceTask.setOwnerEmail("examples@orkes.io");

        TaskDef fallbackTask = new TaskDef();
        fallbackTask.setName("cb_fallback");
        fallbackTask.setTimeoutSeconds(60);
        fallbackTask.setResponseTimeoutSeconds(30);
        fallbackTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(checkCircuitTask, callServiceTask, fallbackTask));

        System.out.println("  Registered: cb_check_circuit, cb_call_service, cb_fallback\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'circuit_breaker_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CheckCircuitWorker(),
                new CallServiceWorker(),
                new FallbackWorker()
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

        // Step 4 -- Scenario A: Circuit CLOSED (normal operation)
        System.out.println("Step 4: Scenario A -- Circuit CLOSED (normal, calls go through)...\n");
        String wfIdClosed = client.startWorkflow("circuit_breaker_demo", 1,
                Map.of("failureCount", 0, "threshold", 3, "serviceName", "payment-api"));
        System.out.println("  Workflow ID: " + wfIdClosed);
        Workflow wfClosed = client.waitForWorkflow(wfIdClosed, "COMPLETED", 30000);
        System.out.println("  Status: " + wfClosed.getStatus().name());
        System.out.println("  Output: " + wfClosed.getOutput() + "\n");

        // Step 5 -- Scenario B: Circuit OPEN (failures exceed threshold, use fallback)
        System.out.println("Step 5: Scenario B -- Circuit OPEN (too many failures, use fallback)...\n");
        String wfIdOpen = client.startWorkflow("circuit_breaker_demo", 1,
                Map.of("failureCount", 5, "threshold", 3, "serviceName", "payment-api"));
        System.out.println("  Workflow ID: " + wfIdOpen);
        Workflow wfOpen = client.waitForWorkflow(wfIdOpen, "COMPLETED", 30000);
        System.out.println("  Status: " + wfOpen.getStatus().name());
        System.out.println("  Output: " + wfOpen.getOutput() + "\n");

        // Step 6 -- Scenario C: Circuit forced OPEN
        System.out.println("Step 6: Scenario C -- Circuit forced OPEN (manual override)...\n");
        String wfIdForced = client.startWorkflow("circuit_breaker_demo", 1,
                Map.of("circuitState", "OPEN", "serviceName", "payment-api"));
        System.out.println("  Workflow ID: " + wfIdForced);
        Workflow wfForced = client.waitForWorkflow(wfIdForced, "COMPLETED", 30000);
        System.out.println("  Status: " + wfForced.getStatus().name());
        System.out.println("  Output: " + wfForced.getOutput() + "\n");

        client.stopWorkers();

        boolean allPassed = "COMPLETED".equals(wfClosed.getStatus().name())
                && "COMPLETED".equals(wfOpen.getStatus().name())
                && "COMPLETED".equals(wfForced.getStatus().name());

        if (allPassed) {
            System.out.println("Result: PASSED");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }
}
