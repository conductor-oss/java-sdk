package logisticsoptimization.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DispatchWorkerTest {
    @Test void taskDefName() { assertEquals("lo_dispatch", new DispatchWorker().getTaskDefName()); }
    @Test void dispatches() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("schedule", List.of(Map.of("route","R1")))));
        TaskResult r = new DispatchWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(true, r.getOutputData().get("dispatched"));
    }
}
