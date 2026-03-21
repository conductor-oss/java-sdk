package audiotranscription;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import audiotranscription.workers.PreprocessAudioWorker;
import audiotranscription.workers.TranscribeWorker;
import audiotranscription.workers.GenerateTimestampsWorker;
import audiotranscription.workers.ExtractKeywordsWorker;

import java.util.List;
import java.util.Map;

/**
 * Audio Transcription Workflow Demo
 *
 * Demonstrates an audio transcription pipeline:
 *   au_preprocess_audio -> au_transcribe -> au_generate_timestamps -> au_extract_keywords
 *
 * Run:
 *   java -jar target/audio-transcription-1.0.0.jar
 */
public class AudioTranscriptionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Audio Transcription Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "au_preprocess_audio", "au_transcribe", "au_generate_timestamps",
                "au_extract_keywords"));
        System.out.println("  Registered: au_preprocess_audio, au_transcribe, au_generate_timestamps, au_extract_keywords\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'audio_transcription'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PreprocessAudioWorker(),
                new TranscribeWorker(),
                new GenerateTimestampsWorker(),
                new ExtractKeywordsWorker()
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
        String workflowId = client.startWorkflow("audio_transcription", 1,
                Map.of("audioUrl", "s3://media/recordings/ml-discussion-2024.wav",
                        "language", "en-US",
                        "speakerCount", 2));
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
