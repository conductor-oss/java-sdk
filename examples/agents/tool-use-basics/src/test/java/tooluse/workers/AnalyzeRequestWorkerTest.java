package tooluse.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzeRequestWorkerTest {

    private final AnalyzeRequestWorker worker = new AnalyzeRequestWorker();

    @Test
    void taskDefName() {
        assertEquals("tu_analyze_request", worker.getTaskDefName());
    }

    @Test
    void weatherRequestReturnsGetWeatherIntent() {
        Task task = taskWith(Map.of("userRequest", "What's the weather like in San Francisco?"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("get_weather", result.getOutputData().get("intent"));
        assertEquals(0.95, result.getOutputData().get("confidence"));

        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) result.getOutputData().get("analysis");
        assertNotNull(analysis);
        assertEquals("get_weather", analysis.get("intent"));
        assertEquals("simple", analysis.get("complexity"));
        assertEquals(false, analysis.get("requiresMultipleTools"));
    }

    @Test
    void calculateRequestReturnsCalculateIntent() {
        Task task = taskWith(Map.of("userRequest", "Calculate the sum of 5 and 10"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("calculate", result.getOutputData().get("intent"));

        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) result.getOutputData().get("analysis");
        @SuppressWarnings("unchecked")
        List<String> entities = (List<String>) analysis.get("entities");
        assertTrue(entities.contains("calculation"));
    }

    @Test
    void searchRequestReturnsSearchIntent() {
        Task task = taskWith(Map.of("userRequest", "Search for recent AI news"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("search", result.getOutputData().get("intent"));

        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) result.getOutputData().get("analysis");
        @SuppressWarnings("unchecked")
        List<String> entities = (List<String>) analysis.get("entities");
        assertTrue(entities.contains("search"));
    }

    @Test
    void unknownRequestReturnsGeneralIntent() {
        Task task = taskWith(Map.of("userRequest", "Tell me a joke"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("general", result.getOutputData().get("intent"));
    }

    @Test
    void analysisContainsEntities() {
        Task task = taskWith(Map.of("userRequest", "What's the weather in NYC?"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> analysis = (Map<String, Object>) result.getOutputData().get("analysis");
        @SuppressWarnings("unchecked")
        List<String> entities = (List<String>) analysis.get("entities");
        assertNotNull(entities);
        assertFalse(entities.isEmpty());
        assertTrue(entities.contains("weather"));
    }

    @Test
    void handlesNullUserRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("userRequest", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("intent"));
        assertNotNull(result.getOutputData().get("analysis"));
    }

    @Test
    void handlesMissingUserRequest() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("general", result.getOutputData().get("intent"));
    }

    @Test
    void handlesBlankUserRequest() {
        Task task = taskWith(Map.of("userRequest", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("general", result.getOutputData().get("intent"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
