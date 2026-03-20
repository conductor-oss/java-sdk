package multiagentplanning;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import multiagentplanning.workers.ArchitectDesignWorker;
import multiagentplanning.workers.EstimateFrontendWorker;
import multiagentplanning.workers.EstimateBackendWorker;
import multiagentplanning.workers.EstimateInfraWorker;
import multiagentplanning.workers.PmTimelineWorker;

import java.util.List;
import java.util.Map;

/**
 * Multi-Agent Project Planning Demo
 *
 * Demonstrates a multi-agent planning pattern where an architect designs the
 * system, three estimation agents run in parallel (frontend, backend, infra)
 * via FORK/JOIN, and a PM agent builds the consolidated timeline.
 *
 * Run:
 *   java -jar target/multi-agent-planning-1.0.0.jar
 */
public class MultiAgentPlanningExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Multi-Agent Project Planning Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "pp_architect_design",
                "pp_estimate_frontend",
                "pp_estimate_backend",
                "pp_estimate_infra",
                "pp_pm_timeline"));
        System.out.println("  Registered: pp_architect_design, pp_estimate_frontend, pp_estimate_backend, pp_estimate_infra, pp_pm_timeline\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'multi_agent_planning'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ArchitectDesignWorker(),
                new EstimateFrontendWorker(),
                new EstimateBackendWorker(),
                new EstimateInfraWorker(),
                new PmTimelineWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("multi_agent_planning", 1,
                Map.of("projectName", "E-Commerce Platform",
                        "requirements", "User authentication, product catalog, shopping cart, payment processing, order management"));
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
