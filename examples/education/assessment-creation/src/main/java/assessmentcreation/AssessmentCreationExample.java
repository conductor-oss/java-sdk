package assessmentcreation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import assessmentcreation.workers.*;
import java.util.List;
import java.util.Map;

/**
 * Example 677: Assessment Creation — Exam Design Pipeline
 */
public class AssessmentCreationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 677: Assessment Creation ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("asc_define_criteria", "asc_create_questions", "asc_review", "asc_publish"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(
                new DefineCriteriaWorker(), new CreateQuestionsWorker(),
                new ReviewWorker(), new PublishWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String workflowId = client.startWorkflow("asc_assessment_creation", 1,
                Map.of("courseId", "CS-201", "assessmentType", "midterm_exam",
                       "topics", List.of("sorting", "searching", "complexity", "trees")));
        System.out.println("  Workflow ID: " + workflowId + "\n");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Criteria: " + workflow.getOutput().get("criteriaCount"));
        System.out.println("  Questions: " + workflow.getOutput().get("questionCount"));
        System.out.println("  Assessment: " + workflow.getOutput().get("assessmentId"));
        System.out.println("  Review: " + workflow.getOutput().get("reviewStatus"));
        System.out.println("  Published: " + workflow.getOutput().get("published"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
