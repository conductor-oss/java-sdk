package databaseintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import databaseintegration.workers.ConnectWorker;
import databaseintegration.workers.QueryWorker;
import databaseintegration.workers.TransformWorker;
import databaseintegration.workers.WriteWorker;
import databaseintegration.workers.VerifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 444: Database Integration
 *
 * Performs a database integration workflow:
 * connect -> query -> transform -> write -> verify.
 *
 * Run:
 *   java -jar target/database-integration-1.0.0.jar
 */
public class DatabaseIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 444: Database Integration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "dbi_connect", "dbi_query", "dbi_transform", "dbi_write", "dbi_verify"));
        System.out.println("  Registered: dbi_connect, dbi_query, dbi_transform, dbi_write, dbi_verify\n");

        System.out.println("Step 2: Registering workflow \'database_integration_444\'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ConnectWorker(),
                new QueryWorker(),
                new TransformWorker(),
                new WriteWorker(),
                new VerifyWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("database_integration_444", 1,
                Map.of("sourceDb", "postgres://source:5432/users",
                        "targetDb", "postgres://target:5432/users_v2",
                        "query", "SELECT * FROM users WHERE status = 'active'",
                        "transformRules", List.of("uppercase_name", "add_timestamp")));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Source Rows: " + workflow.getOutput().get("sourceRows"));
        System.out.println("  Transformed: " + workflow.getOutput().get("transformedRows"));
        System.out.println("  Written: " + workflow.getOutput().get("writtenRows"));
        System.out.println("  Verified: " + workflow.getOutput().get("verified"));

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
