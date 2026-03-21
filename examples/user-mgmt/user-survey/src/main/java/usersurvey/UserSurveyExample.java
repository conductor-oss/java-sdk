package usersurvey;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import usersurvey.workers.*;
import java.util.List;
import java.util.Map;

public class UserSurveyExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 619: User Survey ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("usv_create", "usv_distribute", "usv_collect", "usv_analyze", "usv_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CreateSurveyWorker(), new DistributeSurveyWorker(), new CollectResponsesWorker(), new AnalyzeSurveyWorker(), new SurveyReportWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String workflowId = client.startWorkflow("usv_user_survey", 1,
                Map.of("surveyTitle", "Q4 Product Satisfaction", "questions", List.of("Overall satisfaction?", "Feature most valued?", "Improvement suggestions?"), "targetAudience", "active_users"));
        System.out.println("  Workflow ID: " + workflowId + "\n");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + workflow.getOutput());
        client.stopWorkers();
        System.out.println("\nResult: " + ("COMPLETED".equals(status) ? "PASSED" : "FAILED"));
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
