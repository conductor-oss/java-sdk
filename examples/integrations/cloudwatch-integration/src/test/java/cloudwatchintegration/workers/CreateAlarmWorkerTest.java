package cloudwatchintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreateAlarmWorkerTest {

    private final CreateAlarmWorker worker = new CreateAlarmWorker();

    @Test
    void taskDefName() {
        assertEquals("cw_create_alarm", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("namespace", "Custom/App", "metricName", "CPUUtil", "threshold", 80));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("alarmName"));
        assertNotNull(result.getOutputData().get("alarmArn"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
