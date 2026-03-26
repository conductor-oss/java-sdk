package aifinetuning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ConfigureWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aft_configure";
    }

    @Override
    public TaskResult execute(Task task) {

        String baseModel = (String) task.getInputData().get("baseModel");
        String datasetSize = (String) task.getInputData().get("datasetSize");
        System.out.printf("  [configure] Fine-tune config: base=%s, lr=2e-5, epochs=3%n", baseModel, datasetSize);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("config", "lr-2e5-epochs-3-batch-16");
        result.getOutputData().put("learningRate", 2e-05);
        result.getOutputData().put("epochs", 3);
        return result;
    }
}
