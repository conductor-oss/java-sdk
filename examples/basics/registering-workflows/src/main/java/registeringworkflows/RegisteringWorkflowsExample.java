package registeringworkflows;

import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.common.run.Workflow;
import registeringworkflows.workers.EchoWorker;

import java.util.List;
import java.util.Map;

/**
 * Workflow Registration with Conductor — SDK and JSON Approaches
 *
 * Demonstrates two ways to register workflows:
 *   1. JSON file: Load a workflow definition from a resource file (declarative)
 *   2. SDK: Build a WorkflowDef programmatically and register via MetadataClient (programmatic)
 *
 * Also shows version management: register v1 from JSON, register v2 from code,
 * then verify both versions exist.
 *
 * Run:
 *   java -jar target/registering-workflows-1.0.0.jar
 */
public class RegisteringWorkflowsExample {

    private static final String WORKFLOW_NAME = "registration_demo";

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Workflow Registration: SDK and JSON Approaches ===\n");

        var client = new ConductorClientHelper();
        MetadataClient metadataClient = client.getMetadataClient();

        // Step 1 — Register task definition
        System.out.println("Step 1: Registering task definition...");
        client.registerTaskDefs(List.of("echo_task"));
        System.out.println("  Registered: echo_task\n");

        // Step 2 — Method 1: Register workflow v1 from JSON resource file
        System.out.println("Step 2: Registering workflow v1 from workflow.json (declarative)...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow '" + WORKFLOW_NAME + "' v1 registered from JSON.\n");

        // Step 3 — Method 2: Register workflow v2 programmatically via SDK
        System.out.println("Step 3: Registering workflow v2 programmatically (SDK)...");
        WorkflowDef v2 = buildWorkflowDefV2();
        metadataClient.updateWorkflowDefs(List.of(v2));
        System.out.println("  Workflow '" + WORKFLOW_NAME + "' v2 registered via SDK.\n");

        // Step 4 — Verify both versions exist
        System.out.println("Step 4: Verifying registered versions...");
        boolean v1ok = false;
        boolean v2ok = false;

        try {
            WorkflowDef fetched1 = metadataClient.getWorkflowDef(WORKFLOW_NAME, 1);
            System.out.println("  v1: name=" + fetched1.getName()
                    + ", version=" + fetched1.getVersion()
                    + ", tasks=" + fetched1.getTasks().size()
                    + ", description=\"" + fetched1.getDescription() + "\"");
            v1ok = true;
        } catch (Exception e) {
            System.out.println("  v1: NOT FOUND — " + e.getMessage());
        }

        try {
            WorkflowDef fetched2 = metadataClient.getWorkflowDef(WORKFLOW_NAME, 2);
            System.out.println("  v2: name=" + fetched2.getName()
                    + ", version=" + fetched2.getVersion()
                    + ", tasks=" + fetched2.getTasks().size()
                    + ", description=\"" + fetched2.getDescription() + "\"");
            v2ok = true;
        } catch (Exception e) {
            System.out.println("  v2: NOT FOUND — " + e.getMessage());
        }
        System.out.println();

        if (!v1ok || !v2ok) {
            System.out.println("Result: FAILED (could not verify both versions)");
            System.exit(1);
        }

        // Step 5 — Start worker
        System.out.println("Step 5: Starting worker...");
        List<Worker> workers = List.of(new EchoWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 6 — Run workflow v1
        System.out.println("Step 6: Running workflow v1...\n");
        String workflowId = client.startWorkflow(WORKFLOW_NAME, 1,
                Map.of("message", "Hello from v1!"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 7 — Wait for completion
        System.out.println("Step 7: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status) && v1ok && v2ok) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }

    /**
     * Builds a v2 WorkflowDef programmatically — the SDK approach.
     * Identical structure to v1 but with an updated description to show versioning.
     */
    private static WorkflowDef buildWorkflowDefV2() {
        WorkflowTask echoTask = new WorkflowTask();
        echoTask.setName("echo_task");
        echoTask.setTaskReferenceName("echo_task_ref");
        echoTask.setType("SIMPLE");
        echoTask.setInputParameters(Map.of("message", "${workflow.input.message}"));

        WorkflowDef def = new WorkflowDef();
        def.setName(WORKFLOW_NAME);
        def.setDescription("Registration demo v2 — registered programmatically via SDK");
        def.setVersion(2);
        def.setSchemaVersion(2);
        def.setInputParameters(List.of("message"));
        def.setTasks(List.of(echoTask));
        def.setOutputParameters(Map.of("result", "${echo_task_ref.output.result}"));
        def.setOwnerEmail("examples@orkes.io");
        def.setTimeoutSeconds(60);

        return def;
    }
}
