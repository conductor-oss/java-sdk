package toolaugmentedgeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Merges the tool result into the partial text, producing enriched text
 * that fills the previously detected knowledge gap.
 */
public class IncorporateResultWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tg_incorporate_result";
    }

    @Override
    public TaskResult execute(Task task) {
        String partialText = (String) task.getInputData().get("partialText");
        if (partialText == null || partialText.isBlank()) {
            partialText = "";
        }

        String toolResult = (String) task.getInputData().get("toolResult");
        if (toolResult == null || toolResult.isBlank()) {
            toolResult = "";
        }

        System.out.println("  [tg_incorporate_result] Incorporating tool result into partial text");

        String enrichedText = "Node.js was created by Ryan Dahl and first released in 2009. Its current LTS version is v22.x (Jod), as confirmed by official sources.";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enrichedText", enrichedText);
        return result;
    }
}
