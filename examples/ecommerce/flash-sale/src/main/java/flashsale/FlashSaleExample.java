package flashsale;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import flashsale.workers.*;
import java.util.List;
import java.util.Map;

/** Example 469: Flash Sale */
public class FlashSaleExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 469: Flash Sale ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("fls_prepare_inventory", "fls_open_sale", "fls_process_orders", "fls_close_sale", "fls_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new PrepareInventoryWorker(), new OpenSaleWorker(), new ProcessOrdersWorker(), new CloseSaleWorker(), new ReportWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("flash_sale_workflow", 1, Map.of("saleId", "FLASH-2024-003", "saleName", "Summer Blowout", "durationMinutes", 30));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 30000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
