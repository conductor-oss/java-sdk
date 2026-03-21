package apitestgeneration;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import apitestgeneration.workers.*;
import java.util.List;
import java.util.Map;
public class ApiTestGenerationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 644: API Test Generation ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("atg_parse_spec", "atg_generate_tests", "atg_run_tests", "atg_report"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ParseSpecWorker(), new GenerateTestsWorker(), new RunTestsWorker(), new ReportWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("atg_api_test_generation", 1,
                Map.of("specUrl", "https://api.example.com/openapi.json", "format", "openapi3"));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
