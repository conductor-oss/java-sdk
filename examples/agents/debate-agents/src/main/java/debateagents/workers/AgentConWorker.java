package debateagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * CON agent — argues against the debate topic each round.
 * Uses deterministic argument selection based on the round input.
 */
public class AgentConWorker implements Worker {

    private static final List<String> ARGUMENTS = List.of(
            "Microservices introduce significant operational complexity — teams must manage distributed "
                    + "tracing, inter-service communication, and dozens of deployable artifacts instead of one.",
            "Network latency and data consistency challenges multiply with microservices; maintaining "
                    + "transactional integrity across services requires complex patterns like sagas or eventual consistency.",
            "The organizational overhead of microservices is substantial — each service needs its own CI/CD "
                    + "pipeline, monitoring, and on-call rotation, which can overwhelm smaller teams."
    );

    @Override
    public String getTaskDefName() {
        return "da_agent_con";
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

        System.out.println("  [da_agent_con] Round " + round + " CON argument for: " + topic);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("side", "CON");
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
