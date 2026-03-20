package multifactorauth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class SelectMethodWorker implements Worker {
    @Override public String getTaskDefName() { return "mfa_select_method"; }

    @Override public TaskResult execute(Task task) {
        String method = (String) task.getInputData().get("preferred");
        if (method == null) method = "totp";
        System.out.println("  [method] MFA method selected: " + method);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("selectedMethod", method);
        result.getOutputData().put("available", List.of("totp", "sms", "email"));
        return result;
    }
}
