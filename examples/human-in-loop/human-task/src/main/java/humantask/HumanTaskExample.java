package humantask;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import humantask.workers.CollectDataWorker;
import humantask.workers.ProcessFormWorker;

import java.util.List;
import java.util.Map;

/**
 * Human Task (Form-Based Data Collection) Demo
 *
 * Demonstrates a human-in-the-loop workflow where:
 * 1. collect_data gathers initial information
 * 2. A WAIT task performs a HUMAN task with a form schema for review
 * 3. process_form processes the human reviewer's decision
 *
 * Run:
 *   java -jar target/human-task-1.0.0.jar
 *   java -jar target/human-task-1.0.0.jar --workers
 */
public class HumanTaskExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Human Task (Form-Based Data Collection) Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");

        TaskDef collectDataTask = new TaskDef();
        collectDataTask.setName("ht_collect_data");
        collectDataTask.setRetryCount(0);
        collectDataTask.setTimeoutSeconds(60);
        collectDataTask.setResponseTimeoutSeconds(30);
        collectDataTask.setOwnerEmail("examples@orkes.io");

        TaskDef processFormTask = new TaskDef();
        processFormTask.setName("ht_process_form");
        processFormTask.setRetryCount(0);
        processFormTask.setTimeoutSeconds(60);
        processFormTask.setResponseTimeoutSeconds(30);
        processFormTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(collectDataTask, processFormTask));

        System.out.println("  Registered: ht_collect_data, ht_process_form\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'human_task_form_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new CollectDataWorker(), new ProcessFormWorker());
        client.startWorkers(workers);
        System.out.println("  2 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("human_task_form_demo", 1,
                Map.of("applicantName", "Jane Doe"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        System.out.println("  (Note: WAIT task requires external signal to proceed)");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
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
