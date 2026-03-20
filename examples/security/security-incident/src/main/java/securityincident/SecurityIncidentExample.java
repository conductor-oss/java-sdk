package securityincident;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import securityincident.workers.TriageWorker;
import securityincident.workers.ContainWorker;
import securityincident.workers.InvestigateWorker;
import securityincident.workers.RemediateWorker;

import java.util.List;
import java.util.Map;

/**
 * Security Incident Response Orchestration
 *
 * Pattern: triage -> contain -> investigate -> remediate
 *
 * Run:
 *   java -jar target/security-incident-1.0.0.jar
 */
public class SecurityIncidentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Security Incident Response Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "si_triage", "si_contain", "si_investigate", "si_remediate"));
        System.out.println("  Registered: si_triage, si_contain, si_investigate, si_remediate\n");

        System.out.println("Step 2: Registering workflow 'security_incident_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new TriageWorker(),
                new ContainWorker(),
                new InvestigateWorker(),
                new RemediateWorker()
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
        String workflowId = client.startWorkflow("security_incident_workflow", 1,
                Map.of("incidentType", "unauthorized-access",
                        "severity", "P1",
                        "affectedSystem", "api-gateway"));
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
