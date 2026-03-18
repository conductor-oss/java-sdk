package patientintake.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AssignWorkerTest {
    private final AssignWorker worker = new AssignWorker();

    @Test void taskDefName() { assertEquals("pit_assign", worker.getTaskDefName()); }

    @Test void assignsToEmergencyDoctor() {
        TaskResult result = worker.execute(taskWith(Map.of("department", "Emergency", "triageLevel", 2)));
        assertEquals("Dr. Martinez (ER)", result.getOutputData().get("provider"));
    }

    @Test void assignsToUrgentCareDoctor() {
        TaskResult result = worker.execute(taskWith(Map.of("department", "Urgent Care", "triageLevel", 3)));
        assertEquals("Dr. Chen (UC)", result.getOutputData().get("provider"));
    }

    @Test void includesRoom() {
        TaskResult result = worker.execute(taskWith(Map.of("department", "Emergency")));
        assertNotNull(result.getOutputData().get("room"));
    }

    @Test void includesEstimatedWait() {
        TaskResult result = worker.execute(taskWith(Map.of("department", "Urgent Care", "triageLevel", 3)));
        assertNotNull(result.getOutputData().get("estimatedWait"));
    }

    @Test void handlesMissingInputs() {
        TaskResult result = worker.execute(taskWith(Map.of()));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
