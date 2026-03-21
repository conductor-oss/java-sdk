package errornotification.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SendPagerDutyWorkerTest {

    @Test
    void taskDefName() {
        SendPagerDutyWorker worker = new SendPagerDutyWorker();
        assertEquals("en_send_pagerduty", worker.getTaskDefName());
    }

    @Test
    void sendsPagerDutyAlert() {
        SendPagerDutyWorker worker = new SendPagerDutyWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sent"));
    }

    @Test
    void outputContainsSent() {
        SendPagerDutyWorker worker = new SendPagerDutyWorker();
        Task task = taskWith(Map.of("severity", "critical"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("sent"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
