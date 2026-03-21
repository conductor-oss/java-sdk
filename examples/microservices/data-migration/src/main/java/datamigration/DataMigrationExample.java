package datamigration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import datamigration.workers.*;
import java.util.List;
import java.util.Map;

public class DataMigrationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 325: Data Migration ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("dm_backup", "dm_transform", "dm_migrate", "dm_validate", "dm_cutover"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new BackupWorker(), new TransformWorker(), new MigrateWorker(), new ValidateWorker(), new CutoverWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("data_migration_workflow", 1,
                Map.of("sourceDb", "orders-v1", "targetDb", "orders-v2", "migrationName", "order-schema-v2"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Migrated: " + wf.getOutput().get("migrated"));
        System.out.println("  Records: " + wf.getOutput().get("recordCount"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
