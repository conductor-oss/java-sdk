package multiagentresearch;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import multiagentresearch.workers.DefineResearchWorker;
import multiagentresearch.workers.SearchWebWorker;
import multiagentresearch.workers.SearchPapersWorker;
import multiagentresearch.workers.SearchDatabasesWorker;
import multiagentresearch.workers.SynthesizeWorker;
import multiagentresearch.workers.WriteReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Multi-Agent Research Demo
 *
 * Demonstrates a research pipeline where a define-research agent scopes the work,
 * three search agents (web, papers, databases) run in parallel via FORK/JOIN,
 * a synthesize agent merges findings, and a write-report agent produces the final output.
 *
 * Run:
 *   java -jar target/multi-agent-research-1.0.0.jar
 */
public class MultiAgentResearchExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Multi-Agent Research Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "ra_define_research", "ra_search_web", "ra_search_papers",
                "ra_search_databases", "ra_synthesize", "ra_write_report"));
        System.out.println("  Registered: ra_define_research, ra_search_web, ra_search_papers, ra_search_databases, ra_synthesize, ra_write_report\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'multi_agent_research'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DefineResearchWorker(),
                new SearchWebWorker(),
                new SearchPapersWorker(),
                new SearchDatabasesWorker(),
                new SynthesizeWorker(),
                new WriteReportWorker()
        );
        client.startWorkers(workers);
        System.out.println("  6 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("multi_agent_research", 1,
                Map.of("topic", "Impact of large language models on software engineering",
                        "depth", "comprehensive"));
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
