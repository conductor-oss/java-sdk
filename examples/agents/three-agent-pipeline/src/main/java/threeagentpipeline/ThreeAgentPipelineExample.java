package threeagentpipeline;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import threeagentpipeline.workers.FinalOutputWorker;
import threeagentpipeline.workers.ResearcherAgentWorker;
import threeagentpipeline.workers.ReviewerAgentWorker;
import threeagentpipeline.workers.WriterAgentWorker;

import java.util.List;
import java.util.Map;

/**
 * Three-Agent Pipeline — Researcher + Writer + Reviewer
 *
 * Demonstrates a sequential multi-agent pipeline where each agent
 * builds on the output of the previous one:
 *   1. Researcher gathers facts, statistics, and sources
 *   2. Writer produces a draft article using the research
 *   3. Reviewer evaluates the draft for quality
 *   4. Final output assembles the complete report
 *
 * Run:
 *   java -jar target/three-agent-pipeline-1.0.0.jar
 *   java -jar target/three-agent-pipeline-1.0.0.jar --workers
 */
public class ThreeAgentPipelineExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Three-Agent Pipeline: Researcher + Writer + Reviewer ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "thr_researcher_agent",
                "thr_writer_agent",
                "thr_reviewer_agent",
                "thr_final_output"
        ));
        System.out.println("  Registered: thr_researcher_agent, thr_writer_agent, thr_reviewer_agent, thr_final_output\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'three_agent_pipeline'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ResearcherAgentWorker(),
                new WriterAgentWorker(),
                new ReviewerAgentWorker(),
                new FinalOutputWorker()
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
        String workflowId = client.startWorkflow("three_agent_pipeline", 1,
                Map.of(
                        "subject", "artificial intelligence in healthcare",
                        "audience", "healthcare executives"
                ));
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
