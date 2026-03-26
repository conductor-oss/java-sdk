package multiagentplanning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EstimateFrontendWorkerTest {

    private final EstimateFrontendWorker worker = new EstimateFrontendWorker();

    @Test
    void taskDefName() {
        assertEquals("pp_estimate_frontend", worker.getTaskDefName());
    }

    @Test
    void estimatesStandardComponents() {
        Task task = taskWith(Map.of(
                "components", List.of("User Dashboard", "Authentication UI"),
                "complexity", "medium",
                "teamSize", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breakdown = (List<Map<String, Object>>) result.getOutputData().get("breakdown");
        assertEquals(2, breakdown.size());

        // Standard components get 1/2/1 = 4 each, total = 8
        assertEquals(8, result.getOutputData().get("totalWeeks"));
        // calendarWeeks = ceil(8/2) = 4
        assertEquals(4, result.getOutputData().get("calendarWeeks"));
    }

    @Test
    void interactiveComponentGetsHigherEstimate() {
        Task task = taskWith(Map.of(
                "components", List.of("Interactive Data Visualization"),
                "complexity", "medium",
                "teamSize", 2));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breakdown = (List<Map<String, Object>>) result.getOutputData().get("breakdown");
        Map<String, Object> entry = breakdown.get(0);

        assertEquals(2, entry.get("designWeeks"));
        assertEquals(4, entry.get("devWeeks"));
        assertEquals(1, entry.get("testWeeks"));
        assertEquals(7, entry.get("subtotal"));

        // totalWeeks = 7, calendarWeeks = ceil(7/2) = 4
        assertEquals(7, result.getOutputData().get("totalWeeks"));
        assertEquals(4, result.getOutputData().get("calendarWeeks"));
    }

    @Test
    void threeComponentMixedEstimate() {
        Task task = taskWith(Map.of(
                "components", List.of("User Dashboard", "Authentication UI", "Interactive Data Visualization"),
                "complexity", "medium",
                "teamSize", 2));
        TaskResult result = worker.execute(task);

        // Dashboard: 1+2+1=4, Auth: 1+2+1=4, Interactive: 2+4+1=7 => total=15
        assertEquals(15, result.getOutputData().get("totalWeeks"));
        // calendarWeeks = ceil(15/2) = 8
        assertEquals(8, result.getOutputData().get("calendarWeeks"));
    }

    @Test
    void returnsRisks() {
        Task task = taskWith(Map.of(
                "components", List.of("User Dashboard"),
                "teamSize", 2));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> risks = (List<String>) result.getOutputData().get("risks");
        assertNotNull(risks);
        assertFalse(risks.isEmpty());
    }

    @Test
    void returnsTeamSize() {
        Task task = taskWith(Map.of(
                "components", List.of("User Dashboard"),
                "teamSize", 3));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("teamSize"));
    }

    @Test
    void handlesEmptyComponents() {
        Task task = taskWith(Map.of("components", List.of(), "teamSize", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalWeeks"));
        assertEquals(0, result.getOutputData().get("calendarWeeks"));
    }

    @Test
    void handlesNullComponents() {
        Map<String, Object> input = new HashMap<>();
        input.put("components", null);
        input.put("teamSize", 2);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalWeeks"));
    }

    @Test
    void handlesMissingTeamSizeDefaultsToTwo() {
        Task task = taskWith(Map.of("components", List.of("User Dashboard")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("teamSize"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
