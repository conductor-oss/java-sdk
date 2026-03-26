package reinsurance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ConfirmWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rin_confirm";
    }

    @Override
    public TaskResult execute(Task task) {

        String cessionId = (String) task.getInputData().get("cessionId");
        System.out.printf("  [confirm] Reinsurer confirmed cession %s%n", cessionId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("confirmed", true);
        result.getOutputData().put("confirmationDate", "2024-03-10");
        return result;
    }
}
