package trainingmanagement;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import trainingmanagement.workers.*;
import java.util.List; import java.util.Map;
public class TrainingManagementExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 700: Training Management ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("trm_assign","trm_track","trm_assess","trm_certify","trm_record"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new AssignWorker(),new TrackWorker(),new AssessWorker(),new CertifyWorker(),new RecordWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("trm_training_management", 1,
                Map.of("employeeId","EMP-700","courseId","CRS-SEC-101","courseName","Security Awareness"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Certification ID: " + wf.getOutput().get("certificationId"));
        System.out.println("  Score: " + wf.getOutput().get("score"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
