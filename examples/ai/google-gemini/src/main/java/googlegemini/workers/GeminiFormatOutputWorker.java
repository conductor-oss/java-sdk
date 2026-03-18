package googlegemini.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Extracts the generated text from Gemini's candidate structure
 * and returns it as formattedResult.
 */
public class GeminiFormatOutputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gemini_format_output";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) task.getInputData().get("candidates");

        Map<String, Object> content =
                (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts =
                (List<Map<String, Object>>) content.get("parts");
        String text = (String) parts.get(0).get("text");

        System.out.println("  [fmt] Formatted result: " + text.substring(0, Math.min(50, text.length())) + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("formattedResult", text);
        return result;
    }
}
