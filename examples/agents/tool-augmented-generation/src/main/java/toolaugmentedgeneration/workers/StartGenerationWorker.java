package toolaugmentedgeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Begins text generation from a prompt and produces partial text with a
 * knowledge gap that requires an external tool to fill.
 */
public class StartGenerationWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tg_start_generation";
    }

    @Override
    public TaskResult execute(Task task) {
        String prompt = (String) task.getInputData().get("prompt");
        if (prompt == null || prompt.isBlank()) {
            prompt = "";
        }

        System.out.println("  [tg_start_generation] Generating partial text for prompt: " + prompt);

        String partialText = "Node.js was created by Ryan Dahl and first released in 2009. Its current LTS version is";
        String gapType = "factual_lookup";
        boolean needsTool = true;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("partialText", partialText);
        result.getOutputData().put("gapType", gapType);
        result.getOutputData().put("needsTool", needsTool);
        return result;
    }
}
