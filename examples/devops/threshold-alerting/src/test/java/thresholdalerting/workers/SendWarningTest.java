package thresholdalerting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SendWarningTest {

    private final SendWarning worker = new SendWarning();

    @Test void taskDefName() { assertEquals("th_send_warning", worker.getTaskDefName()); }

    @Test void returnsCompletedStatus() {
        TaskResult result = worker.execute(taskWith("latency_p99", 180));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test void returnsWarned() {
        TaskResult result = worker.execute(taskWith("latency_p99", 180));
        assertEquals(true, result.getOutputData().get("warned"));
    }

    @Test void returnsSlackChannel() {
        TaskResult result = worker.execute(taskWith("latency_p99", 180));
        assertEquals("slack", result.getOutputData().get("channel"));
    }

    @Test void returnsSentTo() {
        TaskResult result = worker.execute(taskWith("error_rate", 75));
        assertEquals("#ops-alerts", result.getOutputData().get("sentTo"));
    }

    @Test void handlesNullMetricName() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test void outputContainsAllExpectedKeys() {
        TaskResult result = worker.execute(taskWith("disk_usage", 80));
        assertNotNull(result.getOutputData().get("warned"));
        assertNotNull(result.getOutputData().get("channel"));
        assertNotNull(result.getOutputData().get("sentTo"));
    }

    @Test void warnedIsAlwaysTrue() {
        TaskResult result = worker.execute(taskWith("any_metric", 50));
        assertEquals(true, result.getOutputData().get("warned"));
    }

    private Task taskWith(String metricName, double value) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("metricName", metricName);
        input.put("currentValue", value);
        input.put("severity", "warning");
        task.setInputData(input);
        return task;
    }
}
