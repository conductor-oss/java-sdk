package artifactmanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import artifactmanagement.workers.BuildWorker;
import artifactmanagement.workers.SignWorker;
import artifactmanagement.workers.PublishWorker;
import artifactmanagement.workers.CleanupWorker;

import java.util.List;
import java.util.Map;

/**
 * Artifact Management Workflow Demo
 *
 * Demonstrates a build artifact lifecycle:
 *   am_build -> am_sign -> am_publish -> am_cleanup
 *
 * Run:
 *   java -jar target/artifact-management-1.0.0.jar
 */
public class ArtifactManagementExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Artifact Management Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("am_build", "am_sign", "am_publish", "am_cleanup"));
        System.out.println("  Registered: am_build, am_sign, am_publish, am_cleanup\n");

        System.out.println("Step 2: Registering workflow 'artifact_management_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new BuildWorker(),
                new SignWorker(),
                new PublishWorker(),
                new CleanupWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("artifact_management_workflow", 1,
                Map.of("project", "auth-service", "version", "4.1.0"));
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
