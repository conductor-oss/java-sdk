package taxassessment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CalculateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "txa_calculate";
    }

    @Override
    public TaskResult execute(Task task) {
        double amount = 450000 * 0.012;
        System.out.printf("  [calculate] Tax calculated: $%.2f%n", amount);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("taxAmount", amount);
        return result;
    }
}
