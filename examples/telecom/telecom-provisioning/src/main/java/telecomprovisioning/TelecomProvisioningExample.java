package telecomprovisioning;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import telecomprovisioning.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 815: Telecom Provisioning — Order, Validate, Configure, Activate, Confirm
 */
public class TelecomProvisioningExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 815: Telecom Provisioning ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("tpv_order", "tpv_validate", "tpv_configure", "tpv_activate", "tpv_confirm"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new OrderWorker(), new ValidateWorker(), new ConfigureWorker(), new ActivateWorker(), new ConfirmWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("tpv_telecom_provisioning", 1, Map.of("customerId", "CUST-815", "serviceType", "fiber-internet", "planId", "PLAN-100"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("Service ID: %s%n", workflow.getOutput().get("serviceId"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
