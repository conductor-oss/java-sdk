package cloudwatchintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CwNotifyWorkerTest {

    private final CwNotifyWorker worker = new CwNotifyWorker();

    @Test
    void taskDefName() {
        assertEquals("cw_notify", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("alarmName", "CPUUtil-high", "alarmState", "ALARM", "email", "ops@test.com"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("notified"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
