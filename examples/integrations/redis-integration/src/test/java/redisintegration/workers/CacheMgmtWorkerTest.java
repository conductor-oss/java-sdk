package redisintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CacheMgmtWorkerTest {

    private final CacheMgmtWorker worker = new CacheMgmtWorker();

    @Test
    void taskDefName() {
        assertEquals("red_cache_mgmt", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("connectionId", "redis-1", "key", "user:session:abc"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3600, result.getOutputData().get("ttl"));
        assertEquals(256, result.getOutputData().get("memoryUsage"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
