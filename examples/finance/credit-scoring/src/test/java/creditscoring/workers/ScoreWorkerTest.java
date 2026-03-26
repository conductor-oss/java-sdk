package creditscoring.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ScoreWorkerTest {

    private final ScoreWorker worker = new ScoreWorker();

    @Test
    void taskDefName() {
        assertEquals("csc_score", worker.getTaskDefName());
    }

    @Test
    void computesScoreFromStandardFactors() {
        Map<String, Object> factors = Map.of(
                "paymentHistory", Map.of("weight", 35, "score", 92),
                "utilization", Map.of("weight", 30, "score", 85),
                "creditAge", Map.of("weight", 15, "score", 95),
                "creditMix", Map.of("weight", 10, "score", 88),
                "newCredit", Map.of("weight", 10, "score", 80));
        Task task = taskWith(Map.of("applicantId", "APP-001", "factors", factors));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        int score = (int) result.getOutputData().get("score");
        assertTrue(score > 300 && score <= 850, "Score should be in FICO range: " + score);
    }

    @Test
    void returnsModel() {
        Map<String, Object> factors = Map.of(
                "paymentHistory", Map.of("weight", 35, "score", 92));
        Task task = taskWith(Map.of("applicantId", "APP-002", "factors", factors));
        TaskResult result = worker.execute(task);

        assertEquals("FICO-8", result.getOutputData().get("model"));
    }

    @Test
    void returnsComputedAt() {
        Map<String, Object> factors = Map.of(
                "paymentHistory", Map.of("weight", 35, "score", 92));
        Task task = taskWith(Map.of("applicantId", "APP-003", "factors", factors));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("computedAt"));
    }

    @Test
    void zeroFactorsReturnsBaseScore() {
        Task task = taskWith(Map.of("applicantId", "APP-004", "factors", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(300, result.getOutputData().get("score"));
    }

    @Test
    void perfectScoresReturnHighScore() {
        Map<String, Object> factors = Map.of(
                "paymentHistory", Map.of("weight", 35, "score", 100),
                "utilization", Map.of("weight", 30, "score", 100),
                "creditAge", Map.of("weight", 15, "score", 100),
                "creditMix", Map.of("weight", 10, "score", 100),
                "newCredit", Map.of("weight", 10, "score", 100));
        Task task = taskWith(Map.of("applicantId", "APP-005", "factors", factors));
        TaskResult result = worker.execute(task);

        assertEquals(850, result.getOutputData().get("score"));
    }

    @Test
    void handlesNullFactors() {
        Map<String, Object> input = new HashMap<>();
        input.put("applicantId", "APP-006");
        input.put("factors", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(300, result.getOutputData().get("score"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(300, result.getOutputData().get("score"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
