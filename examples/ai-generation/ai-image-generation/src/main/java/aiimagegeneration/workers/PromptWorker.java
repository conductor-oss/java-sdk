package aiimagegeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PromptWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "aig_prompt";
    }

    @Override
    public TaskResult execute(Task task) {

        String prompt = (String) task.getInputData().get("prompt");
        String style = (String) task.getInputData().get("style");
        System.out.printf("  [prompt] Processing prompt: %s (style=%s)%n", prompt, style);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processedPrompt", "" + prompt + ", " + style + " style, highly detailed, 8k resolution");
        result.getOutputData().put("tokens", 42);
        return result;
    }
}
