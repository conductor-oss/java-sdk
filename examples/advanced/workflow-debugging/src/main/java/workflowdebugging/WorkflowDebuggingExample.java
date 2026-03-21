package workflowdebugging;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import workflowdebugging.workers.WfdInstrumentWorker;
import workflowdebugging.workers.WfdExecuteWorker;
import workflowdebugging.workers.WfdCollectTraceWorker;
import workflowdebugging.workers.WfdAnalyzeWorker;
import workflowdebugging.workers.WfdReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Workflow Debugging Demo
 *
 * Run:
 *   java -jar target/workflowdebugging-1.0.0.jar
 */
public class WorkflowDebuggingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Workflow Debugging Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "wfd_instrument",
                "wfd_execute",
                "wfd_collect_trace",
                "wfd_analyze",
                "wfd_report"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'wfd_workflow_debugging'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new WfdInstrumentWorker(),
                new WfdExecuteWorker(),
                new WfdCollectTraceWorker(),
                new WfdAnalyzeWorker(),
                new WfdReportWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("wfd_workflow_debugging", 1,
                Map.of("workflowName", "order-processing", "debugLevel", "VERBOSE"));
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
