package creatingworkers.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SimpleTransformWorkerTest {

    private final SimpleTransformWorker worker = new SimpleTransformWorker();

    @Test
    void taskDefName() {
        assertEquals("simple_transform", worker.getTaskDefName());
    }

    @Test
    void transformsTextCorrectly() {
        Task task = taskWith(Map.of("text", "Hello Conductor"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Hello Conductor", result.getOutputData().get("original"));
    }

    @Test
    void returnsUpperCase() {
        Task task = taskWith(Map.of("text", "Hello Conductor"));
        TaskResult result = worker.execute(task);

        assertEquals("HELLO CONDUCTOR", result.getOutputData().get("upper"));
    }

    @Test
    void returnsLowerCase() {
        Task task = taskWith(Map.of("text", "Hello Conductor"));
        TaskResult result = worker.execute(task);

        assertEquals("hello conductor", result.getOutputData().get("lower"));
    }

    @Test
    void returnsCorrectLength() {
        Task task = taskWith(Map.of("text", "Hello Conductor"));
        TaskResult result = worker.execute(task);

        assertEquals(15, result.getOutputData().get("length"));
    }

    @Test
    void handlesEmptyText() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("upper"));
        assertEquals("", result.getOutputData().get("lower"));
        assertEquals(0, result.getOutputData().get("length"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
