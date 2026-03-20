package testgeneration;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import testgeneration.workers.*;
import java.util.List;
import java.util.Map;
public class TestGenerationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 641: Test Generation ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("tge_analyze_code", "tge_generate_tests", "tge_validate_tests", "tge_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new AnalyzeCodeWorker(), new GenerateTestsWorker(), new ValidateTestsWorker(), new ReportWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("tge_test_generation", 1,
                Map.of("sourceFile", "src/utils/calculator.js", "language", "javascript", "framework", "jest"));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
