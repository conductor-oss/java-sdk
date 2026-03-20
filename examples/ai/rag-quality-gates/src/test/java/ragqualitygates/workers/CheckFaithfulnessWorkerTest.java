package ragqualitygates.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CheckFaithfulnessWorkerTest {

    private final CheckFaithfulnessWorker worker = new CheckFaithfulnessWorker();

    @Test
    void taskDefName() {
        assertEquals("qg_check_faithfulness", worker.getTaskDefName());
    }

    @Test
    void returnsPassWithAllClaimsSupported() {
        Task task = taskWith(new HashMap<>(Map.of(
                "answer", "Conductor orchestrates microservices with workers polling independently."
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("pass", result.getOutputData().get("decision"));
        assertEquals(1.0, (double) result.getOutputData().get("faithfulnessScore"), 0.001);
    }

    @Test
    void returnsThreeClaims() {
        Task task = taskWith(new HashMap<>(Map.of("answer", "test answer")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> claims = (List<Map<String, Object>>) result.getOutputData().get("claims");
        assertNotNull(claims);
        assertEquals(3, claims.size());
    }

    @Test
    void claimsHaveClaimAndSupportedFields() {
        Task task = taskWith(new HashMap<>(Map.of("answer", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> claims = (List<Map<String, Object>>) result.getOutputData().get("claims");

        for (Map<String, Object> claim : claims) {
            assertNotNull(claim.get("claim"));
            assertNotNull(claim.get("supported"));
            assertInstanceOf(String.class, claim.get("claim"));
            assertInstanceOf(Boolean.class, claim.get("supported"));
        }
    }

    @Test
    void returnsThresholdValue() {
        Task task = taskWith(new HashMap<>(Map.of("answer", "test")));
        TaskResult result = worker.execute(task);

        assertEquals(0.8, result.getOutputData().get("threshold"));
    }

    @Test
    void faithfulnessScoreAboveThreshold() {
        Task task = taskWith(new HashMap<>(Map.of("answer", "test")));
        TaskResult result = worker.execute(task);

        double score = (double) result.getOutputData().get("faithfulnessScore");
        double threshold = (double) result.getOutputData().get("threshold");
        assertTrue(score >= threshold);
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
