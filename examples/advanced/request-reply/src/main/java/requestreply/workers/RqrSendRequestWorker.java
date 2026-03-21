package requestreply.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RqrSendRequestWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rqr_send_request";
    }

    @Override
    public TaskResult execute(Task task) {
        String correlationId = "CORR-" + Long.toString(System.currentTimeMillis(), 36);
        String replyQueue = "reply-queue-" + correlationId;
        System.out.println("  [send] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("correlationId", correlationId);
        result.getOutputData().put("replyQueue", replyQueue);
        result.getOutputData().put("sentAt", java.time.Instant.now().toString());
        return result;
    }
}