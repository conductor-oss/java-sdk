package thresholdalerting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CheckMetricTest {

    private final CheckMetric worker = new CheckMetric();

    @Test void taskDefName() { assertEquals("th_check_metric", worker.getTaskDefName()); }

    @Test void criticalWhenAboveCriticalThreshold() {
        TaskResult result = worker.execute(taskWith(95, 70, 90));
        assertEquals("critical", result.getOutputData().get("severity"));
    }

    @Test void warningWhenAboveWarningThreshold() {
        TaskResult result = worker.execute(taskWith(75, 70, 90));
        assertEquals("warning", result.getOutputData().get("severity"));
    }

    @Test void okWhenBelowWarningThreshold() {
        TaskResult result = worker.execute(taskWith(50, 70, 90));
        assertEquals("ok", result.getOutputData().get("severity"));
    }

    @Test void criticalAtExactCriticalThreshold() {
        TaskResult result = worker.execute(taskWith(90, 70, 90));
        assertEquals("critical", result.getOutputData().get("severity"));
    }

    @Test void warningAtExactWarningThreshold() {
        TaskResult result = worker.execute(taskWith(70, 70, 90));
        assertEquals("warning", result.getOutputData().get("severity"));
    }

    @Test void returnsValueInOutput() {
        TaskResult result = worker.execute(taskWith(12.5, 5, 10));
        assertEquals(12.5, result.getOutputData().get("value"));
    }

    @Test void returnsThresholdsInOutput() {
        TaskResult result = worker.execute(taskWith(50, 70, 90));
        assertEquals(70.0, result.getOutputData().get("warningThreshold"));
        assertEquals(90.0, result.getOutputData().get("criticalThreshold"));
    }

    @Test void defaultsAppliedWhenMissing() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ok", result.getOutputData().get("severity"));
    }

    @Test void handlesStringInputValues() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("currentValue", "85");
        input.put("warningThreshold", "70");
        input.put("criticalThreshold", "90");
        task.setInputData(input);
        TaskResult result = worker.execute(task);
        assertEquals("warning", result.getOutputData().get("severity"));
    }

    private Task taskWith(double value, double warn, double crit) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("metricName", "test_metric");
        input.put("currentValue", value);
        input.put("warningThreshold", warn);
        input.put("criticalThreshold", crit);
        task.setInputData(input);
        return task;
    }
}
