package huggingface.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Formats the raw inference output based on the task type.
 * Extracts the relevant field from the model output (e.g. summary_text
 * for summarization, label for sentiment analysis, generated_text for
 * text generation).
 */
public class HfFormatResultWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hf_format_result";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Object rawOutputObj = task.getInputData().get("rawOutput");
        String taskType = (String) task.getInputData().get("task");

        List<Map<String, Object>> output = (List<Map<String, Object>>) rawOutputObj;

        String resultText;
        if ("summarization".equals(taskType)) {
            resultText = (String) output.get(0).get("summary_text");
        } else if ("sentiment-analysis".equals(taskType)) {
            resultText = (String) output.get(0).get("label");
        } else {
            Object generatedText = output.get(0).get("generated_text");
            if (generatedText != null) {
                resultText = generatedText.toString();
            } else {
                resultText = output.toString();
            }
        }

        System.out.println("  [fmt] Formatted result: "
                + resultText.substring(0, Math.min(60, resultText.length())) + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", resultText);
        return result;
    }
}
