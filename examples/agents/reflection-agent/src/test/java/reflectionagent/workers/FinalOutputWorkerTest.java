package reflectionagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinalOutputWorkerTest {

    private final FinalOutputWorker worker = new FinalOutputWorker();

    @Test
    void taskDefName() {
        assertEquals("rn_final_output", worker.getTaskDefName());
    }

    @Test
    void producesFinalContent() {
        Task task = taskWith(Map.of("topic", "Microservices architecture best practices", "totalIterations", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String content = (String) result.getOutputData().get("content");
        assertNotNull(content);
        assertTrue(content.contains("Microservices"));
    }

    @Test
    void returnsHighQualityScore() {
        Task task = taskWith(Map.of("topic", "Microservices", "totalIterations", 2));
        TaskResult result = worker.execute(task);

        assertEquals(0.92, result.getOutputData().get("qualityScore"));
    }

    @Test
    void contentMentionsDeployableServices() {
        Task task = taskWith(Map.of("topic", "Microservices", "totalIterations", 2));
        TaskResult result = worker.execute(task);

        String content = (String) result.getOutputData().get("content");
        assertTrue(content.contains("deployable services"));
    }

    @Test
    void contentMentionsMonitoring() {
        Task task = taskWith(Map.of("topic", "Microservices", "totalIterations", 2));
        TaskResult result = worker.execute(task);

        String content = (String) result.getOutputData().get("content");
        assertTrue(content.contains("monitoring"));
    }

    @Test
    void contentMentionsFaultIsolation() {
        Task task = taskWith(Map.of("topic", "Microservices", "totalIterations", 2));
        TaskResult result = worker.execute(task);

        String content = (String) result.getOutputData().get("content");
        assertTrue(content.contains("fault isolation"));
    }

    @Test
    void handlesEmptyTopic() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", "");
        input.put("totalIterations", 2);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("content"));
    }

    @Test
    void handlesNullTopic() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", null);
        input.put("totalIterations", 2);
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
