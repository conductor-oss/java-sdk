package serviceregistry;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import serviceregistry.workers.RegisterServiceWorker;
import serviceregistry.workers.HealthCheckWorker;
import serviceregistry.workers.DiscoverServiceWorker;

import java.util.List;
import java.util.Map;

/**
 * Service Registry Demo
 *
 * Demonstrates workflow-based service registration and discovery:
 *   sr_register_service -> sr_health_check -> sr_discover_service
 *
 * Run:
 *   java -jar target/service-registry-1.0.0.jar
 */
public class ServiceRegistryExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Service Registry Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sr_register_service", "sr_health_check", "sr_discover_service"));
        System.out.println("  Registered: sr_register_service, sr_health_check, sr_discover_service\n");

        System.out.println("Step 2: Registering workflow 'service_registry_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RegisterServiceWorker(),
                new HealthCheckWorker(),
                new DiscoverServiceWorker()
        );
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("service_registry_workflow", 1,
                Map.of("serviceName", "order-service",
                        "serviceUrl", "http://order-svc:8080",
                        "version", "2.1.0"));
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
