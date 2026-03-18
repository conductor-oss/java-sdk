package cronjoborchestration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class LogResultWorkerTest {
    @Test void logsResult() {
        LogResultWorker w = new LogResultWorker();
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("jobName", "test", "durationMs", 50, "executed", true)));
        TaskResult r = w.execute(t);
        assertEquals(true, r.getOutputData().get("logged"));
    }
}
