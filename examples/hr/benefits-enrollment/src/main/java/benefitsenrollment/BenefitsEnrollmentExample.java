package benefitsenrollment;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import benefitsenrollment.workers.*;
import java.util.List;
import java.util.Map;

public class BenefitsEnrollmentExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 607: Benefits Enrollment ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ben_present","ben_select","ben_validate","ben_enroll","ben_confirm"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new PresentWorker(), new SelectWorker(), new ValidateWorker(), new EnrollWorker(), new ConfirmWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("ben_benefits_enrollment", 1,
                Map.of("employeeId", "EMP-600", "enrollmentPeriod", "2024-open-enrollment"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Enrollment ID: " + wf.getOutput().get("enrollmentId"));
        System.out.println("  Monthly premium: " + wf.getOutput().get("monthlyPremium"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
