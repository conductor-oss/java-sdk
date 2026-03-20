package eventschemavalidation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Sends an invalid event to the dead-letter queue along with its validation errors.
 *
 * Input:  { event: Map, errors: List<String> }
 * Output: { sentToDLQ: true, errors: List<String> }
 */
public class DeadLetterWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sv_dead_letter";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> event = (Map<String, Object>) task.getInputData().get("event");
        List<String> errors = (List<String>) task.getInputData().get("errors");

        if (errors == null) {
            errors = List.of();
        }

        System.out.println("  [sv_dead_letter] Sending event to DLQ with " + errors.size() + " error(s)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sentToDLQ", true);
        result.getOutputData().put("errors", errors);
        return result;
    }
}
