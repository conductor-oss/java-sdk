package databaseperservice;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import databaseperservice.workers.*;
import java.util.List;
import java.util.Map;

public class DatabasePerServiceExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 314: Database per Service ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("dps_query_user_db", "dps_query_order_db", "dps_query_product_db", "dps_compose_view"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new QueryUserDbWorker(), new QueryOrderDbWorker(), new QueryProductDbWorker(), new ComposeViewWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("database_per_service_workflow", 1, Map.of("userId", "user-42"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
