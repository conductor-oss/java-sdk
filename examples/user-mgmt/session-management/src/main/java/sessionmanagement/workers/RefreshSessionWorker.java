package sessionmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RefreshSessionWorker implements Worker {
    @Override public String getTaskDefName() { return "ses_refresh"; }

    @Override public TaskResult execute(Task task) {
        String sessionId = (String) task.getInputData().get("sessionId");
        System.out.println("  [refresh] Session " + sessionId + " refreshed");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("refreshed", true);
        result.getOutputData().put("newExpiresIn", 3600);
        return result;
    }
}
