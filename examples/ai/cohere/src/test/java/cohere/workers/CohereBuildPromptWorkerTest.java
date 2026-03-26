package cohere.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CohereBuildPromptWorkerTest {

    private final CohereBuildPromptWorker worker = new CohereBuildPromptWorker();

    @Test
    void taskDefName() {
        assertEquals("cohere_build_prompt", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildsRequestBodyWithAllParameters() {
        Task task = taskWith(new HashMap<>(Map.of(
                "productDescription", "SmartBoard Pro",
                "targetAudience", "Engineering managers",
                "model", "command",
                "maxTokens", 300,
                "temperature", 0.9,
                "k", 0,
                "p", 0.75,
                "numGenerations", 3)));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> requestBody = (Map<String, Object>) result.getOutputData().get("requestBody");
        assertNotNull(requestBody);
        assertEquals("command", requestBody.get("model"));
        assertEquals(300, requestBody.get("max_tokens"));
        assertEquals(0.9, requestBody.get("temperature"));
        assertEquals(0, requestBody.get("k"));
        assertEquals(0.75, requestBody.get("p"));
        assertEquals(3, requestBody.get("num_generations"));
        assertEquals("GENERATION", requestBody.get("return_likelihoods"));
        assertEquals("END", requestBody.get("truncate"));

        String prompt = (String) requestBody.get("prompt");
        assertNotNull(prompt);
        assertTrue(prompt.contains("SmartBoard Pro"));
        assertTrue(prompt.contains("Engineering managers"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesDefaultsWhenParametersMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "productDescription", "Test Product",
                "targetAudience", "Developers")));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> requestBody = (Map<String, Object>) result.getOutputData().get("requestBody");
        assertEquals("command", requestBody.get("model"));
        assertEquals(300, requestBody.get("max_tokens"));
        assertEquals(0.9, requestBody.get("temperature"));
        assertEquals(0, requestBody.get("k"));
        assertEquals(0.75, requestBody.get("p"));
        assertEquals(3, requestBody.get("num_generations"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void promptContainsProductAndAudience() {
        Task task = taskWith(new HashMap<>(Map.of(
                "productDescription", "Widget X",
                "targetAudience", "CTO personas")));

        TaskResult result = worker.execute(task);

        Map<String, Object> requestBody = (Map<String, Object>) result.getOutputData().get("requestBody");
        String prompt = (String) requestBody.get("prompt");
        assertTrue(prompt.contains("Widget X"));
        assertTrue(prompt.contains("CTO personas"));
        assertTrue(prompt.contains("marketing copy"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
