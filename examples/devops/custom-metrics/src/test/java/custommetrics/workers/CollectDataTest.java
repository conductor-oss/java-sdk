package custommetrics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CollectDataTest {

    private final CollectData worker = new CollectData();

    @Test void taskDefName() { assertEquals("cus_collect_data", worker.getTaskDefName()); }

    @Test void returnsCompletedStatus() {
        TaskResult result = worker.execute(taskWith(4, "10s"));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test void returnsRawDataPoints() {
        TaskResult result = worker.execute(taskWith(4, "10s"));
        assertEquals(4800, result.getOutputData().get("rawDataPoints"));
    }

    @Test void returnsCollectedAtTimestamp() {
        TaskResult result = worker.execute(taskWith(4, "10s"));
        assertNotNull(result.getOutputData().get("collectedAt"));
    }

    @Test void returnsMetricsWithData() {
        TaskResult result = worker.execute(taskWith(4, "10s"));
        assertEquals(4, result.getOutputData().get("metricsWithData"));
    }

    @Test void handlesNullInterval() {
        TaskResult result = worker.execute(taskWith(4, null));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test void outputContainsAllExpectedKeys() {
        TaskResult result = worker.execute(taskWith(4, "30s"));
        assertNotNull(result.getOutputData().get("rawDataPoints"));
        assertNotNull(result.getOutputData().get("collectedAt"));
        assertNotNull(result.getOutputData().get("metricsWithData"));
    }

    private Task taskWith(int registeredMetrics, String interval) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("registeredMetrics", registeredMetrics);
        if (interval != null) input.put("collectionInterval", interval);
        task.setInputData(input);
        return task;
    }
}
