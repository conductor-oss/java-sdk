package switchjavascript.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VipStandardWorkerTest {

    private final VipStandardWorker worker = new VipStandardWorker();

    @Test
    void taskDefName() {
        assertEquals("swjs_vip_standard", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("amount", 500, "customerType", "vip", "region", "US"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsHandlerName() {
        Task task = taskWith(Map.of("amount", 500, "customerType", "vip", "region", "US"));
        TaskResult result = worker.execute(task);
        assertEquals("swjs_vip_standard", result.getOutputData().get("handler"));
    }

    @Test
    void outputContainsProcessedFlag() {
        Task task = taskWith(Map.of("amount", 500, "customerType", "vip", "region", "US"));
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("processed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
