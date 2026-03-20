package redisintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GetSetWorkerTest {

    private final GetSetWorker worker = new GetSetWorker();

    @Test
    void taskDefName() {
        assertEquals("red_get_set", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("connectionId", "redis-1", "key", "user:session:abc", "value", "testval"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("setOk"));
        assertEquals("testval", result.getOutputData().get("retrievedValue"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
