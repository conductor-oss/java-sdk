package scattergather.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SgrBroadcastWorkerTest {

    private final SgrBroadcastWorker worker = new SgrBroadcastWorker();

    @Test
    void taskDefName() {
        assertEquals("sgr_broadcast", worker.getTaskDefName());
    }

    @Test
    void broadcastsSuccessfully() {
        Task task = taskWith(Map.of("query", "laptop", "sources", List.of("a", "b", "c")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("broadcasted"));
        assertEquals(3, result.getOutputData().get("sourceCount"));
    }

    @Test
    void normalizesQuery() {
        Task task = taskWith(Map.of("query", "  LAPTOP  "));
        TaskResult result = worker.execute(task);

        assertEquals("laptop", result.getOutputData().get("validatedQuery"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertTrue(result.getOutputData().containsKey("broadcasted"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
