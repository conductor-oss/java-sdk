package carecoordination.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreatePlanWorkerTest {

    private final CreatePlanWorker worker = new CreatePlanWorker();

    @Test
    void taskDefName() {
        assertEquals("ccr_create_plan", worker.getTaskDefName());
    }

    @Test
    void createsPlanFromNeeds() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-100",
                "needs", List.of("medication management", "physical therapy"),
                "condition", "Diabetes"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("careplan"));
    }

    @Test
    void careplanContainsGoals() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-200",
                "needs", List.of("a", "b", "c"),
                "condition", "CHF"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> careplan = (Map<String, Object>) result.getOutputData().get("careplan");
        assertNotNull(careplan.get("goals"));
    }

    @Test
    void interventionCountMatchesNeeds() {
        List<String> needs = List.of("med", "pt", "nutrition", "mental");
        Task task = taskWith(Map.of(
                "patientId", "PAT-300",
                "needs", needs,
                "condition", "COPD"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> careplan = (Map<String, Object>) result.getOutputData().get("careplan");
        assertEquals(4, careplan.get("interventions"));
    }

    @Test
    void careplanContainsReviewDate() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-400",
                "needs", List.of("a"),
                "condition", "Asthma"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> careplan = (Map<String, Object>) result.getOutputData().get("careplan");
        assertEquals("2024-04-15", careplan.get("reviewDate"));
    }

    @Test
    void handlesEmptyNeedsList() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-500",
                "needs", List.of(),
                "condition", "Back pain"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> careplan = (Map<String, Object>) result.getOutputData().get("careplan");
        assertEquals(0, careplan.get("interventions"));
    }

    @Test
    void handlesNullNeeds() {
        Map<String, Object> input = new HashMap<>();
        input.put("patientId", "PAT-600");
        input.put("needs", null);
        input.put("condition", "Migraine");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("careplan"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
