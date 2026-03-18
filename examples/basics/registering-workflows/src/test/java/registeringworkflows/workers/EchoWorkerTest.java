package registeringworkflows.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EchoWorkerTest {

    private final EchoWorker worker = new EchoWorker();

    @Test
    void taskDefName() {
        assertEquals("echo_task", worker.getTaskDefName());
    }

    @Test
    void echoesMessageFromInput() {
        Task task = taskWith(Map.of("message", "hello conductor"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("hello conductor", result.getOutputData().get("result"));
    }

    @Test
    void handlesNullMessage() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("(no message)", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsResultField() {
        Task task = taskWith(Map.of("message", "test value"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("result"));
        assertTrue(result.getOutputData().get("result").toString().contains("test value"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
