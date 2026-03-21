package aimodelevaluation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class LoadModelWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ame_load_model";
    }

    @Override
    public TaskResult execute(Task task) {

        String modelId = (String) task.getInputData().get("modelId");
        System.out.printf("  [load] Model %s loaded — 500M parameters%n", modelId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("endpoint", "http://model-server/v1/predict");
        result.getOutputData().put("params", "500M");
        return result;
    }
}
