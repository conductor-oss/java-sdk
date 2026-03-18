package googlegemini.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeminiFormatOutputWorkerTest {

    private final GeminiFormatOutputWorker worker = new GeminiFormatOutputWorker();

    @Test
    void taskDefName() {
        assertEquals("gemini_format_output", worker.getTaskDefName());
    }

    @Test
    void extractsTextFromCandidates() {
        String expectedText = "Product launch plan for Q4:\n\n1. Pre-launch phase...";

        Task task = taskWith(new HashMap<>(Map.of(
                "candidates", List.of(
                        Map.of("content", Map.of(
                                "parts", List.of(Map.of("text", expectedText)),
                                "role", "model"
                        ))
                ),
                "usageMetadata", Map.of(
                        "promptTokenCount", 78,
                        "candidatesTokenCount", 132,
                        "totalTokenCount", 210
                )
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(expectedText, result.getOutputData().get("formattedResult"));
    }

    @Test
    void handlesLongText() {
        String longText = "A".repeat(500);

        Task task = taskWith(new HashMap<>(Map.of(
                "candidates", List.of(
                        Map.of("content", Map.of(
                                "parts", List.of(Map.of("text", longText)),
                                "role", "model"
                        ))
                ),
                "usageMetadata", Map.of("totalTokenCount", 100)
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(longText, result.getOutputData().get("formattedResult"));
    }

    @Test
    void outputContainsFormattedResultField() {
        Task task = taskWith(new HashMap<>(Map.of(
                "candidates", List.of(
                        Map.of("content", Map.of(
                                "parts", List.of(Map.of("text", "Some output")),
                                "role", "model"
                        ))
                ),
                "usageMetadata", Map.of("totalTokenCount", 10)
        )));

        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("formattedResult"));
        assertTrue(result.getOutputData().get("formattedResult").toString().contains("Some output"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
