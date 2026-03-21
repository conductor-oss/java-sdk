package serverlessorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Aggregates results from the serverless function chain.
 * Input: eventId, score, enriched
 * Output: aggregated, stored
 */
public class SvlAggregateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "svl_aggregate";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }
        Object score = task.getInputData().get("score");

        System.out.println("  [aggregate] Event " + eventId + ": score=" + score);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("aggregated", true);
        result.getOutputData().put("stored", true);
        return result;
    }
}
