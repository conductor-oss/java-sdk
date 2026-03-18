package contentenricher;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import contentenricher.workers.ReceiveMessageWorker;
import contentenricher.workers.LookupDataWorker;
import contentenricher.workers.EnrichWorker;
import contentenricher.workers.ForwardWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 592: Content Enricher -- Lookup and Enrich Messages
 */
public class ContentEnricherExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 592: Content Enricher ===\n");

        var client = new ConductorClientHelper();

        client.registerTaskDefs(List.of(
                "enr_receive_message", "enr_lookup_data", "enr_enrich", "enr_forward"));

        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new ReceiveMessageWorker(), new LookupDataWorker(),
                new EnrichWorker(), new ForwardWorker());
        client.startWorkers(workers);

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        String workflowId = client.startWorkflow("enr_content_enricher", 1,
                Map.of("message", Map.of("customerId", "CUST-42", "orderId", "ORD-999", "amount", 1250.00),
                        "enrichmentSources", List.of("customer_db", "crm_system")));

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        System.out.println("Status: " + workflow.getStatus().name());
        System.out.println("Output: " + workflow.getOutput());

        client.stopWorkers();
        System.out.println("COMPLETED".equals(workflow.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
    }
}
