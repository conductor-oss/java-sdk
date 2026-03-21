package authenticationworkflow.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RiskAssessmentWorkerTest {

    private final RiskAssessmentWorker worker = new RiskAssessmentWorker();

    @Test
    void taskDefName() {
        assertEquals("auth_risk_assessment", worker.getTaskDefName());
    }

    @Test
    void assessesRiskSuccessfully() {
        Task task = taskWith(Map.of("risk_assessmentData", Map.of("check_mfa", true)));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("risk_assessment"));
        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void outputContainsRiskAssessment() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("risk_assessment"));
    }

    @Test
    void outputContainsProcessed() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("processed"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesExtraInputFields() {
        Task task = taskWith(Map.of("extra", "data", "risk_assessmentData", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void deterministicOutput() {
        Task task1 = taskWith(Map.of());
        Task task2 = taskWith(Map.of());
        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("risk_assessment"), r2.getOutputData().get("risk_assessment"));
    }

    @Test
    void statusIsCompleted() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
