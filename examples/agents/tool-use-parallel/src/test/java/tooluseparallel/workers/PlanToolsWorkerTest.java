package tooluseparallel.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlanToolsWorkerTest {

    private final PlanToolsWorker worker = new PlanToolsWorker();

    @Test
    void taskDefName() {
        assertEquals("tp_plan_tools", worker.getTaskDefName());
    }

    @Test
    void returnsToolConfigs() {
        Task task = taskWith(Map.of("userRequest", "morning briefing", "location", "San Francisco, CA"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("toolConfigs"));
    }

    @Test
    void toolConfigsContainsWeather() {
        Task task = taskWith(Map.of("userRequest", "morning briefing", "location", "NYC"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> toolConfigs = (Map<String, Object>) result.getOutputData().get("toolConfigs");
        @SuppressWarnings("unchecked")
        Map<String, Object> weather = (Map<String, Object>) toolConfigs.get("weather");
        assertEquals("fahrenheit", weather.get("units"));
        assertEquals(true, weather.get("includeHourly"));
    }

    @Test
    void toolConfigsContainsNewsTopics() {
        Task task = taskWith(Map.of("userRequest", "briefing"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> toolConfigs = (Map<String, Object>) result.getOutputData().get("toolConfigs");
        @SuppressWarnings("unchecked")
        List<String> newsTopics = (List<String>) toolConfigs.get("newsTopics");
        assertEquals(3, newsTopics.size());
        assertTrue(newsTopics.contains("technology"));
        assertTrue(newsTopics.contains("business"));
        assertTrue(newsTopics.contains("world"));
    }

    @Test
    void toolConfigsContainsTickers() {
        Task task = taskWith(Map.of("userRequest", "stocks update"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> toolConfigs = (Map<String, Object>) result.getOutputData().get("toolConfigs");
        @SuppressWarnings("unchecked")
        List<String> tickers = (List<String>) toolConfigs.get("tickers");
        assertEquals(4, tickers.size());
        assertTrue(tickers.contains("AAPL"));
        assertTrue(tickers.contains("GOOGL"));
        assertTrue(tickers.contains("MSFT"));
        assertTrue(tickers.contains("AMZN"));
    }

    @Test
    void returnsToolCountOfThree() {
        Task task = taskWith(Map.of("userRequest", "morning briefing"));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("toolCount"));
    }

    @Test
    void handlesNullUserRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("userRequest", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("toolConfigs"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(3, result.getOutputData().get("toolCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
