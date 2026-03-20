package billingtelecom;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import billingtelecom.workers.CollectUsageWorker;
import billingtelecom.workers.RateWorker;
import billingtelecom.workers.InvoiceWorker;
import billingtelecom.workers.SendWorker;
import billingtelecom.workers.CollectPaymentWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 811: Billing (Telecom)
 */
public class BillingTelecomExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 811: Billing (Telecom) ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("btl_collect_usage", "btl_rate", "btl_invoice", "btl_send", "btl_collect_payment"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectUsageWorker(), new RateWorker(), new InvoiceWorker(), new SendWorker(), new CollectPaymentWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("btl_billing_telecom", 1, Map.of("customerId", "CUST-811", "billingPeriod", "2024-03"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("totalAmount: %s%n", workflow.getOutput().get("totalAmount"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
