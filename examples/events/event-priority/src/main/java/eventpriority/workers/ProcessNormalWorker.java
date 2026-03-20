package eventpriority.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes medium-priority events in the normal lane.
 * Input: eventId, payload
 * Output: processed (true), lane ("normal")
 */
public class ProcessNormalWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pr_process_normal";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        System.out.println("  [pr_process_normal] Processing event " + eventId + " in normal lane");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("lane", "normal");
        return result;
    }
}
