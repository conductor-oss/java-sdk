package eventpriority.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes low-priority events in the batch lane (default case).
 * Input: eventId, payload
 * Output: processed (true), lane ("batch")
 */
public class ProcessBatchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pr_process_batch";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        System.out.println("  [pr_process_batch] Processing event " + eventId + " in batch lane");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("lane", "batch");
        return result;
    }
}
