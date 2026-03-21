package multiagentcontent;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import multiagentcontent.workers.ResearchAgentWorker;
import multiagentcontent.workers.WriterAgentWorker;
import multiagentcontent.workers.SeoAgentWorker;
import multiagentcontent.workers.EditorAgentWorker;
import multiagentcontent.workers.PublishWorker;

import java.util.List;
import java.util.Map;

/**
 * Multi-Agent Content Creation Demo
 *
 * Demonstrates a sequential pipeline of five specialized agents that collaborate
 * to research, write, optimize, edit, and publish content:
 *   research -> writer -> seo -> editor -> publish
 *
 * Run:
 *   java -jar target/multi-agent-content-1.0.0.jar
 */
public class MultiAgentContentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Multi-Agent Content Creation Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "cc_research_agent", "cc_writer_agent", "cc_seo_agent",
                "cc_editor_agent", "cc_publish"));
        System.out.println("  Registered: cc_research_agent, cc_writer_agent, cc_seo_agent, cc_editor_agent, cc_publish\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'multi_agent_content_creation'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ResearchAgentWorker(),
                new WriterAgentWorker(),
                new SeoAgentWorker(),
                new EditorAgentWorker(),
                new PublishWorker()
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
        String workflowId = client.startWorkflow("multi_agent_content_creation", 1,
                Map.of("topic", "AI in Healthcare",
                        "targetAudience", "healthcare professionals",
                        "wordCount", 1000));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
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
