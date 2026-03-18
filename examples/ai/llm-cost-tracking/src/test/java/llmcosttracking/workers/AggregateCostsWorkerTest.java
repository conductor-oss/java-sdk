package llmcosttracking.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AggregateCostsWorkerTest {

    private final AggregateCostsWorker worker = new AggregateCostsWorker();

    @Test
    void taskDefName() {
        assertEquals("ct_aggregate_costs", worker.getTaskDefName());
    }

    @Test
    void aggregatesCostsCorrectly() {
        // GPT-4:   (120/1000)*0.03  + (350/1000)*0.06  = 0.0036 + 0.0210 = 0.0246
        // Claude:  (120/1000)*0.015 + (280/1000)*0.075 = 0.0018 + 0.0210 = 0.0228
        // Gemini:  (120/1000)*0.0005 + (400/1000)*0.0015 = 0.00006 + 0.0006 = 0.00066
        // Total cost: 0.04806, Total tokens: 1390
        Map<String, Object> gpt4Usage = Map.of(
                "model", "gpt-4", "inputTokens", 120, "outputTokens", 350);
        Map<String, Object> claudeUsage = Map.of(
                "model", "claude-3", "inputTokens", 120, "outputTokens", 280);
        Map<String, Object> geminiUsage = Map.of(
                "model", "gemini", "inputTokens", 120, "outputTokens", 400);

        Task task = taskWith(new HashMap<>(Map.of(
                "gpt4Usage", gpt4Usage,
                "claudeUsage", claudeUsage,
                "geminiUsage", geminiUsage
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breakdown =
                (List<Map<String, Object>>) result.getOutputData().get("breakdown");
        assertNotNull(breakdown);
        assertEquals(3, breakdown.size());

        // GPT-4 breakdown
        assertEquals("gpt-4", breakdown.get(0).get("model"));
        assertEquals(120, breakdown.get(0).get("inputTokens"));
        assertEquals(350, breakdown.get(0).get("outputTokens"));
        assertEquals("$0.0246", breakdown.get(0).get("cost"));

        // Claude breakdown
        assertEquals("claude-3", breakdown.get(1).get("model"));
        assertEquals(120, breakdown.get(1).get("inputTokens"));
        assertEquals(280, breakdown.get(1).get("outputTokens"));
        assertEquals("$0.0228", breakdown.get(1).get("cost"));

        // Gemini breakdown
        assertEquals("gemini", breakdown.get(2).get("model"));
        assertEquals(120, breakdown.get(2).get("inputTokens"));
        assertEquals(400, breakdown.get(2).get("outputTokens"));
        assertEquals("$0.0007", breakdown.get(2).get("cost"));

        assertEquals("$0.0481", result.getOutputData().get("totalCost"));
        assertEquals(1390, result.getOutputData().get("totalTokens"));
    }

    @Test
    void handlesSmallTokenCounts() {
        // GPT-4:  (10/1000)*0.03 + (20/1000)*0.06 = 0.0003 + 0.0012 = 0.0015
        // Claude: (10/1000)*0.015 + (20/1000)*0.075 = 0.00015 + 0.0015 = 0.00165
        // Gemini: (10/1000)*0.0005 + (20/1000)*0.0015 = 0.000005 + 0.00003 = 0.000035
        // Total cost: 0.003185, Total tokens: 90
        Map<String, Object> gpt4Usage = Map.of(
                "model", "gpt-4", "inputTokens", 10, "outputTokens", 20);
        Map<String, Object> claudeUsage = Map.of(
                "model", "claude-3", "inputTokens", 10, "outputTokens", 20);
        Map<String, Object> geminiUsage = Map.of(
                "model", "gemini", "inputTokens", 10, "outputTokens", 20);

        Task task = taskWith(new HashMap<>(Map.of(
                "gpt4Usage", gpt4Usage,
                "claudeUsage", claudeUsage,
                "geminiUsage", geminiUsage
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breakdown =
                (List<Map<String, Object>>) result.getOutputData().get("breakdown");
        assertEquals("$0.0015", breakdown.get(0).get("cost"));
        assertEquals("$0.0017", breakdown.get(1).get("cost"));
        assertEquals("$0.0000", breakdown.get(2).get("cost"));
        assertEquals(90, result.getOutputData().get("totalTokens"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
