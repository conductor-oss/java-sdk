package reservationsystem.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CheckAvailabilityWorkerTest {
    @Test void testExecute() {
        CheckAvailabilityWorker worker = new CheckAvailabilityWorker();
        assertEquals("rsv_check_availability", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("date", "2026-03-10", "time", "7:30 PM", "partySize", 4));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("slot"));
    }
}
