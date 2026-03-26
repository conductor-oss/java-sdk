package plagiarismdetection;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import plagiarismdetection.workers.*;
import java.util.List;
import java.util.Map;

/**
 * Example 678: Plagiarism Detection — Scan, Compare & Route with SWITCH
 */
public class PlagiarismDetectionExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 678: Plagiarism Detection ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("plg_submit", "plg_scan", "plg_compare", "plg_handle_clean", "plg_handle_flagged", "plg_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(
                new SubmitWorker(), new ScanWorker(), new CompareWorker(),
                new HandleCleanWorker(), new HandleFlaggedWorker(), new ReportWorker());
        client.startWorkers(workers);
        System.out.println("  6 workers polling.\n");
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String workflowId = client.startWorkflow("plg_plagiarism_detection", 1,
                Map.of("studentId", "STU-2024-678", "assignmentId", "ESSAY-03",
                       "documentText", "This is a default essay about data structures and their applications in modern computing systems."));
        System.out.println("  Workflow ID: " + workflowId + "\n");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Submission: " + workflow.getOutput().get("submissionId"));
        System.out.println("  Similarity: " + workflow.getOutput().get("similarityScore") + "%");
        System.out.println("  Verdict: " + workflow.getOutput().get("verdict"));
        System.out.println("  Report: " + workflow.getOutput().get("reportGenerated"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
