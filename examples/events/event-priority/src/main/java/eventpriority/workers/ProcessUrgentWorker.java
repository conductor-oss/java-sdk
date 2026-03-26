package eventpriority.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes high-priority events in the urgent lane.
 * Input: eventId, payload
 * Output: processed (true), lane ("urgent")
 */
public class ProcessUrgentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pr_process_urgent";
    }

    @Override
    public TaskResult execute(Task task) {
        String eventId = (String) task.getInputData().get("eventId");
        if (eventId == null) {
            eventId = "unknown";
        }

        System.out.println("  [pr_process_urgent] Processing event " + eventId + " in urgent lane");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("lane", "urgent");
        return result;
    }
}
