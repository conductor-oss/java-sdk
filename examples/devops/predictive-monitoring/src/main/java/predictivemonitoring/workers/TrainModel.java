package predictivemonitoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrainModel implements Worker {

    @Override public String getTaskDefName() { return "pdm_train_model"; }

    @Override
    public TaskResult execute(Task task) {
        Object dataPoints = task.getInputData().get("dataPoints");
        System.out.println("[pdm_train_model] Training prediction model on " + dataPoints + " data points...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("modelId", "pdm-model-20260308");
        result.getOutputData().put("accuracy", 94.2);
        result.getOutputData().put("algorithm", "prophet");
        result.getOutputData().put("trainingTimeMs", 8500);
        return result;
    }
}
