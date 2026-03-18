package workflowtesting;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import workflowtesting.workers.SetupWorker;
import workflowtesting.workers.ExecuteWorker;
import workflowtesting.workers.AssertWorker;
import workflowtesting.workers.TeardownWorker;
import workflowtesting.workers.ReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 571: Workflow Testing -- Structured Test Execution
 *
 * Demonstrates a test pipeline:
 *   wft_setup -> wft_execute -> wft_assert -> wft_teardown -> wft_report
 *
 * Run:
 *   java -jar target/workflow-testing-1.0.0.jar
 */
public class WorkflowTestingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 571: Workflow Testing ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wft_setup", "wft_execute", "wft_assert", "wft_teardown", "wft_report"));
        System.out.println("  Registered 5 task definitions.\n");

        System.out.println("Step 2: Registering workflow 'wft_workflow_testing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SetupWorker(),
                new ExecuteWorker(),
                new AssertWorker(),
                new TeardownWorker(),
                new ReportWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("wft_workflow_testing", 1,
                Map.of("testSuite", "order-processing-tests",
                        "workflowUnderTest", "order_processing_workflow",
                        "expectedOutput", Map.of("status", "SUCCESS", "processed", 2)));
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
