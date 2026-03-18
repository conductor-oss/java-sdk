package servicediscoverydevops;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import servicediscoverydevops.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Service Discovery (DevOps) Demo -- Dynamic Service Registration and Health
 *
 * Pattern:
 *   register -> health-check -> update-routing -> notify-consumers
 */
public class ServiceDiscoveryDevopsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Service Discovery (DevOps) Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sd_register", "sd_health_check", "sd_update_routing", "sd_notify_consumers"));
        System.out.println("  Registered.\n");

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RegisterWorker(), new HealthCheckWorker(),
                new UpdateRoutingWorker(), new NotifyConsumersWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("service_discovery_devops_workflow", 1,
                Map.of("serviceName", "recommendation-engine",
                        "endpoint", "http://rec-svc.prod.internal:8080",
                        "version", "2.4.1",
                        "healthCheckUrl", "http://rec-svc.prod.internal:8080/health"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();
        System.out.println("\nResult: " + ("COMPLETED".equals(status) ? "PASSED" : "FAILED"));
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
