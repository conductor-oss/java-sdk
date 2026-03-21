package reportgeneration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import reportgeneration.workers.QueryDataWorker;
import reportgeneration.workers.AggregateResultsWorker;
import reportgeneration.workers.FormatReportWorker;
import reportgeneration.workers.DistributeReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Report Generation Workflow Demo
 *
 * Demonstrates a report generation pipeline:
 *   rg_query_data -> rg_aggregate_results -> rg_format_report -> rg_distribute_report
 *
 * Run:
 *   java -jar target/report-generation-1.0.0.jar
 */
public class ReportGenerationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Report Generation Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 - Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "rg_query_data", "rg_aggregate_results",
                "rg_format_report", "rg_distribute_report"));
        System.out.println("  Registered: rg_query_data, rg_aggregate_results, rg_format_report, rg_distribute_report\n");

        // Step 2 - Register workflow
        System.out.println("Step 2: Registering workflow 'report_generation'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 - Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new QueryDataWorker(),
                new AggregateResultsWorker(),
                new FormatReportWorker(),
                new DistributeReportWorker()
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

        // Step 4 - Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("report_generation", 1,
                Map.of("reportType", "sales",
                        "dateRange", Map.of("start", "2024-03-01", "end", "2024-03-07"),
                        "recipients", List.of("cfo@company.com", "sales-team@company.com", "#sales-channel")));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 - Wait for completion
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
