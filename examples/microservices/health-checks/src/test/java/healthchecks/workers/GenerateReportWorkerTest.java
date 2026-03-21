package healthchecks.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateReportWorkerTest {

    private final GenerateReportWorker worker = new GenerateReportWorker();

    @Test
    void taskDefName() {
        assertEquals("hc_generate_report", worker.getTaskDefName());
    }

    @Test
    void allHealthyReturnsHealthy() {
        Map<String, Object> input = new HashMap<>();
        input.put("apiHealth", Map.of("status", "healthy", "service", "api"));
        input.put("dbHealth", Map.of("status", "healthy", "service", "db"));
        input.put("cacheHealth", Map.of("status", "healthy", "service", "cache"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("healthy", result.getOutputData().get("overallStatus"));
    }

    @Test
    void degradedWhenOneUnhealthy() {
        Map<String, Object> input = new HashMap<>();
        input.put("apiHealth", Map.of("status", "healthy", "service", "api"));
        input.put("dbHealth", Map.of("status", "unhealthy", "service", "db"));
        input.put("cacheHealth", Map.of("status", "healthy", "service", "cache"));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals("degraded", result.getOutputData().get("overallStatus"));
    }

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("apiHealth", null);
        input.put("dbHealth", null);
        input.put("cacheHealth", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("degraded", result.getOutputData().get("overallStatus"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
