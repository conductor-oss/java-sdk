package agentswarm.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Swarm agent 1 — Market Analysis specialist.
 * Analyzes market trends, competitive landscape, and adoption rates.
 * Returns 3 findings with confidence=0.88 and sourcesConsulted=14.
 */
public class Swarm1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "as_swarm_1";
    }

    @Override
    public TaskResult execute(Task task) {
        String agentId = (String) task.getInputData().get("agentId");
        if (agentId == null || agentId.isBlank()) {
            agentId = "swarm-agent-1";
        }

        System.out.println("  [as_swarm_1] Agent " + agentId + " researching Market Analysis...");

        List<String> findings = List.of(
                "Market adoption has grown capacity-planning% year-over-year with enterprise segment leading at 62% share",
                "Three dominant players control 78% of the market, but emerging startups are disrupting niche segments",
                "Average implementation cost has decreased by 45% due to commoditization of core infrastructure"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("agentId", agentId);
        result.getOutputData().put("area", "Market Analysis");
        result.getOutputData().put("findings", findings);
        result.getOutputData().put("confidence", 0.88);
        result.getOutputData().put("sourcesConsulted", 14);
        return result;
    }
}
