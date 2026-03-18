package contractlifecycle;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import contractlifecycle.workers.*;
import java.util.*;

public class ContractLifecycleExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 662: Contract Lifecycle ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("clf_draft","clf_review","clf_approve","clf_execute","clf_renew"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new DraftWorker(), new ReviewWorker(), new ApproveWorker(), new ExecuteWorker(), new RenewWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("clf_contract_lifecycle", 1,
                Map.of("vendor","GlobalLogistics Corp","contractType","service-agreement","value",250000,"termMonths",12));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
