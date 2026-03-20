package aiimagegeneration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import aiimagegeneration.workers.PromptWorker;
import aiimagegeneration.workers.GenerateWorker;
import aiimagegeneration.workers.EnhanceWorker;
import aiimagegeneration.workers.ValidateWorker;
import aiimagegeneration.workers.DeliverWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 809: AI Image Generation — Prompt, Generate, Enhance, Validate, Deliver
 */
public class AiImageGenerationExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 809: AI Image Generation — Prompt, Generate, Enhance, Validate, Deliver ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("aig_prompt", "aig_generate", "aig_enhance", "aig_validate", "aig_deliver"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new PromptWorker(), new GenerateWorker(), new EnhanceWorker(), new ValidateWorker(), new DeliverWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("aig_image_generation", 1, Map.of("prompt", "A futuristic city at sunset", "style", "photorealistic", "resolution", "1024x1024"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("quality: %s%n", workflow.getOutput().get("quality"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
