package webhookratelimiting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Identifies the sender of an incoming webhook request.
 * Input: senderId
 * Output: senderId, ip
 */
public class IdentifySenderWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wl_identify_sender";
    }

    @Override
    public TaskResult execute(Task task) {
        String senderId = (String) task.getInputData().get("senderId");
        if (senderId == null) {
            senderId = "unknown";
        }

        System.out.println("  [wl_identify_sender] Identified sender: " + senderId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("senderId", senderId);
        result.getOutputData().put("ip", "192.168.1.100");
        return result;
    }
}
