package codegeneration;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import codegeneration.workers.*;
import java.util.List;
import java.util.Map;
public class CodeGenerationExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 639: Code Generation ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cdg_parse_requirements", "cdg_generate_code", "cdg_validate", "cdg_test"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ParseRequirementsWorker(), new GenerateCodeWorker(), new ValidateWorker(), new TestWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("cdg_code_generation", 1, Map.of("requirements", "Build a REST API with user management", "language", "javascript", "framework", "express"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
