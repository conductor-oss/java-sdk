package environmentmanagement;

import com.netflix.conductor.client.worker.Worker;
import environmentmanagement.workers.*;

import java.util.*;

/**
 * Environment Management — Lifecycle Orchestration
 *
 * Run:
 *   CONDUCTOR_BASE_URL=http://localhost:8080/api java -jar target/environment-management-1.0.0.jar
 */
public class EnvironmentManagementExample {

    private static final List<String> TASK_NAMES = List.of(
            "em_create_env", "em_configure", "em_seed_data", "em_verify"
    );

    private static List<Worker> allWorkers() {
        return List.of(new CreateEnv(), new ConfigureEnv(), new SeedData(), new VerifyEnv());
    }

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Environment Management Demo ===\n");
        var client = new ConductorClientHelper();

        client.registerTaskDefs(TASK_NAMES);
        client.registerWorkflow("workflow.json");

        List<Worker> workers = allWorkers();
        client.startWorkers(workers);

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("envName", "staging-pr-42");
        input.put("template", "production-like");
        input.put("ttlHours", 24);

        String workflowId = client.startWorkflow("environment_management_workflow", 1, input);
        System.out.println("  Workflow ID: " + workflowId);

        var workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        System.out.println("  Status: " + workflow.getStatus().name());

        client.stopWorkers();
    }
}
