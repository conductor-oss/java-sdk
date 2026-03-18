package cicdpipeline;

import com.netflix.conductor.client.worker.Worker;
import cicdpipeline.workers.*;

import java.util.*;

/**
 * CI/CD Pipeline — Build, Test, Deploy Orchestration
 *
 * Orchestrates a complete CI/CD pipeline: build, unit test, integration
 * test, security scan, deploy to staging, and promote to production.
 *
 * Uses conductor-oss Java SDK v5.
 *
 * Run:
 *   CONDUCTOR_BASE_URL=http://localhost:8080/api java -jar target/ci-cd-pipeline-1.0.0.jar
 */
public class CiCdPipelineExample {

    private static final List<String> TASK_NAMES = List.of(
            "cicd_build",
            "cicd_unit_test",
            "cicd_integration_test",
            "cicd_security_scan",
            "cicd_deploy_staging",
            "cicd_deploy_prod"
    );

    private static List<Worker> allWorkers() {
        return List.of(
                new Build(),
                new UnitTest(),
                new IntegrationTest(),
                new SecurityScan(),
                new DeployStaging(),
                new DeployProd()
        );
    }

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== CI/CD Pipeline Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(TASK_NAMES);

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = allWorkers();
        client.startWorkers(workers);

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("repoUrl", "github.com/acme/api");
        input.put("branch", "main");
        input.put("commitSha", "abc1234def5678");

        String workflowId = client.startWorkflow("cicd_pipeline_workflow", 1, input);
        System.out.println("  Workflow ID: " + workflowId);

        var workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        System.out.println("  Status: " + workflow.getStatus().name());

        client.stopWorkers();
    }
}
