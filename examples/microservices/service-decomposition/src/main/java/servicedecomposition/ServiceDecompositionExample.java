package servicedecomposition;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import servicedecomposition.workers.CheckFeatureFlagWorker;
import servicedecomposition.workers.CallMonolithWorker;
import servicedecomposition.workers.CallMicroserviceWorker;
import servicedecomposition.workers.CompareResultsWorker;

import java.util.List;
import java.util.Map;

/**
 * Service Decomposition Demo
 *
 * Demonstrates the strangler fig pattern:
 *   sd_check_feature_flag -> SWITCH(target:
 *       monolith -> sd_call_monolith,
 *       microservice -> sd_call_microservice,
 *       shadow -> FORK(monolith, microservice) -> sd_compare_results)
 *
 * Run:
 *   java -jar target/service-decomposition-1.0.0.jar
 */
public class ServiceDecompositionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Service Decomposition Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sd_check_feature_flag", "sd_call_monolith",
                "sd_call_microservice", "sd_compare_results"));
        System.out.println("  Registered tasks.\n");

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CheckFeatureFlagWorker(),
                new CallMonolithWorker(),
                new CallMicroserviceWorker(),
                new CompareResultsWorker()
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
        String workflowId = client.startWorkflow("service_decomposition_workflow", 1,
                Map.of("feature", "order-processing",
                        "request", Map.of("orderId", "ORD-100")));
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
