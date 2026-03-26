package penetrationtesting;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import penetrationtesting.workers.ReconnaissanceWorker;
import penetrationtesting.workers.ScanVulnerabilitiesWorker;
import penetrationtesting.workers.ExploitTestWorker;
import penetrationtesting.workers.GenerateReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Automated Pen Test Orchestration
 *
 * Pattern: reconnaissance -> scan-vulnerabilities -> exploit-test -> generate-report
 *
 * Run:
 *   java -jar target/penetration-testing-1.0.0.jar
 */
public class PenetrationTestingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Penetration Testing Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "pen_reconnaissance", "pen_scan_vulnerabilities",
                "pen_exploit_test", "pen_generate_report"));
        System.out.println("  Registered tasks.\n");

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ReconnaissanceWorker(),
                new ScanVulnerabilitiesWorker(),
                new ExploitTestWorker(),
                new GenerateReportWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("penetration_testing_workflow", 1,
                Map.of("target", "api.example.com",
                        "scope", "external"));
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
