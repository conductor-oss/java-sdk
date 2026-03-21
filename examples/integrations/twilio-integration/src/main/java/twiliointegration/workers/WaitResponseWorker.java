package twiliointegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Waits for an SMS response.
 * Input: messageSid, toNumber
 * Output: responseBody, receivedAt
 */
public class WaitResponseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "twl_wait_response";
    }

    @Override
    public TaskResult execute(Task task) {
        String toNumber = (String) task.getInputData().get("toNumber");
        String responseBody = "YES";
        System.out.println("  [wait] Response from " + toNumber + ": \"" + responseBody + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("responseBody", responseBody);
        result.getOutputData().put("receivedAt", java.time.Instant.now().toString());
        return result;
    }
}
