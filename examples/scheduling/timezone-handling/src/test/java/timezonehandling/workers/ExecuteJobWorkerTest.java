package timezonehandling.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ExecuteJobWorkerTest {
    @Test void taskDefName() { assertEquals("tz_execute_job", new ExecuteJobWorker().getTaskDefName()); }
    @Test void executesJob() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(Map.of("jobName", "backup", "scheduledUtc", "2026-03-08T17:00:00Z")));
        TaskResult r = new ExecuteJobWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("executed"));
    }
}
