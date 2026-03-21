package taxcalculation;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import taxcalculation.workers.*;
import java.util.List;
import java.util.Map;

/** Example 470: Tax Calculation */
public class TaxCalculationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 470: Tax Calculation ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("tax_determine_jurisdiction", "tax_calculate_rates", "tax_apply", "tax_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new DetermineJurisdictionWorker(), new CalculateRatesWorker(), new ApplyTaxWorker(), new TaxReportWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("tax_calculation_workflow", 1, Map.of("orderId", "ORD-88421", "subtotal", 259.99, "shippingAddress", Map.of("state", "CA", "city", "San Jose", "zip", "95112", "country", "US")));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 30000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
