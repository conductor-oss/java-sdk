package huggingface.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Selects the appropriate Hugging Face model based on the requested task type.
 * Maps task types (summarization, text-generation, etc.) to specific model IDs
 * and configures inference parameters.
 */
public class HfSelectModelWorker implements Worker {

    private static final Map<String, String> TASK_MODEL_MAP = Map.of(
            "text-generation", "mistralai/Mixtral-8x7B-Instruct-v0.1",
            "summarization", "facebook/bart-large-cnn",
            "sentiment-analysis", "distilbert-base-uncased-finetuned-sst-2-english",
            "translation", "Helsinki-NLP/opus-mt-en-fr"
    );

    @Override
    public String getTaskDefName() {
        return "hf_select_model";
    }

    @Override
    public TaskResult execute(Task task) {
        String taskType = (String) task.getInputData().get("task");
        String text = (String) task.getInputData().get("text");

        String modelId = TASK_MODEL_MAP.getOrDefault(taskType,
                TASK_MODEL_MAP.get("text-generation"));

        Map<String, Object> parameters = Map.of(
                "max_new_tokens", 250,
                "temperature", 0.6,
                "top_p", 0.9,
                "repetition_penalty", 1.1,
                "do_sample", true
        );

        Map<String, Object> options = Map.of(
                "wait_for_model", true,
                "use_cache", false
        );

        System.out.println("  [select] Task: " + taskType + " -> Model: " + modelId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("modelId", modelId);
        result.getOutputData().put("formattedInput", text);
        result.getOutputData().put("parameters", parameters);
        result.getOutputData().put("options", options);
        return result;
    }
}
