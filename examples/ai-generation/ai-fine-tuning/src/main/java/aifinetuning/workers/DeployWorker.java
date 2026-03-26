package aifinetuning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DeployWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aft_deploy";
    }

    @Override
    public TaskResult execute(Task task) {

        String modelId = (String) task.getInputData().get("modelId");
        System.out.printf("  [deploy] Model %s deployed to production%n", modelId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("endpoint", "https://api.example.com/v1/ft-804");
        result.getOutputData().put("deployed", true);
        result.getOutputData().put("replicas", 3);
        return result;
    }
}
