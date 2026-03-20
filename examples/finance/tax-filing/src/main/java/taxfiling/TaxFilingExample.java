package taxfiling;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import taxfiling.workers.*;
import java.util.List;
import java.util.Map;
public class TaxFilingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 503: Tax Filing ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("txf_collect_data","txf_calculate_tax","txf_validate_filing","txf_file_return","txf_confirm_submission"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectDataWorker(), new CalculateTaxWorker(), new ValidateFilingWorker(), new FileReturnWorker(), new ConfirmSubmissionWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("tax_filing_workflow", 1, Map.of("taxpayerId","TP-882244","taxYear",2025,"filingType","individual"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
