package userfeedback;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import userfeedback.workers.*;
import java.util.List;
import java.util.Map;

public class UserFeedbackExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 618: User Feedback ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ufb_collect", "ufb_classify", "ufb_route", "ufb_respond"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectFeedbackWorker(), new ClassifyFeedbackWorker(), new RouteFeedbackWorker(), new RespondFeedbackWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String workflowId = client.startWorkflow("ufb_user_feedback", 1,
                Map.of("userId", "USR-FB001", "feedbackText", "I found a bug in the dashboard - charts crash on mobile", "source", "in-app"));
        System.out.println("  Workflow ID: " + workflowId + "\n");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + workflow.getOutput());
        client.stopWorkers();
        System.out.println("\nResult: " + ("COMPLETED".equals(status) ? "PASSED" : "FAILED"));
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
