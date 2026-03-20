package webhooksecurity.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes a verified webhook payload.
 * Input: payload
 * Output: processed (true)
 */
public class ProcessWebhookWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ws_process_webhook";
    }

    @Override
    public TaskResult execute(Task task) {
        String payload = (String) task.getInputData().get("payload");

        if (payload == null) {
            payload = "";
        }

        System.out.println("  [ws_process_webhook] Processing verified webhook payload...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        return result;
    }
}
