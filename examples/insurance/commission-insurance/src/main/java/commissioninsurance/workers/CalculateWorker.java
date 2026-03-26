package commissioninsurance.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CalculateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cin_calculate";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [calculate] Commission calculated at 15%%");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("commission", 480.0);
        result.getOutputData().put("rate", 0.15);
        return result;
    }
}
