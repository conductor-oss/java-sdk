package legalbilling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SendWorker implements Worker {
    @Override public String getTaskDefName() { return "lgb_send"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [send] Sending invoice " + task.getInputData().get("invoiceId") + " to client " + task.getInputData().get("clientId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("sent", true);
        result.getOutputData().put("sentAt", java.time.Instant.now().toString());
        return result;
    }
}
