package dataexportrequest.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ValidateExportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "der_validate";
    }

    @Override
    public TaskResult execute(Task task) {
        String userId = (String) task.getInputData().get("userId");
        String format = (String) task.getInputData().get("exportFormat");
        System.out.println("  [validate] Export request validated for " + userId + ", format: " + format);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("valid", true);
        result.getOutputData().put("identity", "verified");
        return result;
    }
}
