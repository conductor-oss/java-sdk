package anthropicclaude.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClaudeProcessResponseWorkerTest {

    private final ClaudeProcessResponseWorker worker = new ClaudeProcessResponseWorker();

    @Test
    void taskDefName() {
        assertEquals("claude_process_response", worker.getTaskDefName());
    }

    @Test
    void extractsSingleTextBlock() {
        Task task = taskWith(new HashMap<>(Map.of(
                "apiResponse", new HashMap<>(Map.of(
                        "content", List.of(
                                new HashMap<>(Map.of("type", "text", "text", "Hello from Claude"))
                        )
                ))
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Hello from Claude", result.getOutputData().get("text"));
    }

    @Test
    void joinsMultipleTextBlocks() {
        Task task = taskWith(new HashMap<>(Map.of(
                "apiResponse", new HashMap<>(Map.of(
                        "content", List.of(
                                new HashMap<>(Map.of("type", "text", "text", "First block")),
                                new HashMap<>(Map.of("type", "text", "text", "Second block"))
                        )
                ))
        )));

        TaskResult result = worker.execute(task);

        assertEquals("First block\nSecond block", result.getOutputData().get("text"));
    }

    @Test
    void filtersOutNonTextBlocks() {
        Task task = taskWith(new HashMap<>(Map.of(
                "apiResponse", new HashMap<>(Map.of(
                        "content", List.of(
                                new HashMap<>(Map.of("type", "text", "text", "Visible text")),
                                new HashMap<>(Map.of("type", "image", "source", "data:image/png;base64,..."))
                        )
                ))
        )));

        TaskResult result = worker.execute(task);

        assertEquals("Visible text", result.getOutputData().get("text"));
    }

    @Test
    void handlesSecurityAuditResponse() {
        String auditText = "Security audit findings for the authentication module:\n\n"
                + "1. **Critical**: JWT tokens lack expiration claims";
        Task task = taskWith(new HashMap<>(Map.of(
                "apiResponse", new HashMap<>(Map.of(
                        "content", List.of(
                                new HashMap<>(Map.of("type", "text", "text", auditText))
                        )
                ))
        )));

        TaskResult result = worker.execute(task);

        assertEquals(auditText, result.getOutputData().get("text"));
        assertTrue(((String) result.getOutputData().get("text")).contains("Security audit"));
    }

    @Test
    void outputContainsTextKey() {
        Task task = taskWith(new HashMap<>(Map.of(
                "apiResponse", new HashMap<>(Map.of(
                        "content", List.of(
                                new HashMap<>(Map.of("type", "text", "text", "Test"))
                        )
                ))
        )));

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("text"));
        assertNotNull(result.getOutputData().get("text"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
