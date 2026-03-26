package aivoicecloning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GenerateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "avc_generate";
    }

    @Override
    public TaskResult execute(Task task) {

        String modelId = (String) task.getInputData().get("modelId");
        String targetText = (String) task.getInputData().get("targetText");
        System.out.printf("  [generate] Speech generated for model %s%n", modelId, targetText);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("audioId", "AUD-ai-voice-cloning-001");
        result.getOutputData().put("duration", 8.5);
        return result;
    }
}
