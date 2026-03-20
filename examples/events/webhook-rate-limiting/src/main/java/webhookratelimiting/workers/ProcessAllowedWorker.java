package webhookratelimiting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes an allowed webhook request.
 * Input: senderId, payload
 * Output: processed (true), senderId
 */
public class ProcessAllowedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wl_process_allowed";
    }

    @Override
    public TaskResult execute(Task task) {
        String senderId = (String) task.getInputData().get("senderId");
        if (senderId == null) {
            senderId = "unknown";
        }

        System.out.println("  [wl_process_allowed] Processing webhook from " + senderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("senderId", senderId);
        return result;
    }
}
