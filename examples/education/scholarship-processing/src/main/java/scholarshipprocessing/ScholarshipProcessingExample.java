package scholarshipprocessing;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import scholarshipprocessing.workers.*;
import java.util.List;
import java.util.Map;

/** Example 680: Scholarship Processing — Application to Award */
public class ScholarshipProcessingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 680: Scholarship Processing ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("scp_apply", "scp_evaluate", "scp_rank", "scp_award", "scp_notify"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ApplyWorker(), new EvaluateWorker(), new RankWorker(), new AwardWorker(), new NotifyWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String workflowId = client.startWorkflow("scp_scholarship_processing", 1,
                Map.of("studentId", "STU-2024-680", "scholarshipId", "MERIT-2024", "gpa", 3.9, "financialNeed", "high"));
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Application: " + workflow.getOutput().get("applicationId"));
        System.out.println("  Score: " + workflow.getOutput().get("score"));
        System.out.println("  Rank: " + workflow.getOutput().get("rank"));
        System.out.println("  Awarded: " + workflow.getOutput().get("awarded"));
        System.out.println("  Amount: " + workflow.getOutput().get("amount"));
        System.out.println("  Notified: " + workflow.getOutput().get("notified"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
