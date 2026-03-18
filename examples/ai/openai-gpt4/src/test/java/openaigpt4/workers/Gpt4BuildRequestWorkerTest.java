package openaigpt4.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Gpt4BuildRequestWorkerTest {

    private final Gpt4BuildRequestWorker worker = new Gpt4BuildRequestWorker();

    @Test
    void taskDefName() {
        assertEquals("gpt4_build_request", worker.getTaskDefName());
    }

    @Test
    void buildsRequestBodyWithAllParameters() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "Analyze revenue",
                "systemMessage", "You are an analyst",
                "model", "gpt-4.1-mini",
                "temperature", 0.5,
                "top_p", 0.9,
                "max_tokens", 512,
                "frequency_penalty", 0.1,
                "presence_penalty", 0.2
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("requestBody"));

        @SuppressWarnings("unchecked")
        Map<String, Object> requestBody = (Map<String, Object>) result.getOutputData().get("requestBody");
        assertEquals("gpt-4.1-mini", requestBody.get("model"));
        assertEquals(0.5, requestBody.get("temperature"));
        assertEquals(0.9, requestBody.get("top_p"));
        assertEquals(512, requestBody.get("max_tokens"));
        assertEquals(0.1, requestBody.get("frequency_penalty"));
        assertEquals(0.2, requestBody.get("presence_penalty"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) requestBody.get("messages");
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).get("role"));
        assertEquals("You are an analyst", messages.get(0).get("content"));
        assertEquals("user", messages.get(1).get("role"));
        assertEquals("Analyze revenue", messages.get(1).get("content"));
    }

    @Test
    void usesDefaultsWhenOptionalParametersMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "Hello"
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> requestBody = (Map<String, Object>) result.getOutputData().get("requestBody");
        assertEquals("gpt-4o-mini", requestBody.get("model"));
        assertEquals(0.7, requestBody.get("temperature"));
        assertEquals(1.0, requestBody.get("top_p"));
        assertEquals(1024, requestBody.get("max_tokens"));
        assertEquals(0.0, requestBody.get("frequency_penalty"));
        assertEquals(0.0, requestBody.get("presence_penalty"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) requestBody.get("messages");
        assertEquals("You are a helpful assistant.", messages.get(0).get("content"));
        assertEquals("Hello", messages.get(1).get("content"));
    }

    @Test
    void messagesArrayHasSystemAndUserRoles() {
        Task task = taskWith(new HashMap<>(Map.of(
                "prompt", "Test prompt",
                "systemMessage", "Test system"
        )));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> requestBody = (Map<String, Object>) result.getOutputData().get("requestBody");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) requestBody.get("messages");

        assertEquals("system", messages.get(0).get("role"));
        assertEquals("user", messages.get(1).get("role"));
    }

    @Test
    void outputContainsRequestBodyKey() {
        Task task = taskWith(new HashMap<>(Map.of("prompt", "test")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("requestBody"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
