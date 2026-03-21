package serviceactivation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import serviceactivation.workers.ValidateOrderWorker;
import serviceactivation.workers.ProvisionWorker;
import serviceactivation.workers.TestWorker;
import serviceactivation.workers.ActivateWorker;
import serviceactivation.workers.NotifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 814: Service Activation
 */
public class ServiceActivationExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 814: Service Activation ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("sac_validate_order", "sac_provision", "sac_test", "sac_activate", "sac_notify"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ValidateOrderWorker(), new ProvisionWorker(), new TestWorker(), new ActivateWorker(), new NotifyWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("sac_service_activation", 1, Map.of("orderId", "ORD-814", "customerId", "CUST-814", "serviceType", "VoIP"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("activated: %s%n", workflow.getOutput().get("activated"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
