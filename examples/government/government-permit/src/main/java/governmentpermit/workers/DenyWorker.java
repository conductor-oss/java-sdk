package governmentpermit.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DenyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gvp_deny";
    }

    @Override
    public TaskResult execute(Task task) {
        String reason = (String) task.getInputData().get("reason");
        System.out.printf("  [deny] Permit denied: %s%n", reason);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("denied", true);
        return result;
    }
}
