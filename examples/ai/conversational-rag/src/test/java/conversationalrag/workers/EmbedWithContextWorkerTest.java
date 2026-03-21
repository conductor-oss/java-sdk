package conversationalrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmbedWithContextWorkerTest {

    private final EmbedWithContextWorker worker = new EmbedWithContextWorker();

    @Test
    void taskDefName() {
        assertEquals("crag_embed_with_context", worker.getTaskDefName());
    }

    @Test
    void returnsFixedEmbedding() {
        Task task = taskWith(new HashMap<>(Map.of("userMessage", "What is Conductor?")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Double> embedding = (List<Double>) result.getOutputData().get("embedding");
        assertNotNull(embedding);
        assertEquals(8, embedding.size());
        assertEquals(List.of(0.1, -0.3, 0.5, 0.2, -0.8, 0.4, -0.1, 0.7), embedding);
    }

    @Test
    void contextualQueryWithoutHistory() {
        Task task = taskWith(new HashMap<>(Map.of("userMessage", "What is Conductor?")));
        TaskResult result = worker.execute(task);

        assertEquals("What is Conductor?", result.getOutputData().get("contextualQuery"));
    }

    @Test
    void contextualQueryWithHistory() {
        List<Map<String, String>> history = new ArrayList<>();
        history.add(new HashMap<>(Map.of("user", "Hello", "assistant", "Hi")));
        history.add(new HashMap<>(Map.of("user", "Tell me about workflows", "assistant", "Sure")));

        Task task = taskWith(new HashMap<>(Map.of(
                "userMessage", "More details please",
                "history", history
        )));
        TaskResult result = worker.execute(task);

        String contextualQuery = (String) result.getOutputData().get("contextualQuery");
        assertTrue(contextualQuery.contains("Hello"));
        assertTrue(contextualQuery.contains("Tell me about workflows"));
        assertTrue(contextualQuery.contains("More details please"));
    }

    @Test
    void contextualQueryUsesLastTwoTurns() {
        List<Map<String, String>> history = new ArrayList<>();
        history.add(new HashMap<>(Map.of("user", "First", "assistant", "r1")));
        history.add(new HashMap<>(Map.of("user", "Second", "assistant", "r2")));
        history.add(new HashMap<>(Map.of("user", "Third", "assistant", "r3")));

        Task task = taskWith(new HashMap<>(Map.of(
                "userMessage", "Fourth",
                "history", history
        )));
        TaskResult result = worker.execute(task);

        String contextualQuery = (String) result.getOutputData().get("contextualQuery");
        // Only last 2 turns should be included
        assertFalse(contextualQuery.contains("First"));
        assertTrue(contextualQuery.contains("Second"));
        assertTrue(contextualQuery.contains("Third"));
        assertTrue(contextualQuery.contains("Fourth"));
    }

    @Test
    void handlesNullHistory() {
        Task task = taskWith(new HashMap<>(Map.of("userMessage", "test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("test", result.getOutputData().get("contextualQuery"));
    }

    @Test
    void handlesMissingUserMessage() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("contextualQuery"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
