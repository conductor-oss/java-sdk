package nonprofitdonation;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import nonprofitdonation.workers.*;
import java.util.List;
import java.util.Map;
/** Example 751: Nonprofit Donation — Receive, Process Payment, Receipt, Thank You, Record */
public class NonprofitDonationExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 751: Nonprofit Donation ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("don_receive", "don_process_payment", "don_receipt", "don_thank_you", "don_record"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ReceiveWorker(), new ProcessPaymentWorker(), new ReceiptWorker(), new ThankYouWorker(), new RecordWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("nonprofit_donation_751", 1, Map.of("donorName", "Jane Smith", "amount", 250, "campaign", "Clean Water Initiative"));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
