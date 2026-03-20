package competingconsumers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import competingconsumers.workers.CcsPublishWorker;
import competingconsumers.workers.CcsCompeteWorker;
import competingconsumers.workers.CcsProcessWorker;
import competingconsumers.workers.CcsAcknowledgeWorker;

import java.util.List;
import java.util.Map;

/**
 * Competing Consumers Demo
 *
 * Run:
 *   java -jar target/competingconsumers-1.0.0.jar
 */
public class CompetingConsumersExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Competing Consumers Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ccs_publish",
                "ccs_compete",
                "ccs_process",
                "ccs_acknowledge"));
        System.out.println("  Tasks registered.\n");

        System.out.println("Step 2: Registering workflow 'ccs_competing_consumers'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CcsPublishWorker(),
                new CcsCompeteWorker(),
                new CcsProcessWorker(),
                new CcsAcknowledgeWorker());
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("ccs_competing_consumers", 1,
                Map.of("taskPayload", "image_resize", "queueName", "image_processing", "consumerCount", 5));
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