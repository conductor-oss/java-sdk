package performancereview;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import performancereview.workers.SelfEvalWorker;
import performancereview.workers.ManagerEvalWorker;
import performancereview.workers.CalibrateWorker;
import performancereview.workers.FinalizeWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 608: Performance Review
 *
 * Runs an annual performance review cycle.
 */
public class PerformanceReviewExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 608: Performance Review ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "pfr_self_eval", "pfr_manager_eval", "pfr_calibrate", "pfr_finalize"));

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SelfEvalWorker(), new ManagerEvalWorker(),
                new CalibrateWorker(), new FinalizeWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("pfr_performance_review", 1,
                Map.of("employeeId", "EMP-300", "reviewPeriod", "2023-H2", "managerId", "MGR-50"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Final rating: " + workflow.getOutput().get("finalRating"));
        System.out.println("  Review ID: " + workflow.getOutput().get("reviewId"));

        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
