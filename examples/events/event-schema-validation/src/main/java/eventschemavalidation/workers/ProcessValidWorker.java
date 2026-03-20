package eventschemavalidation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Processes a valid event that has passed schema validation.
 *
 * Input:  { event: Map, schema: String }
 * Output: { processed: true }
 */
public class ProcessValidWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sv_process_valid";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> event = (Map<String, Object>) task.getInputData().get("event");
        String schema = (String) task.getInputData().get("schema");

        System.out.println("  [sv_process_valid] Processing valid event with schema: " + schema);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        return result;
    }
}
