package circuitbreakermicroservice;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import circuitbreakermicroservice.workers.*;
import java.util.List;
import java.util.Map;

public class CircuitBreakerMicroserviceExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 310: Circuit Breaker Microservice ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cb_check_circuit", "cb_call_service", "cb_record_result", "cb_fallback"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new CheckCircuitWorker(), new CallServiceWorker(), new RecordResultWorker(), new FallbackWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("circuit_breaker_workflow", 1,
                Map.of("serviceName", "inventory-service", "request", Map.of("itemId", "SKU-500")));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Circuit: " + wf.getOutput().get("circuitState"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
