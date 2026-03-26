package patchmanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import patchmanagement.workers.*;

import java.util.*;

/**
 * Patch Management — Automated Security Patch Deployment
 *
 * Pipeline: scan-vulnerabilities -> test-patch -> deploy-patch -> verify-patch
 *
 * Run:
 *   CONDUCTOR_BASE_URL=http://localhost:8080/api java -jar target/patch-management-1.0.0.jar
 */
public class PatchManagementExample {

    private static final List<String> TASK_NAMES = List.of(
            "pm_scan_vulnerabilities", "pm_test_patch", "pm_deploy_patch", "pm_verify_patch"
    );

    private static List<Worker> allWorkers() {
        return List.of(
                new ScanVulnerabilities(),
                new TestPatch(),
                new DeployPatch(),
                new VerifyPatch()
        );
    }

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Patch Management Demo: Automated Security Patch Deployment ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(TASK_NAMES);
        System.out.println("  Registered: " + String.join(", ", TASK_NAMES) + "\n");

        System.out.println("Step 2: Registering workflow 'patch_management_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = allWorkers();
        client.startWorkers(workers);
        System.out.println("  " + workers.size() + " workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode — workers are polling for tasks.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting patch management workflow...\n");
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("patchId", "PATCH-2024-001");
        input.put("severity", "critical");

        String workflowId = client.startWorkflow("patch_management_workflow", 1, input);
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for workflow to complete...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n");

        Map<String, Object> output = workflow.getOutput();
        if (output != null) {
            System.out.println("--- Patch Management Results ---");
            System.out.println("  scanResult   : " + output.get("scan_vulnerabilitiesResult"));
            System.out.println("  verifyResult : " + output.get("verify_patchResult"));
        }

        client.stopWorkers();

        if (!"COMPLETED".equals(status)) {
            System.out.println("\nResult: WORKFLOW_ERROR");
            System.exit(1);
        }

        System.out.println("\nResult: PASSED");
    }
}
