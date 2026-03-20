package amazonbedrock.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Parses the Bedrock response to extract the classification text.
 *
 * Input: responseBody (Claude-on-Bedrock format), metrics
 * Output: classification (extracted text from content[0].text)
 */
public class BedrockParseOutputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bedrock_parse_output";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> responseBody = (Map<String, Object>) task.getInputData().get("responseBody");
        List<Map<String, Object>> content = (List<Map<String, Object>>) responseBody.get("content");
        String text = (String) content.get(0).get("text");

        System.out.println("  [parse] Classification extracted: " + text.substring(0, Math.min(45, text.length())) + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("classification", text);
        return result;
    }
}
