package gracefuldegradation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import gracefuldegradation.workers.AnalyticsWorker;
import gracefuldegradation.workers.CoreProcessWorker;
import gracefuldegradation.workers.EnrichWorker;
import gracefuldegradation.workers.FinalizeWorker;

import java.util.List;
import java.util.Map;

/**
 * Graceful Degradation -- Continue with Reduced Functionality
 *
 * Demonstrates a workflow where core processing always runs, but optional
 * enrichment and analytics services may be unavailable. When they are,
 * the workflow continues with reduced functionality and sets a degraded flag.
 *
 * Run:
 *   java -jar target/graceful-degradation-1.0.0.jar
 *   java -jar target/graceful-degradation-1.0.0.jar --workers
 */
public class GracefulDegradationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Graceful Degradation Demo: Continue with Reduced Functionality ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef coreTask = createTaskDef("gd_core_process");
        TaskDef enrichTask = createTaskDef("gd_enrich");
        TaskDef analyticsTask = createTaskDef("gd_analytics");
        TaskDef finalizeTask = createTaskDef("gd_finalize");

        client.registerTaskDefs(List.of(coreTask, enrichTask, analyticsTask, finalizeTask));

        System.out.println("  Registered: gd_core_process, gd_enrich, gd_analytics, gd_finalize\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'graceful_degradation_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CoreProcessWorker(),
                new EnrichWorker(),
                new AnalyticsWorker(),
                new FinalizeWorker()
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

        // Step 4 -- Run with all services available (no degradation)
        System.out.println("Step 4: Starting workflow with all services available...\n");
        String wfId1 = client.startWorkflow("graceful_degradation_demo", 1,
                Map.of("data", "order-123", "enrichAvailable", true, "analyticsAvailable", true));
        System.out.println("  Workflow ID: " + wfId1);

        Workflow wf1 = client.waitForWorkflow(wfId1, "COMPLETED", 30000);
        System.out.println("  Status: " + wf1.getStatus().name());
        System.out.println("  Output: " + wf1.getOutput());
        System.out.println();

        // Step 5 -- Run with enrichment unavailable (degraded mode)
        System.out.println("Step 5: Starting workflow with enrichment unavailable...\n");
        String wfId2 = client.startWorkflow("graceful_degradation_demo", 1,
                Map.of("data", "order-456", "enrichAvailable", false, "analyticsAvailable", true));
        System.out.println("  Workflow ID: " + wfId2);

        Workflow wf2 = client.waitForWorkflow(wfId2, "COMPLETED", 30000);
        System.out.println("  Status: " + wf2.getStatus().name());
        System.out.println("  Output: " + wf2.getOutput());
        System.out.println();

        client.stopWorkers();

        boolean passed = "COMPLETED".equals(wf1.getStatus().name())
                && "COMPLETED".equals(wf2.getStatus().name());

        if (passed) {
            System.out.println("Result: PASSED");
            System.exit(0);
        } else {
            System.out.println("Result: FAILED");
            System.exit(1);
        }
    }

    private static TaskDef createTaskDef(String name) {
        TaskDef taskDef = new TaskDef();
        taskDef.setName(name);
        taskDef.setRetryCount(0);
        taskDef.setTimeoutSeconds(60);
        taskDef.setResponseTimeoutSeconds(30);
        taskDef.setOwnerEmail("examples@orkes.io");
        return taskDef;
    }
}
