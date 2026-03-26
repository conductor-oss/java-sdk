package taskrouting;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import taskrouting.workers.TrtAnalyzeRequirementsWorker;
import taskrouting.workers.TrtSelectPoolWorker;
import taskrouting.workers.TrtDispatchWorker;
import taskrouting.workers.TrtVerifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Task Routing Demo
 *
 * Run:
 *   java -jar target/taskrouting-1.0.0.jar
 */
public class TaskRoutingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Task Routing Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "trt_analyze_requirements",
                "trt_select_pool",
                "trt_dispatch",
                "trt_verify"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'trt_task_routing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new TrtAnalyzeRequirementsWorker(),
                new TrtSelectPoolWorker(),
                new TrtDispatchWorker(),
                new TrtVerifyWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("trt_task_routing", 1,
                Map.of("taskType", "ml_inference", "region", "us-east"));
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