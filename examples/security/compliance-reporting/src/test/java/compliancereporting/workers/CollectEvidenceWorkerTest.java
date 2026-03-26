package compliancereporting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CollectEvidenceWorkerTest {

    private final CollectEvidenceWorker worker = new CollectEvidenceWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_collect_evidence", worker.getTaskDefName());
    }

    @Test
    void collectsEvidenceForSoc2() {
        Task task = taskWith(Map.of("framework", "SOC2-TypeII", "period", "2024-Q1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("COLLECT_EVIDENCE-1383", result.getOutputData().get("collect_evidenceId"));
        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void collectsEvidenceForPciDss() {
        Task task = taskWith(Map.of("framework", "PCI-DSS", "period", "2024-Q2"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("COLLECT_EVIDENCE-1383", result.getOutputData().get("collect_evidenceId"));
    }

    @Test
    void collectsEvidenceForIso27001() {
        Task task = taskWith(Map.of("framework", "ISO-27001", "period", "2024-annual"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("success"));
    }

    @Test
    void handlesNullFramework() {
        Map<String, Object> input = new HashMap<>();
        input.put("framework", null);
        input.put("period", "2024-Q1");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("COLLECT_EVIDENCE-1383", result.getOutputData().get("collect_evidenceId"));
    }

    @Test
    void outputContainsExpectedKeys() {
        Task task = taskWith(Map.of("framework", "HIPAA", "period", "2024-Q3"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("collect_evidenceId"));
        assertTrue(result.getOutputData().containsKey("success"));
    }

    @Test
    void successIsAlwaysTrue() {
        Task task = taskWith(Map.of("framework", "GDPR"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("success"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
