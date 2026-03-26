package timebasedtriggers.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CheckTimeWorkerTest {
    private final CheckTimeWorker worker = new CheckTimeWorker();

    @Test void taskDefName() { assertEquals("tb_check_time", worker.getTaskDefName()); }

    @Test void morningWindow() {
        Task task = taskWith(Map.of("currentHour", 9, "timezone", "UTC"));
        TaskResult r = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals("morning", r.getOutputData().get("timeWindow"));
    }

    @Test void afternoonWindow() {
        Task task = taskWith(Map.of("currentHour", 14, "timezone", "UTC"));
        TaskResult r = worker.execute(task);
        assertEquals("afternoon", r.getOutputData().get("timeWindow"));
    }

    @Test void eveningWindow() {
        Task task = taskWith(Map.of("currentHour", 20, "timezone", "UTC"));
        TaskResult r = worker.execute(task);
        assertEquals("evening", r.getOutputData().get("timeWindow"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
