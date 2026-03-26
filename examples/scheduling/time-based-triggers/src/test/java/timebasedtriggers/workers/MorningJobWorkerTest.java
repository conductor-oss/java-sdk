package timebasedtriggers.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MorningJobWorkerTest {
    private final MorningJobWorker worker = new MorningJobWorker();

    @Test void taskDefName() { assertEquals("tb_morning_job", worker.getTaskDefName()); }

    @Test void executesMorningJob() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("jobType", "morning-sync", "timezone", "UTC")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("executed"));
        assertEquals(1500, r.getOutputData().get("recordsSynced"));
    }
}
