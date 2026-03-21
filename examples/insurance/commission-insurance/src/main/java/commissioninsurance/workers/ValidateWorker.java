package commissioninsurance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ValidateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cin_validate";
    }

    @Override
    public TaskResult execute(Task task) {

        String agentId = (String) task.getInputData().get("agentId");
        System.out.printf("  [validate] Commission validated for agent %s%n", agentId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("valid", true);
        return result;
    }
}
