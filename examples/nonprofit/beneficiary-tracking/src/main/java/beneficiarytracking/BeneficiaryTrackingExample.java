package beneficiarytracking;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import beneficiarytracking.workers.*;
import java.util.List;
import java.util.Map;
/** Example 758: Beneficiary Tracking — Register, Assess Needs, Provide Services, Monitor, Report */
public class BeneficiaryTrackingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 758: Beneficiary Tracking ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("btr_register", "btr_assess_needs", "btr_provide_services", "btr_monitor", "btr_report"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new RegisterWorker(), new AssessNeedsWorker(), new ProvideServicesWorker(), new MonitorWorker(), new ReportWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("beneficiary_tracking_758", 1, Map.of("beneficiaryName", "Aisha Johnson", "programId", "PGM-Education", "location", "East District"));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
