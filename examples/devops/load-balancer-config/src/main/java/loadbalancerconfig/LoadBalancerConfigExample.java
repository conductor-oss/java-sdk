package loadbalancerconfig;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import loadbalancerconfig.workers.DiscoverBackendsWorker;
import loadbalancerconfig.workers.ConfigureRulesWorker;
import loadbalancerconfig.workers.ApplyConfigWorker;
import loadbalancerconfig.workers.HealthCheckWorker;

import java.util.List;
import java.util.Map;

/**
 * Load Balancer Configuration Workflow Demo
 *
 * Demonstrates dynamic traffic management:
 *   lb_discover_backends -> lb_configure_rules -> lb_apply_config -> lb_health_check
 *
 * Run:
 *   java -jar target/load-balancer-config-1.0.0.jar
 */
public class LoadBalancerConfigExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Load Balancer Configuration Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "lb_discover_backends", "lb_configure_rules",
                "lb_apply_config", "lb_health_check"));
        System.out.println("  Registered: lb_discover_backends, lb_configure_rules, lb_apply_config, lb_health_check\n");

        System.out.println("Step 2: Registering workflow 'load_balancer_config_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DiscoverBackendsWorker(),
                new ConfigureRulesWorker(),
                new ApplyConfigWorker(),
                new HealthCheckWorker()
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
        String workflowId = client.startWorkflow("load_balancer_config_workflow", 1,
                Map.of("loadBalancer", "prod-alb-01", "action", "rebalance"));
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
