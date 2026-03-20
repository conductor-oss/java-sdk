package cohere.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CohereGenerateWorkerTest {

    private final CohereGenerateWorker worker = new CohereGenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("cohere_generate", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsThreeGenerations() {
        Task task = taskWith(new HashMap<>(Map.of("requestBody", Map.of())));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> apiResponse = (Map<String, Object>) result.getOutputData().get("apiResponse");
        assertNotNull(apiResponse);

        List<Map<String, Object>> generations = (List<Map<String, Object>>) apiResponse.get("generations");
        assertEquals(3, generations.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void generationsHaveCorrectLikelihoods() {
        Task task = taskWith(new HashMap<>(Map.of("requestBody", Map.of())));

        TaskResult result = worker.execute(task);

        Map<String, Object> apiResponse = (Map<String, Object>) result.getOutputData().get("apiResponse");
        List<Map<String, Object>> generations = (List<Map<String, Object>>) apiResponse.get("generations");

        // In simulated mode (no COHERE_API_KEY), likelihoods are fixed
        if (System.getenv("COHERE_API_KEY") == null || System.getenv("COHERE_API_KEY").isBlank()) {
            assertEquals(-1.82, ((Number) generations.get(0).get("likelihood")).doubleValue());
            assertEquals(-1.65, ((Number) generations.get(1).get("likelihood")).doubleValue());
            assertEquals(-1.91, ((Number) generations.get(2).get("likelihood")).doubleValue());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void generationsHaveCompleteFinishReason() {
        Task task = taskWith(new HashMap<>(Map.of("requestBody", Map.of())));

        TaskResult result = worker.execute(task);

        Map<String, Object> apiResponse = (Map<String, Object>) result.getOutputData().get("apiResponse");
        List<Map<String, Object>> generations = (List<Map<String, Object>>) apiResponse.get("generations");

        // In simulated mode, verify finish_reason and text content
        if (System.getenv("COHERE_API_KEY") == null || System.getenv("COHERE_API_KEY").isBlank()) {
            for (Map<String, Object> gen : generations) {
                assertEquals("COMPLETE", gen.get("finish_reason"));
                assertNotNull(gen.get("text"));
                assertTrue(((String) gen.get("text")).length() > 0);
                assertTrue(((String) gen.get("text")).length() > 10);
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void metaContainsApiVersionAndBilledUnits() {
        Task task = taskWith(new HashMap<>(Map.of("requestBody", Map.of())));

        TaskResult result = worker.execute(task);

        Map<String, Object> apiResponse = (Map<String, Object>) result.getOutputData().get("apiResponse");

        // In simulated mode, meta is always present
        if (System.getenv("COHERE_API_KEY") == null || System.getenv("COHERE_API_KEY").isBlank()) {
            Map<String, Object> meta = (Map<String, Object>) apiResponse.get("meta");
            assertNotNull(meta);

            Map<String, Object> apiVersion = (Map<String, Object>) meta.get("api_version");
            assertEquals("1", apiVersion.get("version"));

            Map<String, Object> billedUnits = (Map<String, Object>) meta.get("billed_units");
            assertEquals(42, billedUnits.get("input_tokens"));
            assertEquals(185, billedUnits.get("output_tokens"));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
