package sdksetup;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import sdksetup.workers.SdkTestWorker;

import java.util.List;
import java.util.Map;

/**
 * SDK Setup — Installation, Configuration, and Smoke Test
 *
 * Demonstrates how to set up the conductor-oss Java SDK v5:
 * - Maven dependency and configuration options
 * - SDK client classes and their key methods
 * - A quick smoke test to verify everything works
 *
 * Run:
 *   java -jar target/sdk-setup-1.0.0.jar
 */
public class SdkSetupExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== SDK Setup: Installation, Configuration, and Smoke Test ===\n");

        // --- Part 1: SDK Configuration Options ---
        printSdkConfiguration();

        // --- Part 2: SDK Client Classes ---
        printSdkClients();

        // --- Part 3: Smoke Test ---
        System.out.println("--- Part 3: Quick Smoke Test ---\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definition
        System.out.println("Step 1: Registering task definition...");
        client.registerTaskDefs(List.of("sdk_test_task"));
        System.out.println("  Registered: sdk_test_task\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'sdk_setup_test'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start worker
        System.out.println("Step 3: Starting worker...");
        List<Worker> workers = List.of(new SdkTestWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("sdk_setup_test", 1,
                Map.of("check", "connectivity"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nSmoke Test Result: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nSmoke Test Result: FAILED");
            System.exit(1);
        }
    }

    private static void printSdkConfiguration() {
        System.out.println("--- Part 1: SDK Configuration Options ---\n");
        System.out.println("Maven dependency:");
        System.out.println("  <dependency>");
        System.out.println("      <groupId>org.conductoross</groupId>");
        System.out.println("      <artifactId>conductor-client</artifactId>");
        System.out.println("      <version>5.0.1</version>");
        System.out.println("  </dependency>\n");

        System.out.println("Configuration — Local OSS Conductor:");
        System.out.println("  ConductorClient client = ConductorClient.builder()");
        System.out.println("      .basePath(\"http://localhost:8080/api\")");
        System.out.println("      .build();\n");

        System.out.println("Configuration — Orkes Cloud:");
        System.out.println("  ConductorClient client = ConductorClient.builder()");
        System.out.println("      .basePath(\"https://your-cluster.orkesconductor.io/api\")");
        System.out.println("      .credentials(\"your-key-id\", \"your-key-secret\")");
        System.out.println("      .build();\n");
    }

    private static void printSdkClients() {
        System.out.println("--- Part 2: SDK Client Classes and Key Methods ---\n");

        System.out.println("MetadataClient — Define tasks and workflows:");
        System.out.println("  MetadataClient metadataClient = new MetadataClient(client);");
        System.out.println("  - registerTaskDefs(List<TaskDef>)    Register task definitions");
        System.out.println("  - updateWorkflowDefs(List<WorkflowDef>)  Create/update workflow definitions");
        System.out.println("  - getWorkflowDef(name, version)      Retrieve a workflow definition\n");

        System.out.println("WorkflowClient — Run and manage workflows:");
        System.out.println("  WorkflowClient workflowClient = new WorkflowClient(client);");
        System.out.println("  - startWorkflow(StartWorkflowRequest) Start a new workflow execution");
        System.out.println("  - getWorkflow(workflowId, includeTasks) Get workflow execution status");
        System.out.println("  - pauseWorkflow(workflowId)           Pause a running workflow");
        System.out.println("  - resumeWorkflow(workflowId)          Resume a paused workflow\n");

        System.out.println("TaskClient — Poll and update tasks:");
        System.out.println("  TaskClient taskClient = new TaskClient(client);");
        System.out.println("  - pollTask(taskType, workerId, domain) Poll for a task to execute");
        System.out.println("  - updateTask(TaskResult)               Report task completion/failure\n");

        System.out.println("TaskRunnerConfigurer — Automated worker polling:");
        System.out.println("  TaskRunnerConfigurer configurer = new TaskRunnerConfigurer.Builder(taskClient, workers)");
        System.out.println("      .withThreadCount(workerCount)");
        System.out.println("      .withSleepWhenRetry(100)");
        System.out.println("      .build();");
        System.out.println("  - init()      Start polling for tasks");
        System.out.println("  - shutdown()  Stop polling gracefully\n");
    }
}
