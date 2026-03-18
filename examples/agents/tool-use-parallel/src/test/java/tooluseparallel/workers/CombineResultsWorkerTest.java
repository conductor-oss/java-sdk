package tooluseparallel.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CombineResultsWorkerTest {

    private final CombineResultsWorker worker = new CombineResultsWorker();

    @Test
    void taskDefName() {
        assertEquals("tp_combine_results", worker.getTaskDefName());
    }

    @Test
    void returnsBriefing() {
        Task task = taskWith(Map.of(
                "weather", Map.of("location", "SF", "high", 68),
                "news", List.of(Map.of("title", "Tech news")),
                "stocks", List.of(Map.of("ticker", "AAPL", "price", 178.52))
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("briefing"));
    }

    @Test
    void briefingContainsAllSections() {
        Task task = taskWith(Map.of(
                "weather", Map.of("temp", 58),
                "news", List.of(Map.of("title", "Headline")),
                "stocks", List.of(Map.of("ticker", "GOOGL"))
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> briefing = (Map<String, Object>) result.getOutputData().get("briefing");
        assertNotNull(briefing.get("weather"));
        assertNotNull(briefing.get("news"));
        assertNotNull(briefing.get("stocks"));
        assertNotNull(briefing.get("summary"));
    }

    @Test
    void returnsToolsUsed() {
        Task task = taskWith(Map.of(
                "weather", Map.of("temp", 58),
                "news", List.of(),
                "stocks", List.of()
        ));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> toolsUsed = (List<String>) result.getOutputData().get("toolsUsed");
        assertEquals(3, toolsUsed.size());
        assertTrue(toolsUsed.contains("weather_api"));
        assertTrue(toolsUsed.contains("news_api"));
        assertTrue(toolsUsed.contains("stock_api"));
    }

    @Test
    void returnsGeneratedAtTimestamp() {
        Task task = taskWith(Map.of(
                "weather", Map.of("temp", 58),
                "news", List.of(),
                "stocks", List.of()
        ));
        TaskResult result = worker.execute(task);

        assertEquals("2026-03-08T08:00:00Z", result.getOutputData().get("generatedAt"));
    }

    @Test
    void handlesNullWeather() {
        Map<String, Object> input = new HashMap<>();
        input.put("weather", null);
        input.put("news", List.of(Map.of("title", "News")));
        input.put("stocks", List.of(Map.of("ticker", "AAPL")));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> briefing = (Map<String, Object>) result.getOutputData().get("briefing");
        assertNotNull(briefing.get("weather"));
    }

    @Test
    void handlesNullNews() {
        Map<String, Object> input = new HashMap<>();
        input.put("weather", Map.of("temp", 58));
        input.put("news", null);
        input.put("stocks", List.of(Map.of("ticker", "MSFT")));
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesNullStocks() {
        Map<String, Object> input = new HashMap<>();
        input.put("weather", Map.of("temp", 58));
        input.put("news", List.of());
        input.put("stocks", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingAllInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("briefing"));
        assertNotNull(result.getOutputData().get("toolsUsed"));
        assertNotNull(result.getOutputData().get("generatedAt"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
