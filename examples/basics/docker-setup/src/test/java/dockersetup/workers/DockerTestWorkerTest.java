package dockersetup.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DockerTestWorkerTest {

    private final DockerTestWorker worker = new DockerTestWorker();

    @Test
    void taskDefName() {
        assertEquals("docker_test_task", worker.getTaskDefName());
    }

    @Test
    void returnsResultWithMessage() {
        Task task = taskWith(Map.of("message", "Hello from test"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Hello from test", result.getOutputData().get("message"));
        assertNotNull(result.getOutputData().get("timestamp"));
    }

    @Test
    void handlesNullMessage() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Docker setup test", result.getOutputData().get("message"));
        assertNotNull(result.getOutputData().get("timestamp"));
    }

    @Test
    void handlesBlankMessage() {
        Task task = taskWith(Map.of("message", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Docker setup test", result.getOutputData().get("message"));
    }

    @Test
    void outputContainsTimestamp() {
        Task task = taskWith(Map.of("message", "Test"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("timestamp"));
        assertTrue(result.getOutputData().get("timestamp").toString().length() > 0);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
