package thresholdalerting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LogOkTest {

    private final LogOk worker = new LogOk();

    @Test void taskDefName() { assertEquals("th_log_ok", worker.getTaskDefName()); }

    @Test void returnsCompletedStatus() {
        TaskResult result = worker.execute(taskWith("cpu_usage", 30));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test void returnsLogged() {
        TaskResult result = worker.execute(taskWith("cpu_usage", 30));
        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test void returnsHealthyStatus() {
        TaskResult result = worker.execute(taskWith("cpu_usage", 30));
        assertEquals("healthy", result.getOutputData().get("status"));
    }

    @Test void handlesNullInputs() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test void outputContainsAllExpectedKeys() {
        TaskResult result = worker.execute(taskWith("mem", 40));
        assertNotNull(result.getOutputData().get("logged"));
        assertNotNull(result.getOutputData().get("status"));
    }

    @Test void loggedIsAlwaysTrue() {
        TaskResult result = worker.execute(taskWith("any_metric", 0));
        assertEquals(true, result.getOutputData().get("logged"));
    }

    @Test void statusIsAlwaysHealthy() {
        TaskResult result = worker.execute(taskWith("test", 10));
        assertEquals("healthy", result.getOutputData().get("status"));
    }

    private Task taskWith(String metricName, double value) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("metricName", metricName);
        input.put("currentValue", value);
        task.setInputData(input);
        return task;
    }
}
