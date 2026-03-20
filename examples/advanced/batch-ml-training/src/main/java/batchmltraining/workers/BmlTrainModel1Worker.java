package batchmltraining.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class BmlTrainModel1Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bml_train_model_1";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [train-M1] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("modelPath", "/models/rf_v1.pkl");
        result.getOutputData().put("accuracy", 0.923);
        result.getOutputData().put("f1Score", 0.918);
        return result;
    }
}