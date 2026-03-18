package waitforevent;

import com.netflix.conductor.client.worker.Worker;
import waitforevent.workers.PrepareWorker;
import waitforevent.workers.ProcessSignalWorker;

import java.util.List;
import java.util.Map;

/**
 * WAIT Task — Pause Until External Signal
 *
 * Demonstrates the WAIT task for pausing workflow execution until an external
 * system completes the task via the Conductor API.
 *
 * The WAIT task is a system task (no worker) that holds the workflow in a
 * paused state. An external system calls the Conductor API to complete the
 * WAIT task with output data (decision, signalData), which resumes the
 * workflow and passes the data to downstream tasks.
 *
 * Useful for:
 * - Human-in-the-loop approvals
 * - Waiting for external system callbacks
 * - Asynchronous event-driven workflows
 *
 * Run:
 *   java -jar target/wait-for-event-1.0.0.jar --workers
 */
public class WaitForEventExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== WAIT Task: Pause Until External Signal ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("we_prepare", "we_process_signal"));
        System.out.println("  Registered: we_prepare, we_process_signal");
        System.out.println("  Note: wait_for_signal is a system WAIT task — no worker needed.\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'wait_event_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new PrepareWorker(), new ProcessSignalWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("The WAIT task pauses execution until you complete it via the API.");
            System.out.println("");
            System.out.println("Example:");
            System.out.println("  1. Start workflow:");
            System.out.println("     conductor workflow start --workflow wait_event_demo --version 1 \\");
            System.out.println("       --input '{\"requestId\": \"REQ-001\", \"requester\": \"alice\"}'");
            System.out.println("");
            System.out.println("  2. Find the WAIT task ID in the Conductor UI, then complete it:");
            System.out.println("     curl -X POST http://localhost:8080/api/tasks \\");
            System.out.println("       -H 'Content-Type: application/json' \\");
            System.out.println("       -d '{\"taskId\": \"<WAIT_TASK_ID>\", \"status\": \"COMPLETED\", \\");
            System.out.println("            \"outputData\": {\"decision\": \"approved\", \"signalData\": \"external-data\"}}'");
            System.out.println("");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        System.out.println("This example runs in --workers mode only.");
        System.out.println("The WAIT task requires external API completion, so automated");
        System.out.println("end-to-end execution is not possible without a running Conductor.\n");
        System.out.println("To run the full flow:");
        System.out.println("  java -jar target/wait-for-event-1.0.0.jar --workers\n");

        client.stopWorkers();
    }
}
