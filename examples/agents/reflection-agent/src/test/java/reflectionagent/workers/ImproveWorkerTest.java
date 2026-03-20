package reflectionagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ImproveWorkerTest {

    private final ImproveWorker worker = new ImproveWorker();

    @Test
    void taskDefName() {
        assertEquals("rn_improve", worker.getTaskDefName());
    }

    @Test
    void improvesBasedOnFeedback() {
        Task task = taskWith(Map.of(
                "feedback", "Too shallow — needs concrete benefits",
                "iteration", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String content = (String) result.getOutputData().get("content");
        assertNotNull(content);
        assertTrue(content.contains("Improved"));
    }

    @Test
    void returnsAppliedFlag() {
        Task task = taskWith(Map.of(
                "feedback", "Needs more depth",
                "iteration", 1));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("applied"));
    }

    @Test
    void contentContainsTradeOffs() {
        Task task = taskWith(Map.of(
                "feedback", "Add trade-offs",
                "iteration", 1));
        TaskResult result = worker.execute(task);

        String content = (String) result.getOutputData().get("content");
        assertTrue(content.contains("trade-offs"));
    }

    @Test
    void contentContainsPatterns() {
        Task task = taskWith(Map.of(
                "feedback", "Add patterns",
                "iteration", 2));
        TaskResult result = worker.execute(task);

        String content = (String) result.getOutputData().get("content");
        assertTrue(content.contains("patterns"));
    }

    @Test
    void handlesEmptyFeedback() {
        Map<String, Object> input = new HashMap<>();
        input.put("feedback", "");
        input.put("iteration", 1);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("content"));
    }

    @Test
    void handlesNullFeedback() {
        Map<String, Object> input = new HashMap<>();
        input.put("feedback", null);
        input.put("iteration", 1);
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
        assertNotNull(result.getOutputData().get("applied"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
