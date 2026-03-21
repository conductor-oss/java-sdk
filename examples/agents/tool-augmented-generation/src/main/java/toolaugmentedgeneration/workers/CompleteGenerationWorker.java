package toolaugmentedgeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Finalises text generation by appending remaining content to the enriched
 * text and reporting total token count.
 */
public class CompleteGenerationWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tg_complete_generation";
    }

    @Override
    public TaskResult execute(Task task) {
        String enrichedText = (String) task.getInputData().get("enrichedText");
        if (enrichedText == null || enrichedText.isBlank()) {
            enrichedText = "";
        }

        String prompt = (String) task.getInputData().get("prompt");
        if (prompt == null || prompt.isBlank()) {
            prompt = "";
        }

        System.out.println("  [tg_complete_generation] Completing generation for prompt: " + prompt);

        String finalText = enrichedText
                + " Node.js uses an event-driven, non-blocking I/O model and is widely used for building scalable server-side applications and APIs.";
        int totalTokens = 87;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("finalText", finalText);
        result.getOutputData().put("totalTokens", totalTokens);
        return result;
    }
}
