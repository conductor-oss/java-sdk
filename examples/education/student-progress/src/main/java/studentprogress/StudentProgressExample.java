package studentprogress;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import studentprogress.workers.*;
import java.util.List;
import java.util.Map;

/**
 * Example 675: Student Progress — Grades Analysis & Reporting
 */
public class StudentProgressExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 675: Student Progress ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("spr_collect_grades", "spr_analyze", "spr_generate_report", "spr_notify"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(
                new CollectGradesWorker(), new AnalyzeWorker(),
                new GenerateReportWorker(), new NotifyWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String workflowId = client.startWorkflow("spr_student_progress", 1,
                Map.of("studentId", "STU-2024-675", "semester", "Spring 2024"));
        System.out.println("  Workflow ID: " + workflowId + "\n");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Courses: " + workflow.getOutput().get("courseCount"));
        System.out.println("  GPA: " + workflow.getOutput().get("gpa"));
        System.out.println("  Standing: " + workflow.getOutput().get("standing"));
        System.out.println("  Report: " + workflow.getOutput().get("reportGenerated"));
        System.out.println("  Notified: " + workflow.getOutput().get("notified"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
