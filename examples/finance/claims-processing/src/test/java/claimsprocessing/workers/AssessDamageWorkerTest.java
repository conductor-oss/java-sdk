package claimsprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AssessDamageWorkerTest {
    private final AssessDamageWorker worker = new AssessDamageWorker();

    @Test void taskDefName() { assertEquals("clp_assess_damage", worker.getTaskDefName()); }

    @Test void assessesModerateAt85Percent() {
        Task task = taskWith(Map.of("claimId", "CLM-100", "requestedAmount", 5000));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4250L, result.getOutputData().get("assessedAmount")); // 5000 * 0.85
        assertEquals("moderate", result.getOutputData().get("damageCategory"));
    }

    @Test void assessesMinorAt95Percent() {
        Task task = taskWith(Map.of("requestedAmount", 1000));
        TaskResult result = worker.execute(task);
        assertEquals(950L, result.getOutputData().get("assessedAmount")); // 1000 * 0.95
        assertEquals("minor", result.getOutputData().get("damageCategory"));
    }

    @Test void assessesMajorAt80Percent() {
        Task task = taskWith(Map.of("requestedAmount", 20000));
        TaskResult result = worker.execute(task);
        assertEquals(16000L, result.getOutputData().get("assessedAmount")); // 20000 * 0.80
        assertEquals("major", result.getOutputData().get("damageCategory"));
    }

    @Test void assessesSevereAt75Percent() {
        Task task = taskWith(Map.of("requestedAmount", 100000));
        TaskResult result = worker.execute(task);
        assertEquals(75000L, result.getOutputData().get("assessedAmount")); // 100000 * 0.75
        assertEquals("severe", result.getOutputData().get("damageCategory"));
    }

    @Test void assessesStringAmount() {
        Task task = taskWith(Map.of("claimId", "CLM-101", "requestedAmount", "5000"));
        TaskResult result = worker.execute(task);
        assertEquals(4250L, result.getOutputData().get("assessedAmount"));
    }

    @Test void handlesZeroAmount() {
        Task task = taskWith(Map.of("requestedAmount", 0));
        TaskResult result = worker.execute(task);
        assertEquals(0L, result.getOutputData().get("assessedAmount"));
    }

    @Test void handlesNullAmount() {
        Map<String, Object> input = new HashMap<>(); input.put("requestedAmount", null);
        TaskResult result = worker.execute(taskWith(input));
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0L, result.getOutputData().get("assessedAmount"));
    }

    @Test void outputContainsAssessorNotes() {
        Task task = taskWith(Map.of("requestedAmount", 5000));
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("assessorNotes"));
        assertTrue(((String) result.getOutputData().get("assessorNotes")).contains("moderate"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(new HashMap<>(input)); return t;
    }
}
