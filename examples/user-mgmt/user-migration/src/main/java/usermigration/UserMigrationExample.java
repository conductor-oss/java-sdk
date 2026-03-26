package usermigration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import usermigration.workers.*;
import java.util.List;
import java.util.Map;

public class UserMigrationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 615: User Migration ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("umg_extract", "umg_transform", "umg_load", "umg_verify", "umg_notify"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ExtractWorker(), new TransformWorker(), new LoadWorker(), new VerifyMigrationWorker(), new NotifyMigrationWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String workflowId = client.startWorkflow("umg_user_migration", 1, Map.of("sourceDb", "legacy_mysql", "targetDb", "postgres_v2", "batchSize", 100));
        System.out.println("  Workflow ID: " + workflowId + "\n");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + workflow.getOutput());
        client.stopWorkers();
        System.out.println("\nResult: " + ("COMPLETED".equals(status) ? "PASSED" : "FAILED"));
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
