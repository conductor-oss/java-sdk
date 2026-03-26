package agentswarm.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Swarm agent 2 — Technical Landscape specialist.
 * Surveys technical approaches, architectures, and key innovations.
 * Returns 3 findings with confidence=0.91 and sourcesConsulted=11.
 */
public class Swarm2Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "as_swarm_2";
    }

    @Override
    public TaskResult execute(Task task) {
        String agentId = (String) task.getInputData().get("agentId");
        if (agentId == null || agentId.isBlank()) {
            agentId = "swarm-agent-2";
        }

        System.out.println("  [as_swarm_2] Agent " + agentId + " researching Technical Landscape...");

        List<String> findings = List.of(
                "Transformer-based architectures have become the dominant paradigm, replacing prior approaches in 94% of new deployments",
                "Retrieval-augmented generation (RAG) has emerged as the leading pattern for grounding outputs in factual data",
                "Edge deployment capabilities have improved 5x, enabling real-time inference on consumer hardware"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("agentId", agentId);
        result.getOutputData().put("area", "Technical Landscape");
        result.getOutputData().put("findings", findings);
        result.getOutputData().put("confidence", 0.91);
        result.getOutputData().put("sourcesConsulted", 11);
        return result;
    }
}
