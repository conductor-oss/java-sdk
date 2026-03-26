package modelregistry.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MrgRegisterWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "mrg_register";
    }

    @Override
    public TaskResult execute(Task task) {
        String modelName = (String) task.getInputData().getOrDefault("modelName", "model");
        System.out.println("  [register] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("registryId", "REG-" + modelName + "-" + System.currentTimeMillis());
        result.getOutputData().put("artifact", task.getInputData().getOrDefault("modelArtifact", ""));
        return result;
    }
}