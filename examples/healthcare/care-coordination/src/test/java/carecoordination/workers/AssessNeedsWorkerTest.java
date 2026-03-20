package carecoordination.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssessNeedsWorkerTest {

    private final AssessNeedsWorker worker = new AssessNeedsWorker();

    @Test
    void taskDefName() {
        assertEquals("ccr_assess_needs", worker.getTaskDefName());
    }

    @Test
    void assessesHighAcuityPatient() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-10234",
                "condition", "Type 2 Diabetes with complications",
                "acuity", "high"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("needs"));
    }

    @Test
    void returnsNeedsAsList() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-100",
                "condition", "CHF",
                "acuity", "moderate"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> needs = (List<String>) result.getOutputData().get("needs");
        assertEquals(4, needs.size());
        assertTrue(needs.contains("medication management"));
    }

    @Test
    void returnsRiskScore() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-200",
                "condition", "Hypertension",
                "acuity", "low"));
        TaskResult result = worker.execute(task);

        assertEquals(7.2, result.getOutputData().get("riskScore"));
    }

    @Test
    void returnsComplexCaseFlag() {
        Task task = taskWith(Map.of(
                "patientId", "PAT-300",
                "condition", "COPD",
                "acuity", "high"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("complexCase"));
    }

    @Test
    void handlesNullPatientId() {
        Map<String, Object> input = new HashMap<>();
        input.put("patientId", null);
        input.put("condition", "Diabetes");
        input.put("acuity", "low");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullCondition() {
        Map<String, Object> input = new HashMap<>();
        input.put("patientId", "PAT-400");
        input.put("condition", null);
        input.put("acuity", "moderate");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullAcuity() {
        Map<String, Object> input = new HashMap<>();
        input.put("patientId", "PAT-500");
        input.put("condition", "Asthma");
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
        assertNotNull(result.getOutputData().get("needs"));
        assertNotNull(result.getOutputData().get("riskScore"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
