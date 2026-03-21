package appointmentscheduling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckAvailabilityWorkerTest {

    private final CheckAvailabilityWorker worker = new CheckAvailabilityWorker();

    @Test
    void taskDefName() {
        assertEquals("apt_check_availability", worker.getTaskDefName());
    }

    @Test
    void returnsAvailableSlot() {
        Task task = taskWith(Map.of("providerId", "DR-001", "preferredDate", "2024-03-20", "visitType", "follow-up"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("availableSlot"));
    }

    @Test
    void availableSlotContainsPreferredDate() {
        Task task = taskWith(Map.of("providerId", "DR-001", "preferredDate", "2024-05-01", "visitType", "checkup"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> slot = (Map<String, Object>) result.getOutputData().get("availableSlot");
        assertEquals("2024-05-01", slot.get("date"));
        assertEquals("10:30 AM", slot.get("time"));
        assertEquals(30, slot.get("duration"));
    }

    @Test
    void returnsAlternateSlots() {
        Task task = taskWith(Map.of("providerId", "DR-001", "preferredDate", "2024-03-20", "visitType", "follow-up"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> alternates = (List<Map<String, Object>>) result.getOutputData().get("alternateSlots");
        assertNotNull(alternates);
        assertEquals(2, alternates.size());
    }

    @Test
    void handlesNullProviderId() {
        Map<String, Object> input = new HashMap<>();
        input.put("providerId", null);
        input.put("preferredDate", "2024-03-20");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullPreferredDate() {
        Map<String, Object> input = new HashMap<>();
        input.put("providerId", "DR-001");
        input.put("preferredDate", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> slot = (Map<String, Object>) result.getOutputData().get("availableSlot");
        assertEquals("2024-01-01", slot.get("date"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void slotDurationIs30Minutes() {
        Task task = taskWith(Map.of("providerId", "DR-002", "preferredDate", "2024-04-15", "visitType", "new"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> slot = (Map<String, Object>) result.getOutputData().get("availableSlot");
        assertEquals(30, slot.get("duration"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
