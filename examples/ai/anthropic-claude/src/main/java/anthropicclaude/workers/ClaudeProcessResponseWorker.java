package anthropicclaude.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Processes Claude's content-block response.
 *
 * Filters content blocks for type=="text", joins their text fields,
 * and outputs the combined text.
 */
public class ClaudeProcessResponseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "claude_process_response";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> apiResponse = (Map<String, Object>) task.getInputData().get("apiResponse");
        List<Map<String, Object>> content = (List<Map<String, Object>>) apiResponse.get("content");

        List<String> textBlocks = content.stream()
                .filter(block -> "text".equals(block.get("type")))
                .map(block -> (String) block.get("text"))
                .collect(Collectors.toList());

        String fullText = String.join("\n", textBlocks);

        System.out.println("  [process] Extracted " + textBlocks.size() + " text block(s), " + fullText.length() + " chars");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("text", fullText);
        return result;
    }
}
