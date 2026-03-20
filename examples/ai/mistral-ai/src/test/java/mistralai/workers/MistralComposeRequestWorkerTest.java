package mistralai.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MistralComposeRequestWorkerTest {

    private final MistralComposeRequestWorker worker = new MistralComposeRequestWorker();

    @Test
    void taskDefName() {
        assertEquals("mistral_compose_request", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void composesRequestBodyWithAllInputs() {
        Task task = taskWith(new HashMap<>(Map.of(
                "document", "Test document content",
                "question", "What is the summary?",
                "model", "mistral-large-latest",
                "temperature", 0.5,
                "maxTokens", 512,
                "safePrompt", true
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> requestBody = (Map<String, Object>) result.getOutputData().get("requestBody");
        assertNotNull(requestBody);
        assertEquals("mistral-large-latest", requestBody.get("model"));
        assertEquals(0.5, requestBody.get("temperature"));
        assertEquals(512, requestBody.get("max_tokens"));
        assertEquals(true, requestBody.get("safe_prompt"));

        List<Map<String, Object>> messages = (List<Map<String, Object>>) requestBody.get("messages");
        assertEquals(2, messages.size());
        assertEquals("system", messages.get(0).get("role"));
        assertTrue(messages.get(0).get("content").toString().contains("legal document analyst"));
        assertEquals("user", messages.get(1).get("role"));
        assertTrue(messages.get(1).get("content").toString().contains("Test document content"));
        assertTrue(messages.get(1).get("content").toString().contains("What is the summary?"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesDefaultsWhenOptionalInputsMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "document", "Some document",
                "question", "Summarize"
        )));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> requestBody = (Map<String, Object>) result.getOutputData().get("requestBody");
        assertEquals("mistral-large-latest", requestBody.get("model"));
        assertEquals(0.7, requestBody.get("temperature"));
        assertEquals(1024, requestBody.get("max_tokens"));
        assertEquals(false, requestBody.get("safe_prompt"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void userMessageContainsDocumentAndQuestion() {
        Task task = taskWith(new HashMap<>(Map.of(
                "document", "CONTRACT: Party A agrees to pay Party B.",
                "question", "Who pays whom?"
        )));

        TaskResult result = worker.execute(task);

        Map<String, Object> requestBody = (Map<String, Object>) result.getOutputData().get("requestBody");
        List<Map<String, Object>> messages = (List<Map<String, Object>>) requestBody.get("messages");
        String userContent = (String) messages.get(1).get("content");

        assertTrue(userContent.startsWith("Document:\n"));
        assertTrue(userContent.contains("CONTRACT: Party A agrees to pay Party B."));
        assertTrue(userContent.contains("Question: Who pays whom?"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
