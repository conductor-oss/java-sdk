package aivoicecloning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrainModelWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "avc_train_model";
    }

    @Override
    public TaskResult execute(Task task) {

        String samples = (String) task.getInputData().get("samples");
        String speakerId = (String) task.getInputData().get("speakerId");
        System.out.printf("  [train] Voice model trained on %s samples%n", samples, speakerId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("modelId", "VMDL-ai-voice-cloning-001");
        result.getOutputData().put("epochs", 500);
        result.getOutputData().put("loss", 0.023);
        return result;
    }
}
