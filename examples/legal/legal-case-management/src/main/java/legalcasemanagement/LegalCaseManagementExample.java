package legalcasemanagement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import legalcasemanagement.workers.*;
import java.util.List;
import java.util.Map;
public class LegalCaseManagementExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 691: Legal Case Management ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("lcm_intake","lcm_assess","lcm_assign","lcm_track","lcm_close"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new IntakeWorker(),new AssessWorker(),new AssignWorker(),new TrackWorker(),new CloseWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("lcm_legal_case_management",1,Map.of("clientId","CLT-100","caseType","contract-dispute","description","Breach of vendor agreement"));
        Workflow wf = client.waitForWorkflow(wfId,"COMPLETED",60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: "+status+"\n  Case ID: "+wf.getOutput().get("caseId")+"\n  Outcome: "+wf.getOutput().get("outcome"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(status)?0:1);
    }
}
