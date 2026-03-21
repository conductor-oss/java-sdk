package batchmltraining.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class BmlTrainModel2Worker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bml_train_model_2";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [train-M2] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("modelPath", "/models/gb_v1.pkl");
        result.getOutputData().put("accuracy", 0.941);
        result.getOutputData().put("f1Score", 0.935);
        return result;
    }
}