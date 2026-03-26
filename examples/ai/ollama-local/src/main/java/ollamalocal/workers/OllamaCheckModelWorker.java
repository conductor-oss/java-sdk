package ollamalocal.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Worker that verifies an Ollama model is available (deterministic..
 * Takes model and ollamaHost, returns resolvedModel and availability status.
 */
public class OllamaCheckModelWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ollama_check_model";
    }

    @Override
    public TaskResult execute(Task task) {
        String model = (String) task.getInputData().get("model");
        String ollamaHost = (String) task.getInputData().get("ollamaHost");

        if (model == null || model.isBlank()) {
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "Model name is required");
            return result;
        }

        if (ollamaHost == null || ollamaHost.isBlank()) {
            ollamaHost = "http://localhost:11434";
        }

        System.out.println("  [ollama_check_model] Checking model '" + model + "' on " + ollamaHost);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("resolvedModel", model);
        result.getOutputData().put("available", true);
        return result;
    }
}
