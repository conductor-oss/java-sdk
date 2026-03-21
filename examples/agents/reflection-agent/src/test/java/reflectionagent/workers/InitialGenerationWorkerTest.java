package reflectionagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InitialGenerationWorkerTest {

    private final InitialGenerationWorker worker = new InitialGenerationWorker();

    @Test
    void taskDefName() {
        assertEquals("rn_initial_generation", worker.getTaskDefName());
    }

    @Test
    void generatesContentForTopic() {
        Task task = taskWith(Map.of("topic", "Microservices architecture best practices"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String content = (String) result.getOutputData().get("content");
        assertNotNull(content);
        assertTrue(content.contains("Microservices"));
    }

    @Test
    void returnsQualityScore() {
        Task task = taskWith(Map.of("topic", "Microservices architecture best practices"));
        TaskResult result = worker.execute(task);

        assertEquals(0.5, result.getOutputData().get("qualityScore"));
    }

    @Test
    void contentContainsAPIReference() {
        Task task = taskWith(Map.of("topic", "Microservices"));
        TaskResult result = worker.execute(task);

        String content = (String) result.getOutputData().get("content");
        assertTrue(content.contains("APIs"));
    }

    @Test
    void contentContainsIndependentServices() {
        Task task = taskWith(Map.of("topic", "Microservices"));
        TaskResult result = worker.execute(task);

        String content = (String) result.getOutputData().get("content");
        assertTrue(content.contains("independently"));
    }

    @Test
    void handlesEmptyTopic() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", "");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("content"));
    }

    @Test
    void handlesNullTopic() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("content"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("content"));
        assertNotNull(result.getOutputData().get("qualityScore"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
