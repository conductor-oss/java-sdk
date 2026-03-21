package workflowoptimization;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import workflowoptimization.workers.WfoAnalyzeExecutionWorker;
import workflowoptimization.workers.WfoIdentifyWasteWorker;
import workflowoptimization.workers.WfoParallelizeWorker;
import workflowoptimization.workers.WfoBenchmarkWorker;

import java.util.List;
import java.util.Map;

/**
 * Workflow Optimization Demo
 *
 * Run:
 *   java -jar target/workflowoptimization-1.0.0.jar
 */
public class WorkflowOptimizationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Workflow Optimization Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wfo_analyze_execution",
                "wfo_identify_waste",
                "wfo_parallelize",
                "wfo_benchmark"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'wfo_workflow_optimization'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new WfoAnalyzeExecutionWorker(),
                new WfoIdentifyWasteWorker(),
                new WfoParallelizeWorker(),
                new WfoBenchmarkWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("wfo_workflow_optimization", 1,
                Map.of("workflowName", "order_pipeline"));
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