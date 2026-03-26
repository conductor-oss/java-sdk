package ragevaluation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AggregateScoresWorkerTest {

    private final AggregateScoresWorker worker = new AggregateScoresWorker();

    @Test
    void taskDefName() {
        assertEquals("re_aggregate_scores", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void computesOverallScoreAndPassVerdict() {
        Task task = taskWith(new HashMap<>(Map.of(
                "faithfulnessScore", 0.92,
                "faithfulnessReason", "Answer is well-supported by context.",
                "relevanceScore", 0.88,
                "relevanceReason", "Answer addresses the question.",
                "coherenceScore", 0.95,
                "coherenceReason", "Answer is well-structured."
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        double overallScore = (double) result.getOutputData().get("overallScore");
        // (0.92 + 0.88 + 0.95) / 3 = 0.9166...
        assertEquals(0.9167, overallScore, 0.0001);

        assertEquals("PASS", result.getOutputData().get("verdict"));

        Map<String, Object> breakdown = (Map<String, Object>) result.getOutputData().get("breakdown");
        assertNotNull(breakdown);
        assertEquals(3, breakdown.size());
        assertTrue(breakdown.containsKey("faithfulness"));
        assertTrue(breakdown.containsKey("relevance"));
        assertTrue(breakdown.containsKey("coherence"));

        Map<String, Object> faithfulness = (Map<String, Object>) breakdown.get("faithfulness");
        assertEquals(0.92, faithfulness.get("score"));
        assertNotNull(faithfulness.get("reason"));
    }

    @Test
    void marginalVerdict() {
        Task task = taskWith(new HashMap<>(Map.of(
                "faithfulnessScore", 0.7,
                "faithfulnessReason", "Partially supported.",
                "relevanceScore", 0.6,
                "relevanceReason", "Somewhat relevant.",
                "coherenceScore", 0.65,
                "coherenceReason", "Somewhat coherent."
        )));
        TaskResult result = worker.execute(task);

        // (0.7 + 0.6 + 0.65) / 3 = 0.65
        double overallScore = (double) result.getOutputData().get("overallScore");
        assertEquals(0.65, overallScore, 0.0001);
        assertEquals("MARGINAL", result.getOutputData().get("verdict"));
    }

    @Test
    void failVerdict() {
        Task task = taskWith(new HashMap<>(Map.of(
                "faithfulnessScore", 0.3,
                "faithfulnessReason", "Not supported.",
                "relevanceScore", 0.4,
                "relevanceReason", "Off topic.",
                "coherenceScore", 0.5,
                "coherenceReason", "Disjointed."
        )));
        TaskResult result = worker.execute(task);

        // (0.3 + 0.4 + 0.5) / 3 = 0.4
        double overallScore = (double) result.getOutputData().get("overallScore");
        assertEquals(0.4, overallScore, 0.0001);
        assertEquals("FAIL", result.getOutputData().get("verdict"));
    }

    @Test
    void boundaryPassAt08() {
        Task task = taskWith(new HashMap<>(Map.of(
                "faithfulnessScore", 0.8,
                "faithfulnessReason", "Reason.",
                "relevanceScore", 0.8,
                "relevanceReason", "Reason.",
                "coherenceScore", 0.8,
                "coherenceReason", "Reason."
        )));
        TaskResult result = worker.execute(task);

        assertEquals("PASS", result.getOutputData().get("verdict"));
    }

    @Test
    void boundaryMarginalAt06() {
        Task task = taskWith(new HashMap<>(Map.of(
                "faithfulnessScore", 0.6,
                "faithfulnessReason", "Reason.",
                "relevanceScore", 0.6,
                "relevanceReason", "Reason.",
                "coherenceScore", 0.6,
                "coherenceReason", "Reason."
        )));
        TaskResult result = worker.execute(task);

        assertEquals("MARGINAL", result.getOutputData().get("verdict"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
