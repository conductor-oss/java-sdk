package normalizer;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import normalizer.workers.NrmDetectFormatWorker;
import normalizer.workers.NrmConvertJsonWorker;
import normalizer.workers.NrmConvertXmlWorker;
import normalizer.workers.NrmConvertCsvWorker;
import normalizer.workers.NrmOutputWorker;

import java.util.List;
import java.util.Map;

/**
 * Normalizer Demo
 *
 * Run:
 *   java -jar target/normalizer-1.0.0.jar
 */
public class NormalizerExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Normalizer Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "nrm_detect_format",
                "nrm_convert_json",
                "nrm_convert_xml",
                "nrm_convert_csv",
                "nrm_output"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'nrm_normalizer'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new NrmDetectFormatWorker(),
                new NrmConvertJsonWorker(),
                new NrmConvertXmlWorker(),
                new NrmConvertCsvWorker(),
                new NrmOutputWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("nrm_normalizer", 1,
                Map.of("rawInput", java.util.Map.of("name", "Widget A", "price", 19.99), "sourceSystem", "partner_api"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}