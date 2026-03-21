package aifinetuning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrainWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aft_train";
    }

    @Override
    public TaskResult execute(Task task) {

        String config = (String) task.getInputData().get("config");
        String datasetPath = (String) task.getInputData().get("datasetPath");
        System.out.println("  [train] Training complete — 3 epochs, final loss: 0.142");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("modelId", "FT-ai-fine-tuning-001");
        result.getOutputData().put("checkpointId", "CKP-ai-fine-tuning-E3");
        result.getOutputData().put("finalLoss", 0.142);
        result.getOutputData().put("trainingTime", "2h 15m");
        return result;
    }
}
