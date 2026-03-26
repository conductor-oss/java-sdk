package debateagents.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Moderator agent — summarizes the debate and delivers a verdict after all rounds.
 */
public class ModeratorSummarizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "da_moderator_summarize";
    }

    @Override
    public TaskResult execute(Task task) {
        String topic = (String) task.getInputData().get("topic");
        if (topic == null || topic.isBlank()) {
            topic = "the given topic";
        }

        int totalRounds = 3;
        Object roundsObj = task.getInputData().get("totalRounds");
        if (roundsObj instanceof Number) {
            totalRounds = ((Number) roundsObj).intValue();
        } else if (roundsObj instanceof String) {
            try {
                totalRounds = Integer.parseInt((String) roundsObj);
            } catch (NumberFormatException e) {
                // keep default
            }
        }

        System.out.println("  [da_moderator_summarize] Summarizing " + totalRounds
                + " rounds on: " + topic);

        String summary = String.format(
                "After %d rounds of debate on '%s', both sides presented compelling arguments. "
                        + "The PRO side highlighted benefits such as independent scaling, technology flexibility, "
                        + "and fault isolation. The CON side raised valid concerns about operational complexity, "
                        + "data consistency challenges, and organizational overhead.",
                totalRounds, topic);

        String verdict = String.format(
                "The debate on '%s' is nuanced. Microservices offer clear advantages for large, "
                        + "complex systems with multiple teams, but the trade-offs in complexity and "
                        + "operational cost mean they are not universally the best choice. "
                        + "The decision should be driven by organizational maturity and system scale.",
                topic);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("verdict", verdict);
        return result;
    }
}
