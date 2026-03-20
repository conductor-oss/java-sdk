package automatedtesting;

import com.netflix.conductor.client.worker.Worker;
import automatedtesting.workers.*;

import java.util.*;

/**
 * Automated Testing — Test Suite Orchestration
 *
 * Run:
 *   CONDUCTOR_BASE_URL=http://localhost:8080/api java -jar target/automated-testing-1.0.0.jar
 */
public class AutomatedTestingExample {

    private static final List<String> TASK_NAMES = List.of(
            "at_setup_env", "at_run_unit", "at_run_integration", "at_run_e2e", "at_aggregate_results"
    );

    private static List<Worker> allWorkers() {
        return List.of(new SetupEnv(), new RunUnit(), new RunIntegration(), new RunE2e(), new AggregateResults());
    }

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Automated Testing Demo ===\n");
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
        input.put("suite", "full");
        input.put("branch", "main");

        String workflowId = client.startWorkflow("automated_testing_workflow", 1, input);
        System.out.println("  Workflow ID: " + workflowId);

        var workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        System.out.println("  Status: " + workflow.getStatus().name());

        client.stopWorkers();
    }
}
