package multimodalrag;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import multimodalrag.workers.DetectModalityWorker;
import multimodalrag.workers.ProcessTextWorker;
import multimodalrag.workers.ProcessImageWorker;
import multimodalrag.workers.ProcessAudioWorker;
import multimodalrag.workers.MultimodalSearchWorker;
import multimodalrag.workers.GenerateWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 159: Multimodal RAG — Cross-Modal Retrieval and Generation
 *
 * Detects modalities in a query, processes text/image/audio in parallel
 * via a FORK/JOIN, performs multimodal search across all modalities,
 * and generates a unified answer. Workers perform multimodal processing.
 *
 * Run:
 *   java -jar target/multimodal-rag-1.0.0.jar
 *   java -jar target/multimodal-rag-1.0.0.jar --workers
 */
public class MultimodalRagExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 159: Multimodal RAG ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "mm_detect_modality", "mm_process_text", "mm_process_image",
                "mm_process_audio", "mm_multimodal_search", "mm_generate"));
        System.out.println("  Registered: mm_detect_modality, mm_process_text, mm_process_image, mm_process_audio, mm_multimodal_search, mm_generate\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'multimodal_rag_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DetectModalityWorker(),
                new ProcessTextWorker(),
                new ProcessImageWorker(),
                new ProcessAudioWorker(),
                new MultimodalSearchWorker(),
                new GenerateWorker()
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

        // Step 4 — Start multimodal RAG workflow
        System.out.println("Step 4: Starting multimodal RAG workflow...\n");
        String workflowId = client.startWorkflow("multimodal_rag_workflow", 1,
                Map.of("question", "How does multimodal retrieval improve search quality?",
                        "attachments", List.of(
                                Map.of("type", "image", "url", "https://storage.example.com/diagram.png"),
                                Map.of("type", "audio", "url", "https://storage.example.com/presentation.wav")
                        )));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("  Waiting for completion...");
        Workflow wf = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Answer: " + wf.getOutput().get("answer"));

        System.out.println("\n--- Multimodal RAG Pattern ---");
        System.out.println("  - Detect modality: Identify text, image, and audio inputs");
        System.out.println("  - Parallel processing: FORK processes each modality concurrently");
        System.out.println("  - Multimodal search: Unified search across all modality features");
        System.out.println("  - Generate: Produce answer from cross-modal search results");

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
