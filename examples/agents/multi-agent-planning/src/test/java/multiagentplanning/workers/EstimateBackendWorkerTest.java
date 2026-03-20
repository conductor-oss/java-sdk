package multiagentplanning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EstimateBackendWorkerTest {

    private final EstimateBackendWorker worker = new EstimateBackendWorker();

    @Test
    void taskDefName() {
        assertEquals("pp_estimate_backend", worker.getTaskDefName());
    }

    @Test
    void estimatesStandardComponents() {
        Task task = taskWith(Map.of(
                "components", List.of("REST API Gateway", "Notification Service"),
                "complexity", "high",
                "teamSize", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breakdown = (List<Map<String, Object>>) result.getOutputData().get("breakdown");
        assertEquals(2, breakdown.size());

        // Standard components get 1/3/1 = 5 each, total = 10
        assertEquals(10, result.getOutputData().get("totalWeeks"));
        // calendarWeeks = ceil(10/3) = 4
        assertEquals(4, result.getOutputData().get("calendarWeeks"));
    }

    @Test
    void coreComponentGetsHigherEstimate() {
        Task task = taskWith(Map.of(
                "components", List.of("Core Business Logic Service"),
                "complexity", "high",
                "teamSize", 3));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breakdown = (List<Map<String, Object>>) result.getOutputData().get("breakdown");
        Map<String, Object> entry = breakdown.get(0);

        assertEquals(3, entry.get("designWeeks"));
        assertEquals(6, entry.get("devWeeks"));
        assertEquals(2, entry.get("testWeeks"));
        assertEquals(11, entry.get("subtotal"));

        assertEquals(11, result.getOutputData().get("totalWeeks"));
        // calendarWeeks = ceil(11/3) = 4
        assertEquals(4, result.getOutputData().get("calendarWeeks"));
    }

    @Test
    void fourComponentMixedEstimate() {
        Task task = taskWith(Map.of(
                "components", List.of("REST API Gateway", "Core Business Logic Service",
                        "Data Processing Pipeline", "Notification Service"),
                "complexity", "high",
                "teamSize", 3));
        TaskResult result = worker.execute(task);

        // Gateway: 1+3+1=5, Core: 3+6+2=11, Pipeline: 1+3+1=5, Notification: 1+3+1=5 => total=26
        assertEquals(26, result.getOutputData().get("totalWeeks"));
        // calendarWeeks = ceil(26/3) = 9
        assertEquals(9, result.getOutputData().get("calendarWeeks"));
    }

    @Test
    void returnsRisks() {
        Task task = taskWith(Map.of(
                "components", List.of("REST API Gateway"),
                "teamSize", 3));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> risks = (List<String>) result.getOutputData().get("risks");
        assertNotNull(risks);
        assertFalse(risks.isEmpty());
    }

    @Test
    void returnsTeamSize() {
        Task task = taskWith(Map.of(
                "components", List.of("REST API Gateway"),
                "teamSize", 4));
        TaskResult result = worker.execute(task);

        assertEquals(4, result.getOutputData().get("teamSize"));
    }

    @Test
    void handlesEmptyComponents() {
        Task task = taskWith(Map.of("components", List.of(), "teamSize", 3));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalWeeks"));
        assertEquals(0, result.getOutputData().get("calendarWeeks"));
    }

    @Test
    void handlesNullComponents() {
        Map<String, Object> input = new HashMap<>();
        input.put("components", null);
        input.put("teamSize", 3);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("totalWeeks"));
    }

    @Test
    void handlesMissingTeamSizeDefaultsToThree() {
        Task task = taskWith(Map.of("components", List.of("REST API Gateway")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("teamSize"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
