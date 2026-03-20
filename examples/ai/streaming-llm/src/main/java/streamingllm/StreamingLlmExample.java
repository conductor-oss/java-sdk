package streamingllm;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import streamingllm.workers.StreamCollectChunksWorker;
import streamingllm.workers.StreamPostProcessWorker;
import streamingllm.workers.StreamPrepareWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 127: Streaming LLM Responses Through Conductor
 *
 * Worker performs collecting streamed chunks from an LLM, assembling
 * them into a full response. Shows the pattern for handling streaming
 * APIs within Conductor's task-based model: collect chunks in the
 * worker, return the assembled result.
 *
 * Run:
 *   java -jar target/streaming-llm-1.0.0.jar
 */
public class StreamingLlmExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 127: Streaming LLM Responses ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("stream_prepare", "stream_collect_chunks", "stream_post_process"));
        System.out.println("  Registered: stream_prepare, stream_collect_chunks, stream_post_process\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'streaming_llm_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new StreamPrepareWorker(),
                new StreamCollectChunksWorker(),
                new StreamPostProcessWorker()
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
        String workflowId = client.startWorkflow("streaming_llm_wf", 1,
                Map.of("prompt", "Describe Conductor in one sentence",
                       "model", "gpt-4",
                       "maxTokens", 200));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        System.out.println("\n  Response: " + workflow.getOutput().get("response"));
        System.out.println("  Chunks: " + workflow.getOutput().get("chunks"));
        System.out.println("  Stream duration: " + workflow.getOutput().get("streamMs") + "ms");
        System.out.println("  Word count: " + workflow.getOutput().get("wordCount"));

        System.out.println("\n--- Streaming Pattern ---");
        System.out.println("  Worker collects chunks -> assembles full response -> returns to Conductor");
        System.out.println("  Conductor sees a single completed task with the full result");

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
