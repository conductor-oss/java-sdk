package predictivemonitoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CollectHistoryTest {

    private final CollectHistory worker = new CollectHistory();

    @Test
    void taskDefName() {
        assertEquals("pdm_collect_history", worker.getTaskDefName());
    }

    @Test
    void returnsExpectedFieldsForValidInput() {
        Task task = taskWith("cpu_usage", 30);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(43200, result.getOutputData().get("dataPoints"));
        assertEquals("cpu_usage", result.getOutputData().get("metricName"));
        assertEquals("1m", result.getOutputData().get("granularity"));
        assertNotNull(result.getOutputData().get("oldest"));
        assertNotNull(result.getOutputData().get("newest"));
    }

    @Test
    void metricNamePassedThrough() {
        Task task = taskWith("memory_pct", 7);

        TaskResult result = worker.execute(task);

        assertEquals("memory_pct", result.getOutputData().get("metricName"));
    }

    @Test
    void nullMetricNamePassedAsNull() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("historyDays", 14);
        // metricName deliberately omitted
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNull(result.getOutputData().get("metricName"));
    }

    private Task taskWith(String metricName, int historyDays) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("metricName", metricName);
        input.put("historyDays", historyDays);
        task.setInputData(input);
        return task;
    }
}
