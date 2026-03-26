package networkautomation;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import networkautomation.workers.*;

import java.util.*;

/**
 * Network Automation — Automated Network Configuration Management
 *
 * Pipeline: audit -> plan-changes -> apply-config -> verify-connectivity
 *
 * Run:
 *   CONDUCTOR_BASE_URL=http://localhost:8080/api java -jar target/network-automation-1.0.0.jar
 */
public class NetworkAutomationExample {

    private static final List<String> TASK_NAMES = List.of(
            "na_audit", "na_plan_changes", "na_apply_config", "na_verify_connectivity"
    );

    private static List<Worker> allWorkers() {
        return List.of(
                new AuditNetwork(),
                new PlanChanges(),
                new ApplyConfig(),
                new VerifyConnectivity()
        );
    }

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Network Automation Demo: Automated Network Configuration Management ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(TASK_NAMES);
        System.out.println("  Registered: " + String.join(", ", TASK_NAMES) + "\n");

        System.out.println("Step 2: Registering workflow 'network_automation_workflow'...");
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

        System.out.println("Step 4: Starting network automation workflow...\n");
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("network", "prod-datacenter");
        input.put("changeType", "firewall-update");

        String workflowId = client.startWorkflow("network_automation_workflow", 1, input);
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for workflow to complete...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n");

        Map<String, Object> output = workflow.getOutput();
        if (output != null) {
            System.out.println("--- Network Automation Results ---");
            System.out.println("  auditResult              : " + output.get("auditResult"));
            System.out.println("  verify_connectivityResult: " + output.get("verify_connectivityResult"));
        }

        client.stopWorkers();

        if (!"COMPLETED".equals(status)) {
            System.out.println("\nResult: WORKFLOW_ERROR");
            System.exit(1);
        }

        System.out.println("\nResult: PASSED");
    }
}
