package conversationalrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateWorkerTest {

    private final GenerateWorker worker = new GenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("crag_generate", worker.getTaskDefName());
    }

    @Test
    void firstTurnResponseWithoutHistory() {
        List<Map<String, Object>> context = List.of(
                new HashMap<>(Map.of("text", "Doc 1", "score", 0.93)),
                new HashMap<>(Map.of("text", "Doc 2", "score", 0.88)),
                new HashMap<>(Map.of("text", "Doc 3", "score", 0.84))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "userMessage", "What features does Conductor offer?",
                "context", context
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String response = (String) result.getOutputData().get("response");
        assertNotNull(response);
        assertTrue(response.contains("To answer your question"));
        assertTrue(response.contains("3 sources"));
        assertFalse(response.contains("Following up"));
    }

    @Test
    void followUpResponseWithHistory() {
        List<Map<String, String>> history = new ArrayList<>();
        history.add(new HashMap<>(Map.of("user", "Hello", "assistant", "Hi")));

        List<Map<String, Object>> context = List.of(
                new HashMap<>(Map.of("text", "Doc 1", "score", 0.9))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "userMessage", "Tell me more",
                "history", history,
                "context", context
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String response = (String) result.getOutputData().get("response");
        assertNotNull(response);
        assertTrue(response.contains("Following up"));
        assertTrue(response.contains("1 sources"));
        assertTrue(response.contains("1 prior turns"));
    }

    @Test
    void handlesNullHistory() {
        List<Map<String, Object>> context = List.of(
                new HashMap<>(Map.of("text", "Doc", "score", 0.9))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "userMessage", "test",
                "context", context
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String response = (String) result.getOutputData().get("response");
        assertTrue(response.contains("To answer your question"));
    }

    @Test
    void handlesNullContext() {
        Task task = taskWith(new HashMap<>(Map.of(
                "userMessage", "test"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String response = (String) result.getOutputData().get("response");
        assertTrue(response.contains("0 sources"));
    }

    @Test
    void handlesMissingUserMessage() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("response"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
