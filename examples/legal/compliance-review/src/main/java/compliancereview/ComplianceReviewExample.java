package compliancereview;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import compliancereview.workers.*;
import java.util.List;
import java.util.Map;
public class ComplianceReviewExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 695: Compliance Review ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cmr_identify","cmr_assess","cmr_gap_analysis","cmr_remediate"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new IdentifyWorker(),new AssessWorker(),new GapAnalysisWorker(),new RemediateWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("cmr_compliance_review",1,Map.of("regulationType","SOC2","entityId","ORG-500"));
        Workflow wf = client.waitForWorkflow(wfId,"COMPLETED",60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: "+status+"\n  Score: "+wf.getOutput().get("complianceScore")+"\n  Gaps: "+wf.getOutput().get("gapsFound")+"\n  Plan: "+wf.getOutput().get("remediationPlan"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(status)?0:1);
    }
}
