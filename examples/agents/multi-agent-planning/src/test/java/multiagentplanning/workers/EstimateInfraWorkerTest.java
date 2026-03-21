package multiagentplanning.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EstimateInfraWorkerTest {

    private final EstimateInfraWorker worker = new EstimateInfraWorker();

    @Test
    void taskDefName() {
        assertEquals("pp_estimate_infra", worker.getTaskDefName());
    }

    @Test
    void estimatesStandardComponents() {
        Task task = taskWith(Map.of(
                "components", List.of("PostgreSQL Database", "Redis Cache"),
                "complexity", "medium",
                "teamSize", 2));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breakdown = (List<Map<String, Object>>) result.getOutputData().get("breakdown");
        assertEquals(2, breakdown.size());

        // Standard components get 1+1+1 = 3 each, total = 6
        assertEquals(6, result.getOutputData().get("totalWeeks"));
        // calendarWeeks = ceil(6/2) = 3
        assertEquals(3, result.getOutputData().get("calendarWeeks"));
    }

    @Test
    void kubernetesGetsHigherEstimate() {
        Task task = taskWith(Map.of(
                "components", List.of("Kubernetes Cluster"),
                "complexity", "medium",
                "teamSize", 2));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breakdown = (List<Map<String, Object>>) result.getOutputData().get("breakdown");
        Map<String, Object> entry = breakdown.get(0);

        assertEquals(3, entry.get("setupWeeks"));
        assertEquals(1, entry.get("configWeeks"));
        assertEquals(1, entry.get("testWeeks"));
        assertEquals(5, entry.get("subtotal"));

        assertEquals(5, result.getOutputData().get("totalWeeks"));
        // calendarWeeks = ceil(5/2) = 3
        assertEquals(3, result.getOutputData().get("calendarWeeks"));
    }

    @Test
    void fourComponentMixedEstimate() {
        Task task = taskWith(Map.of(
                "components", List.of("Kubernetes Cluster", "PostgreSQL Database",
                        "Redis Cache", "CI/CD Pipeline"),
                "complexity", "medium",
                "teamSize", 2));
        TaskResult result = worker.execute(task);

        // K8s: 3+1+1=5, Postgres: 1+1+1=3, Redis: 1+1+1=3, CICD: 1+1+1=3 => total=14
        assertEquals(14, result.getOutputData().get("totalWeeks"));
        // calendarWeeks = ceil(14/2) = 7
        assertEquals(7, result.getOutputData().get("calendarWeeks"));
    }

    @Test
    void returnsRisks() {
        Task task = taskWith(Map.of(
                "components", List.of("PostgreSQL Database"),
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
                "components", List.of("Redis Cache"),
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
        Task task = taskWith(Map.of("components", List.of("Redis Cache")));
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
