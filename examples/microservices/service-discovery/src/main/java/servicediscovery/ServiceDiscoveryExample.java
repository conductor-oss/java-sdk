package servicediscovery;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import servicediscovery.workers.*;

import java.util.List;
import java.util.Map;

public class ServiceDiscoveryExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Service Discovery Demo ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("sd_discover_services", "sd_select_instance", "sd_call_service", "sd_handle_failover"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new DiscoverServicesWorker(), new SelectInstanceWorker(), new CallServiceWorker(), new HandleFailoverWorker());
        client.startWorkers(workers);

        if (workersOnly) { System.out.println("Worker-only mode. Ctrl+C to stop."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("service_discovery_293", 1,
                Map.of("serviceName", "order-service", "request", Map.of("action", "getOrder", "orderId", "ORD-555")));
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        System.out.println("Status: " + workflow.getStatus().name());
        client.stopWorkers();
        System.exit("COMPLETED".equals(workflow.getStatus().name()) ? 0 : 1);
    }
}
