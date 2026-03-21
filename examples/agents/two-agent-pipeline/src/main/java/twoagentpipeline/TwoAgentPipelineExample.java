package twoagentpipeline;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import twoagentpipeline.workers.WriterAgentWorker;
import twoagentpipeline.workers.EditorAgentWorker;
import twoagentpipeline.workers.FinalOutputWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 166: Two-Agent Pipeline -- Writer + Editor
 *
 * Sequential pipeline: a writer agent drafts content about a topic,
 * an editor agent refines it, and a final output worker assembles the result.
 *
 * Run:
 *   java -jar target/two-agent-pipeline-1.0.0.jar
 *   java -jar target/two-agent-pipeline-1.0.0.jar --workers
 */
public class TwoAgentPipelineExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 166: Two-Agent Pipeline -- Writer + Editor ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "tap_writer_agent", "tap_editor_agent", "tap_final_output"));
        System.out.println("  Registered: tap_writer_agent, tap_editor_agent, tap_final_output\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'two_agent_pipeline'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new WriterAgentWorker(),
                new EditorAgentWorker(),
                new FinalOutputWorker()
        );
        client.startWorkers(workers);
        System.out.println("  3 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("two_agent_pipeline", 1, Map.of(
                "topic", "artificial intelligence",
                "tone", "professional",
                "writerSystemPrompt", "You are a skilled technical writer.",
                "editorSystemPrompt", "You are a meticulous editor."
        ));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Final content: " + workflow.getOutput().get("finalContent"));
        System.out.println("  Summary: " + workflow.getOutput().get("summary"));

        System.out.println("\n--- Two-Agent Pipeline Pattern ---");
        System.out.println("  - Writer agent: Drafts content about a topic");
        System.out.println("  - Editor agent: Refines the draft for clarity and tone");
        System.out.println("  - Final output: Assembles result with metadata");

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
