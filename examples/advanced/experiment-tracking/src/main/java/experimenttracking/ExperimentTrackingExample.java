package experimenttracking;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import experimenttracking.workers.ExtDefineExperimentWorker;
import experimenttracking.workers.ExtRunExperimentWorker;
import experimenttracking.workers.ExtLogMetricsWorker;
import experimenttracking.workers.ExtCompareWorker;
import experimenttracking.workers.ExtDecideWorker;

import java.util.List;
import java.util.Map;

/**
 * Experiment Tracking Demo
 *
 * Run:
 *   java -jar target/experimenttracking-1.0.0.jar
 */
public class ExperimentTrackingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Experiment Tracking Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ext_define_experiment",
                "ext_run_experiment",
                "ext_log_metrics",
                "ext_compare",
                "ext_decide"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'experiment_tracking_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ExtDefineExperimentWorker(),
                new ExtRunExperimentWorker(),
                new ExtLogMetricsWorker(),
                new ExtCompareWorker(),
                new ExtDecideWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("experiment_tracking_demo", 1,
                Map.of("experimentName", "new-embedding-layer", "hypothesis", "Adding attention layer improves accuracy", "baselineMetric", 0.92));
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