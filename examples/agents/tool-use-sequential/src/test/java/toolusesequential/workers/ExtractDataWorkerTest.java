package toolusesequential.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtractDataWorkerTest {

    private final ExtractDataWorker worker = new ExtractDataWorker();

    @Test
    void taskDefName() {
        assertEquals("ts_extract_data", worker.getTaskDefName());
    }

    @Test
    void returnsFiveFacts() {
        Task task = taskWith(Map.of("query", "What is Conductor?", "pageContent", Map.of("title", "Test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getOutputData().get("data");
        assertNotNull(data);

        @SuppressWarnings("unchecked")
        List<String> facts = (List<String>) data.get("facts");
        assertNotNull(facts);
        assertEquals(5, facts.size());
    }

    @Test
    void returnsFiveKeyFeatures() {
        Task task = taskWith(Map.of("query", "features", "pageContent", Map.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getOutputData().get("data");
        @SuppressWarnings("unchecked")
        List<String> features = (List<String>) data.get("keyFeatures");
        assertNotNull(features);
        assertEquals(5, features.size());
    }

    @Test
    void returnsFiveUseCases() {
        Task task = taskWith(Map.of("query", "use cases", "pageContent", Map.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getOutputData().get("data");
        @SuppressWarnings("unchecked")
        List<String> useCases = (List<String>) data.get("useCases");
        assertNotNull(useCases);
        assertEquals(5, useCases.size());
    }

    @Test
    void returnsOriginAndType() {
        Task task = taskWith(Map.of("query", "origin"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getOutputData().get("data");
        assertEquals("Netflix", data.get("origin"));
        assertEquals("open-source", data.get("type"));
    }

    @Test
    void returnsRelevanceScore() {
        Task task = taskWith(Map.of("query", "Conductor"));
        TaskResult result = worker.execute(task);

        assertEquals(0.92, result.getOutputData().get("relevanceScore"));
    }

    @Test
    void handlesNullQuery() {
        Map<String, Object> input = new HashMap<>();
        input.put("query", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("data"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("data"));
    }

    @Test
    void factsContainConductorInfo() {
        Task task = taskWith(Map.of("query", "Conductor"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) result.getOutputData().get("data");
        @SuppressWarnings("unchecked")
        List<String> facts = (List<String>) data.get("facts");
        assertTrue(facts.get(0).contains("Conductor"));
        assertTrue(facts.get(1).contains("Netflix"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
