package backgroundcheck;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import backgroundcheck.workers.*;
import java.util.List; import java.util.Map;
public class BackgroundCheckExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 710: Background Check ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("bgc_consent","bgc_criminal","bgc_employment","bgc_education","bgc_report"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new ConsentWorker(),new CriminalWorker(),new EmploymentWorker(),new EducationWorker(),new ReportWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("bgc_background_check", 1,
                Map.of("candidateName","Alex Rivera","candidateId","CAND-710"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Overall result: " + wf.getOutput().get("overallResult"));
        System.out.println("  Report ID: " + wf.getOutput().get("reportId"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
