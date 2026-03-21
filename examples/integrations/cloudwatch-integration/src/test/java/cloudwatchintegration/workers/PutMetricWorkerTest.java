package cloudwatchintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PutMetricWorkerTest {

    private final PutMetricWorker worker = new PutMetricWorker();

    @Test
    void taskDefName() {
        assertEquals("cw_put_metric", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("namespace", "Custom/App", "metricName", "CPUUtil", "value", 92));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("published"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
