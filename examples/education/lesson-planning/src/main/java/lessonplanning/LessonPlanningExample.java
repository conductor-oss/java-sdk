package lessonplanning;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import lessonplanning.workers.*;
import java.util.List;
import java.util.Map;

/**
 * Example 676: Lesson Planning — Define, Create & Publish
 */
public class LessonPlanningExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 676: Lesson Planning ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("lpl_define_objectives", "lpl_create_content", "lpl_review", "lpl_publish"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(
                new DefineObjectivesWorker(), new CreateContentWorker(),
                new ReviewWorker(), new PublishWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String workflowId = client.startWorkflow("lpl_lesson_planning", 1,
                Map.of("courseId", "CS-201", "lessonTitle", "Binary Search Trees", "week", 6));
        System.out.println("  Workflow ID: " + workflowId + "\n");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Objectives: " + workflow.getOutput().get("objectiveCount"));
        System.out.println("  Sections: " + workflow.getOutput().get("contentSections"));
        System.out.println("  Review: " + workflow.getOutput().get("reviewStatus"));
        System.out.println("  Published: " + workflow.getOutput().get("published"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
