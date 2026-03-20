package streamingllm.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Formats a raw prompt into a system/user prompt pair for the LLM.
 */
public class StreamPrepareWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "stream_prepare";
    }

    @Override
    public TaskResult execute(Task task) {
        String prompt = (String) task.getInputData().get("prompt");
        String model = (String) task.getInputData().get("model");

        String formattedPrompt = "System: You are a helpful assistant.\nUser: " + prompt;

        System.out.println("  [prepare] Formatted prompt for " + model);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("formattedPrompt", formattedPrompt);
        return result;
    }
}
