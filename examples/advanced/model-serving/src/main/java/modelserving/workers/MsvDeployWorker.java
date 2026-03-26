package modelserving.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MsvDeployWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "msv_deploy";
    }

    @Override
    public TaskResult execute(Task task) {
        String modelName = (String) task.getInputData().getOrDefault("modelName", "model");
        String modelVersion = (String) task.getInputData().getOrDefault("modelVersion", "1");
        String env = (String) task.getInputData().getOrDefault("environment", "staging");
        System.out.println("  [deploy] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("endpointUrl", "https://ml.example.com/" + env + "/" + modelName + "/v" + modelVersion);
        result.getOutputData().put("replicas", 2);
        return result;
    }
}