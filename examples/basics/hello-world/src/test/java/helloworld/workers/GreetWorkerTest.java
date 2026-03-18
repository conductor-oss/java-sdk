package helloworld.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GreetWorkerTest {

    private final GreetWorker worker = new GreetWorker();

    @Test
    void taskDefName() {
        assertEquals("greet", worker.getTaskDefName());
    }

    @Test
    void greetsNameFromInput() {
        Task task = taskWith(Map.of("name", "Alice"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Hello, Alice! Welcome to Conductor.", result.getOutputData().get("greeting"));
    }

    @Test
    void defaultsToWorldWhenNameMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals("Hello, World! Welcome to Conductor.", result.getOutputData().get("greeting"));
    }

    @Test
    void defaultsToWorldWhenNameBlank() {
        Task task = taskWith(Map.of("name", "   "));
        TaskResult result = worker.execute(task);

        assertEquals("Hello, World! Welcome to Conductor.", result.getOutputData().get("greeting"));
    }

    @Test
    void outputContainsGreetingField() {
        Task task = taskWith(Map.of("name", "Test"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("greeting"));
        assertTrue(result.getOutputData().get("greeting").toString().contains("Test"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
