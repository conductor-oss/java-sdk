package messagebroker.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MbrReceiveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mbr_receive";
    }

    @Override
    public TaskResult execute(Task task) {
        String msgId = "BRK-" + Long.toString(System.currentTimeMillis(), 36);
        System.out.println("  [receive] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("messageId", msgId);
        result.getOutputData().put("receivedAt", java.time.Instant.now().toString());
        return result;
    }
}