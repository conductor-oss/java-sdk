package eventfundraising.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class PlanWorkerTest {
    @Test void testExecute() { PlanWorker w = new PlanWorker(); assertEquals("efr_plan", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("eventName", "Test", "eventDate", "2026-01-01"));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus()); }
}
