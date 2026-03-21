package bulkuserimport;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import bulkuserimport.workers.*;
import java.util.List;
import java.util.Map;

public class BulkUserImportExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 616: Bulk User Import ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("bui_parse_file", "bui_validate", "bui_batch_insert", "bui_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ParseFileWorker(), new ValidateRecordsWorker(), new BatchInsertWorker(), new ReportWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String workflowId = client.startWorkflow("bui_bulk_user_import", 1, Map.of("fileUrl", "https://uploads.example.com/users-batch-42.csv", "format", "csv"));
        System.out.println("  Workflow ID: " + workflowId + "\n");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + workflow.getOutput());
        client.stopWorkers();
        System.out.println("\nResult: " + ("COMPLETED".equals(status) ? "PASSED" : "FAILED"));
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
