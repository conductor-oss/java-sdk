package workflowprofiling;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import workflowprofiling.workers.WfpInstrumentWorker;
import workflowprofiling.workers.WfpExecuteWorker;
import workflowprofiling.workers.WfpMeasureTimesWorker;
import workflowprofiling.workers.WfpBottleneckWorker;
import workflowprofiling.workers.WfpOptimizeWorker;

import java.util.List;
import java.util.Map;

/**
 * Workflow Profiling Demo
 *
 * Run:
 *   java -jar target/workflowprofiling-1.0.0.jar
 */
public class WorkflowProfilingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Workflow Profiling Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wfp_instrument",
                "wfp_execute",
                "wfp_measure_times",
                "wfp_bottleneck",
                "wfp_optimize"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'wfp_workflow_profiling'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new WfpInstrumentWorker(),
                new WfpExecuteWorker(),
                new WfpMeasureTimesWorker(),
                new WfpBottleneckWorker(),
                new WfpOptimizeWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("wfp_workflow_profiling", 1,
                Map.of("workflowName", "data_pipeline", "iterations", 5));
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