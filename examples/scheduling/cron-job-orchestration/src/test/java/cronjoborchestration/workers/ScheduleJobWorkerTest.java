package cronjoborchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ScheduleJobWorkerTest {
    @Test void parsesDailyCron() {
        ScheduleJobWorker w = new ScheduleJobWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("cronExpression", "0 2 * * *", "jobName", "daily-backup")));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("validCron"));
        assertTrue(((String) r.getOutputData().get("frequency")).contains("daily"));
    }
}
