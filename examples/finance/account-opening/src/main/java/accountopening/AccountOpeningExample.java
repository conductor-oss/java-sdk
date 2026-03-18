package accountopening;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import accountopening.workers.*;
import java.util.List;
import java.util.Map;
public class AccountOpeningExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 499: Account Opening ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("acc_collect_info", "acc_verify_identity", "acc_credit_check", "acc_open_account", "acc_welcome"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectInfoWorker(), new VerifyIdentityWorker(), new CreditCheckWorker(), new OpenAccountWorker(), new WelcomeWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("account_opening_workflow", 1, Map.of("applicationId", "ACCTAPP-2024-001", "applicantName", "Emily Davis", "accountType", "checking", "initialDeposit", 1000));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
