package cloudwatchintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckStatusWorkerTest {

    private final CheckStatusWorker worker = new CheckStatusWorker();

    @Test
    void taskDefName() {
        assertEquals("cw_check_status", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("alarmName", "CPUUtil-high", "currentValue", 92, "threshold", 80));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ALARM", result.getOutputData().get("state"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
