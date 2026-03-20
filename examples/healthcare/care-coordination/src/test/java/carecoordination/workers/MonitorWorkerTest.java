package carecoordination.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MonitorWorkerTest {

    private final MonitorWorker worker = new MonitorWorker();

    @Test
    void taskDefName() {
        assertEquals("ccr_monitor", worker.getTaskDefName());
    }

    @Test
    void activatesMonitoring() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-100",
                "careplan", Map.of(),
                "team", List.of(Map.of("role", "PCP", "name", "Dr. A"))));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("active"));
    }

    @Test
    void returnsWeeklyFrequency() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-200",
                "careplan", Map.of(),
                "team", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals("weekly", result.getOutputData().get("checkInFrequency"));
    }

    @Test
    void returnsNextCheckIn() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-300",
                "careplan", Map.of(),
                "team", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals("2024-03-22", result.getOutputData().get("nextCheckIn"));
    }

    @Test
    void handlesLargeTeam() {
        List<Map<String, String>> team = List.of(
                Map.of("role", "PCP", "name", "Dr. A"),
                Map.of("role", "RN", "name", "Nurse B"),
                Map.of("role", "PT", "name", "Therapist C"),
                Map.of("role", "RD", "name", "Dietitian D"));
        Task task = taskWith(Map.of(
                "patientId", "PAT-400",
                "careplan", Map.of(),
                "team", team));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullTeam() {
        Map<String, Object> input = new HashMap<>();
        input.put("patientId", "PAT-500");
        input.put("careplan", Map.of());
        input.put("team", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("active"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("active"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-600",
                "careplan", Map.of(),
                "team", List.of()));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("active"));
        assertTrue(result.getOutputData().containsKey("checkInFrequency"));
        assertTrue(result.getOutputData().containsKey("nextCheckIn"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
