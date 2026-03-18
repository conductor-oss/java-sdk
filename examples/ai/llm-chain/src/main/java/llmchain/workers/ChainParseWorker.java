package llmchain.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Worker 3: Parses rawText JSON string into a Map.
 * Returns FAILED status if parsing fails.
 */
public class ChainParseWorker implements Worker {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String getTaskDefName() {
        return "chain_parse";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        String rawText = (String) task.getInputData().get("rawText");

        TaskResult result = new TaskResult(task);

        try {
            Map<String, Object> parsedData = MAPPER.readValue(rawText, Map.class);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("parsedData", parsedData);
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.getOutputData().put("error", "Failed to parse JSON: " + e.getMessage());
        }

        return result;
    }
}
