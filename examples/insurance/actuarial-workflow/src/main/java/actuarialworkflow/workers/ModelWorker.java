package actuarialworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ModelWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "act_model";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [model] Model built — 45K records fitted");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("modelId", "MDL-actuarial-workflow-001");
        result.getOutputData().put("r2", 0.94);
        result.getOutputData().put("variables", 12);
        return result;
    }
}
