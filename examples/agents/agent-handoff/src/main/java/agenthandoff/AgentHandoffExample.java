package agenthandoff;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import agenthandoff.workers.TriageWorker;
import agenthandoff.workers.BillingWorker;
import agenthandoff.workers.TechWorker;
import agenthandoff.workers.GeneralWorker;

import java.util.List;
import java.util.Map;

/**
 * Agent Handoff Demo — Triage and Route to Specialist
 *
 * Demonstrates a pattern where a triage agent classifies an incoming customer
 * message, then a SWITCH task routes it to the appropriate specialist agent
 * (billing, technical, or general support).
 *
 * Run:
 *   java -jar target/agent-handoff-1.0.0.jar
 */
public class AgentHandoffExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Agent Handoff Demo: Triage and Route to Specialist ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ah_triage", "ah_billing", "ah_tech", "ah_general"));
        System.out.println("  Registered: ah_triage, ah_billing, ah_tech, ah_general\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'agent_handoff'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new TriageWorker(),
                new BillingWorker(),
                new TechWorker(),
                new GeneralWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("agent_handoff", 1,
                Map.of("customerId", "CUST-9042",
                        "message", "I keep getting timeout errors when calling the API"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
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
