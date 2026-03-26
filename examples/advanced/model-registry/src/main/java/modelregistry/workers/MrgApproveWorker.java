package modelregistry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MrgApproveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mrg_approve";
    }

    @Override
    public TaskResult execute(Task task) {
        boolean valid = "pass".equals(task.getInputData().getOrDefault("validationResult", ""));
        System.out.println("  [approve] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("approved", valid);
        result.getOutputData().put("approver", "ml-team-lead@example.com");
        return result;
    }
}