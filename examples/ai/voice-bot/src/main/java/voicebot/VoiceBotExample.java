package voicebot;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import voicebot.workers.*;
import java.util.List;
import java.util.Map;
public class VoiceBotExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 632: Voice Bot ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("vb_transcribe", "vb_understand", "vb_generate", "vb_synthesize"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new TranscribeWorker(), new UnderstandWorker(), new GenerateWorker(), new SynthesizeWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("vb_voice_bot", 1,
                Map.of("audioUrl", "https://calls.example.com/recording-632.wav", "callerId", "+1-555-0199", "language", "en-US"));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
