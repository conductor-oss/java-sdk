package tutoringmatch;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import tutoringmatch.workers.*;
import java.util.List;
import java.util.Map;

/** Example 679: Tutoring Match — Student-Tutor Pairing */
public class TutoringMatchExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 679: Tutoring Match ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("tut_student_request", "tut_match_tutor", "tut_schedule", "tut_confirm"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new StudentRequestWorker(), new MatchTutorWorker(), new ScheduleWorker(), new ConfirmWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String workflowId = client.startWorkflow("tut_tutoring_match", 1,
                Map.of("studentId", "STU-2024-679", "subject", "Calculus II", "preferredTime", "Tuesday 3:00 PM"));
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Request: " + workflow.getOutput().get("requestId"));
        System.out.println("  Tutor: " + workflow.getOutput().get("tutorName"));
        System.out.println("  Session: " + workflow.getOutput().get("sessionId"));
        System.out.println("  Time: " + workflow.getOutput().get("sessionTime"));
        System.out.println("  Confirmed: " + workflow.getOutput().get("confirmed"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
