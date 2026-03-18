package firstaiworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker that prepares a formatted prompt from a question and model name.
 */
public class AiPreparePromptWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ai_prepare_prompt";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        String model = (String) task.getInputData().get("model");

        if (question == null || question.isBlank()) {
            question = "Hello";
        }
        if (model == null || model.isBlank()) {
            model = "gpt-4";
        }

        String formattedPrompt = "You are a helpful assistant. Using " + model
                + ", please answer: " + question;

        System.out.println("  [ai_prepare_prompt worker] Prepared prompt for model " + model);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("formattedPrompt", formattedPrompt);
        return result;
    }
}
