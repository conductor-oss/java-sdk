package insuranceunderwriting;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import insuranceunderwriting.workers.*;
import java.util.List;
import java.util.Map;
public class InsuranceUnderwritingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 500: Insurance Underwriting ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("uw_collect_app", "uw_assess_risk", "uw_quote", "uw_accept", "uw_decline", "uw_refer", "uw_bind"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectAppWorker(), new AssessRiskWorker(), new QuoteWorker(), new AcceptWorker(), new DeclineWorker(), new ReferWorker(), new BindWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("insurance_underwriting_workflow", 1, Map.of("applicationId", "INSAPP-2024-001", "applicantName", "Michael Chen", "coverageType", "term_life", "coverageAmount", 500000));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
