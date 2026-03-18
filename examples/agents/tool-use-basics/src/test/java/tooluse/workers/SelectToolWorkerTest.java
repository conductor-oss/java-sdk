package tooluse.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SelectToolWorkerTest {

    private final SelectToolWorker worker = new SelectToolWorker();

    @Test
    void taskDefName() {
        assertEquals("tu_select_tool", worker.getTaskDefName());
    }

    @Test
    void weatherIntentSelectsWeatherApi() {
        Task task = taskWith(Map.of("intent", "get_weather"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("weather_api", result.getOutputData().get("selectedTool"));
        assertNotNull(result.getOutputData().get("toolDescription"));

        @SuppressWarnings("unchecked")
        Map<String, Object> toolArgs = (Map<String, Object>) result.getOutputData().get("toolArgs");
        assertEquals("San Francisco, CA", toolArgs.get("location"));
        assertEquals("fahrenheit", toolArgs.get("units"));
    }

    @Test
    void calculateIntentSelectsCalculator() {
        Task task = taskWith(Map.of("intent", "calculate"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("calculator", result.getOutputData().get("selectedTool"));

        @SuppressWarnings("unchecked")
        Map<String, Object> toolArgs = (Map<String, Object>) result.getOutputData().get("toolArgs");
        assertNotNull(toolArgs.get("expression"));
        assertEquals(2, toolArgs.get("precision"));
    }

    @Test
    void searchIntentSelectsWebSearch() {
        Task task = taskWith(Map.of("intent", "search"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("web_search", result.getOutputData().get("selectedTool"));

        @SuppressWarnings("unchecked")
        Map<String, Object> toolArgs = (Map<String, Object>) result.getOutputData().get("toolArgs");
        assertNotNull(toolArgs.get("query"));
        assertEquals(5, toolArgs.get("maxResults"));
    }

    @Test
    void unknownIntentSelectsGeneralHandler() {
        Task task = taskWith(Map.of("intent", "translate"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("general_handler", result.getOutputData().get("selectedTool"));
    }

    @Test
    void toolDescriptionIsPresent() {
        Task task = taskWith(Map.of("intent", "get_weather"));
        TaskResult result = worker.execute(task);

        String desc = (String) result.getOutputData().get("toolDescription");
        assertNotNull(desc);
        assertFalse(desc.isBlank());
    }

    @Test
    void handlesNullIntent() {
        Map<String, Object> input = new HashMap<>();
        input.put("intent", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("general_handler", result.getOutputData().get("selectedTool"));
    }

    @Test
    void handlesMissingIntent() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("general_handler", result.getOutputData().get("selectedTool"));
    }

    @Test
    void handlesBlankIntent() {
        Task task = taskWith(Map.of("intent", "  "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("general_handler", result.getOutputData().get("selectedTool"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
