package invoiceprocessing;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import invoiceprocessing.workers.*;
import java.util.List;
import java.util.Map;
public class InvoiceProcessingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 506: Invoice Processing ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ivc_receive_invoice","ivc_ocr_extract","ivc_match_po","ivc_approve_invoice","ivc_process_payment"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ReceiveInvoiceWorker(), new OcrExtractWorker(), new MatchPoWorker(), new ApproveInvoiceWorker(), new ProcessPaymentWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("invoice_processing_workflow", 1, Map.of("invoiceId","INV-2026-5500","vendorId","VND-330","documentUrl","https://docs.example.com/invoices/5500.pdf"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
