package competitiveagents.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JudgeAgentWorkerTest {

    private final JudgeAgentWorker worker = new JudgeAgentWorker();

    @Test
    void taskDefName() {
        assertEquals("comp_judge_agent", worker.getTaskDefName());
    }

    @Test
    void judgesThreeSolutionsAndPicksWinner() {
        Map<String, Object> sol1 = Map.of(
                "approach", "creative", "title", "AI-Powered Adaptive System",
                "estimatedCost", "$120K", "innovationScore", 9, "riskLevel", "medium-high");
        Map<String, Object> sol2 = Map.of(
                "approach", "analytical", "title", "Data-Driven Optimization Framework",
                "estimatedCost", "$85K", "innovationScore", 7, "riskLevel", "low");
        Map<String, Object> sol3 = Map.of(
                "approach", "practical", "title", "Incremental Process Improvement",
                "estimatedCost", "$45K", "innovationScore", 5, "riskLevel", "very-low");

        Task task = taskWith(Map.of("solution1", sol1, "solution2", sol2, "solution3", sol3,
                "problem", "Test", "criteria", "cost,innovation,risk"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> judgment = (Map<String, Object>) result.getOutputData().get("judgment");
        assertNotNull(judgment);
        assertNotNull(judgment.get("winner"));
        assertNotNull(judgment.get("winnerTitle"));
        assertNotNull(judgment.get("reasoning"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scores = (List<Map<String, Object>>) judgment.get("scores");
        assertEquals(3, scores.size());

        // Scores should be sorted descending by totalScore
        int prevTotal = Integer.MAX_VALUE;
        for (Map<String, Object> s : scores) {
            int total = (int) s.get("totalScore");
            assertTrue(total <= prevTotal, "Scores should be sorted descending");
            prevTotal = total;
        }
    }

    @Test
    void solver3WinsWithDefaultSolutions() {
        // solver3: cost=$45K -> costScore=9, innovation=5, risk=very-low -> riskScore=10, total=24
        // solver2: cost=$85K -> costScore=6, innovation=7, risk=low -> riskScore=8, total=21
        // solver1: cost=$120K -> costScore=4, innovation=9, risk=medium-high -> riskScore=4, total=17
        Map<String, Object> sol1 = Map.of(
                "title", "AI-Powered Adaptive System",
                "estimatedCost", "$120K", "innovationScore", 9, "riskLevel", "medium-high");
        Map<String, Object> sol2 = Map.of(
                "title", "Data-Driven Optimization Framework",
                "estimatedCost", "$85K", "innovationScore", 7, "riskLevel", "low");
        Map<String, Object> sol3 = Map.of(
                "title", "Incremental Process Improvement",
                "estimatedCost", "$45K", "innovationScore", 5, "riskLevel", "very-low");

        Task task = taskWith(Map.of("solution1", sol1, "solution2", sol2, "solution3", sol3,
                "problem", "Test", "criteria", "all"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> judgment = (Map<String, Object>) result.getOutputData().get("judgment");
        assertEquals("solver3", judgment.get("winner"));
        assertEquals("Incremental Process Improvement", judgment.get("winnerTitle"));
    }

    @Test
    void scoresContainAllFields() {
        Map<String, Object> sol1 = Map.of(
                "title", "S1", "estimatedCost", "$100K", "innovationScore", 6, "riskLevel", "medium");
        Map<String, Object> sol2 = Map.of(
                "title", "S2", "estimatedCost", "$60K", "innovationScore", 8, "riskLevel", "low");
        Map<String, Object> sol3 = Map.of(
                "title", "S3", "estimatedCost", "$30K", "innovationScore", 4, "riskLevel", "high");

        Task task = taskWith(Map.of("solution1", sol1, "solution2", sol2, "solution3", sol3,
                "problem", "Test", "criteria", "all"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> judgment = (Map<String, Object>) result.getOutputData().get("judgment");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scores = (List<Map<String, Object>>) judgment.get("scores");
        for (Map<String, Object> s : scores) {
            assertTrue(s.containsKey("solverId"));
            assertTrue(s.containsKey("costScore"));
            assertTrue(s.containsKey("innovationScore"));
            assertTrue(s.containsKey("riskScore"));
            assertTrue(s.containsKey("totalScore"));
        }
    }

    @Test
    void handlesNullSolutions() {
        Map<String, Object> input = new HashMap<>();
        input.put("solution1", null);
        input.put("solution2", null);
        input.put("solution3", null);
        input.put("problem", "Test");
        input.put("criteria", "all");

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> judgment = (Map<String, Object>) result.getOutputData().get("judgment");
        assertNotNull(judgment);
        // All null solutions get default scores of 5+5+5=15, so any can win (all tied)
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> scores = (List<Map<String, Object>>) judgment.get("scores");
        assertEquals(3, scores.size());
    }

    @Test
    void handlesMissingSolutions() {
        Task task = taskWith(Map.of("problem", "Test"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("judgment"));
    }

    @Test
    void costScoreFromEstimate() {
        assertEquals(9, JudgeAgentWorker.costScoreFromEstimate("$45K"));
        assertEquals(6, JudgeAgentWorker.costScoreFromEstimate("$85K"));
        assertEquals(4, JudgeAgentWorker.costScoreFromEstimate("$120K"));
        assertEquals(5, JudgeAgentWorker.costScoreFromEstimate(null));
        assertEquals(5, JudgeAgentWorker.costScoreFromEstimate("free"));
        assertEquals(9, JudgeAgentWorker.costScoreFromEstimate("$50K"));
        assertEquals(6, JudgeAgentWorker.costScoreFromEstimate("$100K"));
        assertEquals(3, JudgeAgentWorker.costScoreFromEstimate("$200K"));
    }

    @Test
    void riskScoreFromLevel() {
        assertEquals(10, JudgeAgentWorker.riskScoreFromLevel("very-low"));
        assertEquals(8, JudgeAgentWorker.riskScoreFromLevel("low"));
        assertEquals(6, JudgeAgentWorker.riskScoreFromLevel("medium"));
        assertEquals(4, JudgeAgentWorker.riskScoreFromLevel("medium-high"));
        assertEquals(2, JudgeAgentWorker.riskScoreFromLevel("high"));
        assertEquals(5, JudgeAgentWorker.riskScoreFromLevel(null));
        assertEquals(5, JudgeAgentWorker.riskScoreFromLevel("unknown"));
    }

    @Test
    void reasoningContainsWinnerTitle() {
        Map<String, Object> sol1 = Map.of(
                "title", "AI Solution", "estimatedCost", "$120K",
                "innovationScore", 9, "riskLevel", "medium-high");
        Map<String, Object> sol2 = Map.of(
                "title", "Data Solution", "estimatedCost", "$85K",
                "innovationScore", 7, "riskLevel", "low");
        Map<String, Object> sol3 = Map.of(
                "title", "Simple Solution", "estimatedCost", "$45K",
                "innovationScore", 5, "riskLevel", "very-low");

        Task task = taskWith(Map.of("solution1", sol1, "solution2", sol2, "solution3", sol3,
                "problem", "Test", "criteria", "all"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> judgment = (Map<String, Object>) result.getOutputData().get("judgment");
        String reasoning = (String) judgment.get("reasoning");
        String winnerTitle = (String) judgment.get("winnerTitle");
        assertTrue(reasoning.contains(winnerTitle));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
