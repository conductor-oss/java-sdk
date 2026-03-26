package redisintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RedisConnectWorkerTest {

    private final RedisConnectWorker worker = new RedisConnectWorker();

    @Test
    void taskDefName() {
        assertEquals("red_connect", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("host", "redis://localhost:6481"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("connected"));
        assertNotNull(result.getOutputData().get("connectionId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
