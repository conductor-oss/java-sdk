package aimusicgeneration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import aimusicgeneration.workers.ComposeWorker;
import aimusicgeneration.workers.ArrangeWorker;
import aimusicgeneration.workers.ProduceWorker;
import aimusicgeneration.workers.MasterWorker;
import aimusicgeneration.workers.DeliverWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 808: AI Music Generation — Compose, Arrange, Produce, Master, Deliver
 */
public class AiMusicGenerationExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 808: AI Music Generation — Compose, Arrange, Produce, Master, Deliver ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("amg_compose", "amg_arrange", "amg_produce", "amg_master", "amg_deliver"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ComposeWorker(), new ArrangeWorker(), new ProduceWorker(), new MasterWorker(), new DeliverWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("amg_music_generation", 1, Map.of("genre", "lo-fi", "mood", "relaxed", "durationSec", 180));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("trackId: %s%n", workflow.getOutput().get("trackId"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
