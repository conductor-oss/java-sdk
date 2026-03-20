package ollamalocal.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker that post-processes the Ollama generation response.
 * Takes response and wraps it in a review field.
 */
public class OllamaPostProcessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ollama_post_process";
    }

    @Override
    public TaskResult execute(Task task) {
        String response = (String) task.getInputData().get("response");

        if (response == null || response.isBlank()) {
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "Response is required");
            return result;
        }

        System.out.println("  [ollama_post_process] Post-processing response");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("review", response);
        return result;
    }
}
