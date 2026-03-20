package gradingworkflow;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import gradingworkflow.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 673: Grading Workflow — Submit, Grade, Review & Record
 */
public class GradingWorkflowExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 673: Grading Workflow ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("grd_submit", "grd_grade", "grd_review", "grd_record", "grd_notify"));

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SubmitWorker(), new GradeWorker(), new ReviewWorker(),
                new RecordWorker(), new NotifyWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("grd_grading", 1,
                Map.of("studentId", "STU-2024-673", "courseId", "CS-101", "assignmentId", "HW-05"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Submission: " + workflow.getOutput().get("submissionId"));
        System.out.println("  Score: " + workflow.getOutput().get("score"));
        System.out.println("  Final: " + workflow.getOutput().get("finalScore"));
        System.out.println("  Recorded: " + workflow.getOutput().get("recorded"));
        System.out.println("  Notified: " + workflow.getOutput().get("notified"));

        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
