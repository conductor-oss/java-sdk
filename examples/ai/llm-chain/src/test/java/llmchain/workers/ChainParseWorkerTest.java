package llmchain.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChainParseWorkerTest {

    private final ChainParseWorker worker = new ChainParseWorker();

    @Test
    void taskDefName() {
        assertEquals("chain_parse", worker.getTaskDefName());
    }

    @Test
    void parsesValidJson() {
        String rawText = "{\"intent\":\"complaint\",\"sentiment\":\"negative\","
                + "\"suggestedProducts\":[\"PROD-ENT-500\"],\"draftReply\":\"Hello there\"}";

        Task task = taskWith(new HashMap<>(Map.of(
                "rawText", rawText,
                "expectedFormat", Map.of("intent", "string")
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> parsedData =
                (Map<String, Object>) result.getOutputData().get("parsedData");
        assertNotNull(parsedData);
        assertEquals("complaint", parsedData.get("intent"));
        assertEquals("negative", parsedData.get("sentiment"));
        assertEquals(List.of("PROD-ENT-500"), parsedData.get("suggestedProducts"));
        assertEquals("Hello there", parsedData.get("draftReply"));
    }

    @Test
    void failsOnInvalidJson() {
        Task task = taskWith(new HashMap<>(Map.of(
                "rawText", "not valid json {{{",
                "expectedFormat", Map.of("intent", "string")
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertNotNull(result.getOutputData().get("error"));
    }

    @Test
    void parsesComplexNestedJson() {
        String rawText = "{\"intent\":\"inquiry\",\"sentiment\":\"positive\","
                + "\"suggestedProducts\":[\"A\",\"B\",\"C\"],\"draftReply\":\"Thank you for reaching out\"}";

        Task task = taskWith(new HashMap<>(Map.of(
                "rawText", rawText,
                "expectedFormat", Map.of("intent", "string")
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> parsedData =
                (Map<String, Object>) result.getOutputData().get("parsedData");
        @SuppressWarnings("unchecked")
        List<String> products = (List<String>) parsedData.get("suggestedProducts");
        assertEquals(3, products.size());
        assertEquals(List.of("A", "B", "C"), products);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
