package appointmentscheduling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RemindWorkerTest {

    private final RemindWorker worker = new RemindWorker();

    @Test
    void taskDefName() {
        assertEquals("apt_remind", worker.getTaskDefName());
    }

    @Test
    void setsReminder() {
        Task task = taskWith(Map.of("patientId", "PAT-001", "appointmentId", "APT-001",
                "slot", Map.of("date", "2024-03-20", "time", "10:30 AM")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("reminderSet"));
    }

    @Test
    void reminderTimeIs24hBefore() {
        Task task = taskWith(Map.of("slot", Map.of("date", "2024-03-20", "time", "10:30 AM")));
        TaskResult result = worker.execute(task);

        assertEquals("24h before", result.getOutputData().get("reminderTime"));
    }

    @Test
    void handlesNullSlot() {
        Map<String, Object> input = new HashMap<>();
        input.put("patientId", "PAT-001");
        input.put("slot", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("reminderSet"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void reminderSetIsAlwaysTrue() {
        Task task = taskWith(Map.of("patientId", "PAT-999"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("reminderSet"));
    }

    @Test
    void reminderTimeIsDeterministic() {
        Task task1 = taskWith(Map.of("slot", Map.of("date", "2024-03-20")));
        Task task2 = taskWith(Map.of("slot", Map.of("date", "2024-04-15")));

        assertEquals(worker.execute(task1).getOutputData().get("reminderTime"),
                     worker.execute(task2).getOutputData().get("reminderTime"));
    }

    @Test
    void outputContainsBothFields() {
        Task task = taskWith(Map.of("slot", Map.of("date", "2024-03-20", "time", "2:00 PM")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("reminderSet"));
        assertTrue(result.getOutputData().containsKey("reminderTime"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
