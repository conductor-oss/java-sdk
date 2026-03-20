package mortgageapplication;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import mortgageapplication.workers.*;
import java.util.List;
import java.util.Map;
public class MortgageApplicationExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 683: Mortgage Application ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("mtg_apply","mtg_credit_check","mtg_underwrite","mtg_approve","mtg_close"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new ApplyWorker(),new CreditCheckWorker(),new UnderwriteWorker(),new ApproveWorker(),new CloseWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("mtg_mortgage_application",1,Map.of("applicantId","APP-300","loanAmount",300000,"propertyValue",475000));
        Workflow wf = client.waitForWorkflow(wfId,"COMPLETED",60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: "+status+"\n  Loan ID: "+wf.getOutput().get("loanId")+"\n  Closing: "+wf.getOutput().get("status"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(status)?0:1);
    }
}
