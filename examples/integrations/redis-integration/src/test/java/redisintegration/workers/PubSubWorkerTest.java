package redisintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PubSubWorkerTest {

    private final PubSubWorker worker = new PubSubWorker();

    @Test
    void taskDefName() {
        assertEquals("red_pub_sub", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("connectionId", "redis-1", "channel", "updates", "message", "hello"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
        assertEquals(3, result.getOutputData().get("subscriberCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
