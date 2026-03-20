package aivoicecloning;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import aivoicecloning.workers.CollectSamplesWorker;
import aivoicecloning.workers.TrainModelWorker;
import aivoicecloning.workers.GenerateWorker;
import aivoicecloning.workers.VerifyWorker;
import aivoicecloning.workers.DeliverWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 801: AI Voice Cloning — Collect Samples, Train Model, Generate, Verify, Deliver
 */
public class AiVoiceCloningExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 801: AI Voice Cloning — Collect Samples, Train Model, Generate, Verify, Deliver ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("avc_collect_samples", "avc_train_model", "avc_generate", "avc_verify", "avc_deliver"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectSamplesWorker(), new TrainModelWorker(), new GenerateWorker(), new VerifyWorker(), new DeliverWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("avc_voice_cloning", 1, Map.of("speakerId", "SPK-801", "targetText", "Welcome to our AI-powered voice platform.", "language", "en-US"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("similarity: %s%n", workflow.getOutput().get("similarity"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
