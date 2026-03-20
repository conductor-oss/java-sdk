package appointmentscheduling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BookWorkerTest {

    private final BookWorker worker = new BookWorker();

    @Test
    void taskDefName() {
        assertEquals("apt_book", worker.getTaskDefName());
    }

    @Test
    void booksAppointmentSuccessfully() {
        Task task = taskWith(Map.of("patientId", "PAT-001", "providerId", "DR-001",
                "slot", Map.of("date", "2024-03-20", "time", "10:30 AM"), "visitType", "follow-up"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("APT-20240315-001", result.getOutputData().get("appointmentId"));
        assertEquals(true, result.getOutputData().get("booked"));
    }

    @Test
    void includesBookedAtTimestamp() {
        Task task = taskWith(Map.of("patientId", "PAT-001", "slot", Map.of("date", "2024-03-20"), "visitType", "new"));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("bookedAt"));
    }

    @Test
    void handlesNullSlot() {
        Map<String, Object> input = new HashMap<>();
        input.put("patientId", "PAT-001");
        input.put("slot", null);
        input.put("visitType", "follow-up");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullVisitType() {
        Map<String, Object> input = new HashMap<>();
        input.put("patientId", "PAT-001");
        input.put("slot", Map.of("date", "2024-03-20"));
        input.put("visitType", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void appointmentIdIsDeterministic() {
        Task task1 = taskWith(Map.of("patientId", "PAT-001", "visitType", "new"));
        Task task2 = taskWith(Map.of("patientId", "PAT-002", "visitType", "follow-up"));
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("appointmentId"), r2.getOutputData().get("appointmentId"));
    }

    @Test
    void bookedIsAlwaysTrue() {
        Task task = taskWith(Map.of("patientId", "PAT-999", "visitType", "urgent"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("booked"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
