package carecoordination.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssignTeamWorkerTest {

    private final AssignTeamWorker worker = new AssignTeamWorker();

    @Test
    void taskDefName() {
        assertEquals("ccr_assign_team", worker.getTaskDefName());
    }

    @Test
    void assignsTeamForHighAcuity() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-100",
                "careplan", Map.of("goals", List.of("goal1")),
                "acuity", "high"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("team"));
    }

    @Test
    void teamHasFourMembers() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-200",
                "careplan", Map.of(),
                "acuity", "moderate"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> team = (List<Map<String, String>>) result.getOutputData().get("team");
        assertEquals(4, team.size());
    }

    @Test
    void teamIncludesPCP() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-300",
                "careplan", Map.of(),
                "acuity", "low"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> team = (List<Map<String, String>>) result.getOutputData().get("team");
        assertTrue(team.stream().anyMatch(m -> "PCP".equals(m.get("role"))));
    }

    @Test
    void teamIncludesCareCoordinator() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-400",
                "careplan", Map.of(),
                "acuity", "high"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> team = (List<Map<String, String>>) result.getOutputData().get("team");
        assertTrue(team.stream().anyMatch(m -> "Care Coordinator".equals(m.get("role"))));
    }

    @Test
    void teamIncludesDietitian() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-500",
                "careplan", Map.of(),
                "acuity", "moderate"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> team = (List<Map<String, String>>) result.getOutputData().get("team");
        assertTrue(team.stream().anyMatch(m -> "Dietitian".equals(m.get("role"))));
    }

    @Test
    void handlesNullAcuity() {
        Map<String, Object> input = new HashMap<>();
        input.put("patientId", "PAT-600");
        input.put("careplan", Map.of());
        input.put("acuity", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("team"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
