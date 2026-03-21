package competitiveagents.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SelectWinnerWorkerTest {

    private final SelectWinnerWorker worker = new SelectWinnerWorker();

    @Test
    void taskDefName() {
        assertEquals("comp_select_winner", worker.getTaskDefName());
    }

    @Test
    void selectsWinnerAndProducesRankings() {
        Map<String, Object> judgment = Map.of(
                "winner", "solver3",
                "winnerTitle", "Incremental Process Improvement",
                "reasoning", "Incremental Process Improvement wins with best overall score.",
                "scores", List.of(
                        Map.of("solverId", "solver3", "costScore", 9, "innovationScore", 5, "riskScore", 10, "totalScore", 24),
                        Map.of("solverId", "solver2", "costScore", 7, "innovationScore", 7, "riskScore", 8, "totalScore", 22),
                        Map.of("solverId", "solver1", "costScore", 4, "innovationScore", 9, "riskScore", 4, "totalScore", 17)
                ));

        Map<String, Object> allSolutions = Map.of(
                "solution1", Map.of("approach", "creative", "title", "AI-Powered Adaptive System",
                        "estimatedCost", "$120K", "timeline", "6 months"),
                "solution2", Map.of("approach", "analytical", "title", "Data-Driven Optimization Framework",
                        "estimatedCost", "$85K", "timeline", "4 months"),
                "solution3", Map.of("approach", "practical", "title", "Incremental Process Improvement",
                        "estimatedCost", "$45K", "timeline", "2 months")
        );

        Task task = taskWith(Map.of("judgment", judgment, "allSolutions", allSolutions));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> winner = (Map<String, Object>) result.getOutputData().get("winner");
        assertNotNull(winner);
        assertEquals("solver3", winner.get("solverId"));
        assertEquals("Incremental Process Improvement", winner.get("title"));
        assertEquals("practical", winner.get("approach"));
        assertEquals("$45K", winner.get("estimatedCost"));
        assertEquals("2 months", winner.get("timeline"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rankings = (List<Map<String, Object>>) result.getOutputData().get("rankings");
        assertEquals(3, rankings.size());
        assertEquals(1, rankings.get(0).get("rank"));
        assertEquals("solver3", rankings.get(0).get("solverId"));
        assertEquals(2, rankings.get(1).get("rank"));
        assertEquals("solver2", rankings.get(1).get("solverId"));
        assertEquals(3, rankings.get(2).get("rank"));
        assertEquals("solver1", rankings.get(2).get("solverId"));
    }

    @Test
    void winnerContainsReasoning() {
        Map<String, Object> judgment = Map.of(
                "winner", "solver1",
                "winnerTitle", "AI Solution",
                "reasoning", "AI wins because of innovation.",
                "scores", List.of(
                        Map.of("solverId", "solver1", "totalScore", 20)));

        Map<String, Object> allSolutions = Map.of(
                "solution1", Map.of("approach", "creative", "title", "AI Solution",
                        "estimatedCost", "$120K", "timeline", "6 months"));

        Task task = taskWith(Map.of("judgment", judgment, "allSolutions", allSolutions));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> winner = (Map<String, Object>) result.getOutputData().get("winner");
        assertEquals("AI wins because of innovation.", winner.get("reasoning"));
    }

    @Test
    void rankingsContainTitlesAndScores() {
        Map<String, Object> judgment = Map.of(
                "winner", "solver2",
                "winnerTitle", "Data Framework",
                "reasoning", "Data wins.",
                "scores", List.of(
                        Map.of("solverId", "solver2", "totalScore", 22),
                        Map.of("solverId", "solver1", "totalScore", 17)
                ));

        Map<String, Object> allSolutions = Map.of(
                "solution1", Map.of("title", "AI System"),
                "solution2", Map.of("title", "Data Framework"));

        Task task = taskWith(Map.of("judgment", judgment, "allSolutions", allSolutions));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rankings = (List<Map<String, Object>>) result.getOutputData().get("rankings");
        assertEquals(2, rankings.size());
        assertEquals("Data Framework", rankings.get(0).get("title"));
        assertEquals(22, rankings.get(0).get("totalScore"));
        assertEquals("AI System", rankings.get(1).get("title"));
        assertEquals(17, rankings.get(1).get("totalScore"));
    }

    @Test
    void handlesNullJudgment() {
        Map<String, Object> input = new HashMap<>();
        input.put("judgment", null);
        input.put("allSolutions", Map.of());

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> winner = (Map<String, Object>) result.getOutputData().get("winner");
        assertNotNull(winner);
        assertEquals("unknown", winner.get("solverId"));
    }

    @Test
    void handlesNullAllSolutions() {
        Map<String, Object> judgment = Map.of(
                "winner", "solver1",
                "winnerTitle", "Test",
                "reasoning", "Reason",
                "scores", List.of(
                        Map.of("solverId", "solver1", "totalScore", 20)));

        Map<String, Object> input = new HashMap<>();
        input.put("judgment", judgment);
        input.put("allSolutions", null);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> winner = (Map<String, Object>) result.getOutputData().get("winner");
        assertNotNull(winner);
        // When allSolutions is null, approach/cost/timeline won't be set
        assertFalse(winner.containsKey("approach"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("winner"));
        assertNotNull(result.getOutputData().get("rankings"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
