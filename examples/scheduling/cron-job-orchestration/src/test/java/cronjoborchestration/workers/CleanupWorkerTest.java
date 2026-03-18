package cronjoborchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CleanupWorkerTest {
    @Test void cleansUp() {
        CleanupWorker w = new CleanupWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("jobName", "test")));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("cleanedUp"));
    }
}
