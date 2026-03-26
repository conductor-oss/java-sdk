package reflectionagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReflectWorkerTest {

    private final ReflectWorker worker = new ReflectWorker();

    @Test
    void taskDefName() {
        assertEquals("rn_reflect", worker.getTaskDefName());
    }

    @Test
    void firstIterationFeedback() {
        Task task = taskWith(Map.of("topic", "Microservices", "iteration", 1));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String feedback = (String) result.getOutputData().get("feedback");
        assertTrue(feedback.contains("shallow"));
    }

    @Test
    void firstIterationScore() {
        Task task = taskWith(Map.of("topic", "Microservices", "iteration", 1));
        TaskResult result = worker.execute(task);

        assertEquals(0.5, result.getOutputData().get("currentScore"));
    }

    @Test
    void secondIterationFeedback() {
        Task task = taskWith(Map.of("topic", "Microservices", "iteration", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String feedback = (String) result.getOutputData().get("feedback");
        assertTrue(feedback.contains("deployment patterns"));
    }

    @Test
    void secondIterationScore() {
        Task task = taskWith(Map.of("topic", "Microservices", "iteration", 2));
        TaskResult result = worker.execute(task);

        assertEquals(0.7, result.getOutputData().get("currentScore"));
    }

    @Test
    void handlesEmptyTopic() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", "");
        input.put("iteration", 1);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("feedback"));
    }

    @Test
    void handlesNullTopic() {
        Map<String, Object> input = new HashMap<>();
        input.put("topic", null);
        input.put("iteration", 1);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("feedback"));
    }

    @Test
    void handlesMissingIteration() {
        Task task = taskWith(Map.of("topic", "Microservices"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        // defaults to iteration 1
        assertEquals(0.5, result.getOutputData().get("currentScore"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("feedback"));
        assertNotNull(result.getOutputData().get("currentScore"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
