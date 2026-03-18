package openaigpt4.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Gpt4ExtractResultWorkerTest {

    private final Gpt4ExtractResultWorker worker = new Gpt4ExtractResultWorker();

    @Test
    void taskDefName() {
        assertEquals("gpt4_extract_result", worker.getTaskDefName());
    }

    @Test
    void extractsContentFromApiResponse() {
        Map<String, Object> apiResponse = new HashMap<>(Map.of(
                "choices", List.of(new HashMap<>(Map.of(
                        "index", 0,
                        "message", new HashMap<>(Map.of(
                                "role", "assistant",
                                "content", "This is the extracted summary."
                        )),
                        "finish_reason", "stop"
                )))
        ));

        Task task = taskWith(new HashMap<>(Map.of("apiResponse", apiResponse)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("This is the extracted summary.", result.getOutputData().get("summary"));
    }

    @Test
    void returnsEmptyStringWhenApiResponseMissing() {
        Task task = taskWith(new HashMap<>(Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("summary"));
    }

    @Test
    void returnsEmptyStringWhenChoicesEmpty() {
        Map<String, Object> apiResponse = new HashMap<>(Map.of("choices", List.of()));

        Task task = taskWith(new HashMap<>(Map.of("apiResponse", apiResponse)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("summary"));
    }

    @Test
    void extractsFromFullGpt4Response() {
        Map<String, Object> message = new HashMap<>(Map.of(
                "role", "assistant",
                "content", "Based on the quarterly data, revenue increased 12% YoY driven by strong enterprise adoption."
        ));
        Map<String, Object> choice = new HashMap<>(Map.of(
                "index", 0,
                "message", message,
                "finish_reason", "stop"
        ));
        Map<String, Object> usage = new HashMap<>(Map.of(
                "prompt_tokens", 85,
                "completion_tokens", 52,
                "total_tokens", 137
        ));
        Map<String, Object> apiResponse = new HashMap<>(Map.of(
                "id", "chatcmpl-abc123def456",
                "object", "chat.completion",
                "model", "gpt-4o-mini",
                "choices", List.of(choice),
                "usage", usage
        ));

        Task task = taskWith(new HashMap<>(Map.of("apiResponse", apiResponse)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(((String) result.getOutputData().get("summary")).contains("revenue increased 12% YoY"));
    }

    @Test
    void outputContainsSummaryKey() {
        Map<String, Object> apiResponse = new HashMap<>(Map.of(
                "choices", List.of(new HashMap<>(Map.of(
                        "message", new HashMap<>(Map.of("content", "test"))
                )))
        ));

        Task task = taskWith(new HashMap<>(Map.of("apiResponse", apiResponse)));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("summary"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
