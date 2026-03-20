package logisticsoptimization.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ScheduleWorkerTest {
    @Test void taskDefName() { assertEquals("lo_schedule", new ScheduleWorker().getTaskDefName()); }
    @Test void schedulesVehicles() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("routes", List.of(Map.of("id","R1")), "date", "2024-01-01")));
        TaskResult r = new ScheduleWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(1, r.getOutputData().get("vehicleCount"));
    }
}
