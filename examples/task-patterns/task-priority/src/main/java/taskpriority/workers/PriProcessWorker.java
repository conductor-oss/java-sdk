package taskpriority.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes a request with priority awareness.
 * Takes requestId and priority, returns { processed: true }.
 */
public class PriProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pri_process";
    }

    @Override
    public TaskResult execute(Task task) {
        String requestId = (String) task.getInputData().get("requestId");
        if (requestId == null || requestId.isBlank()) {
            requestId = "unknown";
        }

        Object priorityObj = task.getInputData().get("priority");
        int priority = 0;
        if (priorityObj instanceof Number) {
            priority = ((Number) priorityObj).intValue();
        } else if (priorityObj instanceof String) {
            try {
                priority = Integer.parseInt((String) priorityObj);
            } catch (NumberFormatException ignored) {
                // default to 0
            }
        }

        System.out.println("  [pri_process] Processing request: " + requestId
                + " (priority=" + priority + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requestId", requestId);
        result.getOutputData().put("processed", true);
        return result;
    }
}
