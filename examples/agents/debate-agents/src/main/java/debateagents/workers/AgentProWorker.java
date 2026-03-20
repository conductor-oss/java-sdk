package debateagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * PRO agent — argues in favor of the debate topic each round.
 * Uses deterministic argument selection based on the round input.
 */
public class AgentProWorker implements Worker {

    private static final List<String> ARGUMENTS = List.of(
            "Microservices enable independent deployment and scaling of individual components, "
                    + "allowing teams to release features faster and allocate resources precisely where demand is highest.",
            "Each microservice can use the technology stack best suited to its domain, "
                    + "empowering teams to innovate with the right tools rather than being locked into a single framework.",
            "Fault isolation in microservices means a failure in one service does not cascade to bring down "
                    + "the entire system, resulting in higher overall availability and resilience."
    );

    @Override
    public String getTaskDefName() {
        return "da_agent_pro";
    }

    @Override
    public TaskResult execute(Task task) {
        int round = parseRound(task.getInputData().get("round"));
        String topic = (String) task.getInputData().get("topic");
        if (topic == null || topic.isBlank()) {
            topic = "the given topic";
        }

        int index = (round - 1) % ARGUMENTS.size();
        String argument = ARGUMENTS.get(index);

        System.out.println("  [da_agent_pro] Round " + round + " PRO argument for: " + topic);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("side", "PRO");
        result.getOutputData().put("round", round);
        result.getOutputData().put("argument", argument);
        return result;
    }

    static int parseRound(Object roundObj) {
        if (roundObj instanceof Number) {
            return ((Number) roundObj).intValue();
        }
        if (roundObj instanceof String) {
            try {
                return Integer.parseInt((String) roundObj);
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return 1;
    }
}
