package sessionmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ValidateSessionWorker implements Worker {
    @Override public String getTaskDefName() { return "ses_validate"; }

    @Override public TaskResult execute(Task task) {
        String sessionId = (String) task.getInputData().get("sessionId");
        System.out.println("  [validate] Session " + sessionId + " is valid");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("valid", true);
        result.getOutputData().put("remainingTtl", 3200);
        return result;
    }
}
