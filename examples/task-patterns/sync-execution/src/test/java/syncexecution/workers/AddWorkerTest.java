package syncexecution.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AddWorkerTest {

    private final AddWorker worker = new AddWorker();

    @Test
    void taskDefName() {
        assertEquals("sync_add", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("a", 3, "b", 5));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void addsIntegerInputs() {
        Task task = taskWith(Map.of("a", 7, "b", 35));
        TaskResult result = worker.execute(task);

        assertEquals(42.0, ((Number) result.getOutputData().get("sum")).doubleValue());
    }

    @Test
    void addsDoubleInputs() {
        Task task = taskWith(Map.of("a", 1.5, "b", 2.5));
        TaskResult result = worker.execute(task);

        assertEquals(4.0, ((Number) result.getOutputData().get("sum")).doubleValue());
    }

    @Test
    void addsNegativeNumbers() {
        Task task = taskWith(Map.of("a", -10, "b", 3));
        TaskResult result = worker.execute(task);

        assertEquals(-7.0, ((Number) result.getOutputData().get("sum")).doubleValue());
    }

    @Test
    void handlesZeroInputs() {
        Task task = taskWith(Map.of("a", 0, "b", 0));
        TaskResult result = worker.execute(task);

        assertEquals(0.0, ((Number) result.getOutputData().get("sum")).doubleValue());
    }

    @Test
    void handlesNullA() {
        Map<String, Object> input = new HashMap<>();
        input.put("a", null);
        input.put("b", 5);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(5.0, ((Number) result.getOutputData().get("sum")).doubleValue());
    }

    @Test
    void handlesNullB() {
        Map<String, Object> input = new HashMap<>();
        input.put("a", 10);
        input.put("b", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(10.0, ((Number) result.getOutputData().get("sum")).doubleValue());
    }

    @Test
    void handlesBothNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("a", null);
        input.put("b", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0.0, ((Number) result.getOutputData().get("sum")).doubleValue());
    }

    @Test
    void outputContainsSumKey() {
        Task task = taskWith(Map.of("a", 1, "b", 2));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("sum"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
