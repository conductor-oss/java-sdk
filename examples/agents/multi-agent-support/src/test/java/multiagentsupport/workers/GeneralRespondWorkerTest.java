package multiagentsupport.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeneralRespondWorkerTest {

    private final GeneralRespondWorker worker = new GeneralRespondWorker();

    @Test
    void taskDefName() {
        assertEquals("cs_general_respond", worker.getTaskDefName());
    }

    @Test
    void returnsHelpfulResponse() {
        Task task = taskWith(Map.of(
                "subject", "How do I reset my password?",
                "description", "I forgot my password"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String response = (String) result.getOutputData().get("response");
        assertNotNull(response);
        assertTrue(response.contains("support team"));
    }

    @Test
    void returnsSuggestedDocs() {
        Task task = taskWith(Map.of(
                "subject", "General question",
                "description", "Need help"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> docs = (List<String>) result.getOutputData().get("suggestedDocs");
        assertNotNull(docs);
        assertEquals(3, docs.size());
        assertTrue(docs.contains("Getting Started Guide"));
        assertTrue(docs.contains("FAQ - Frequently Asked Questions"));
        assertTrue(docs.contains("API Documentation"));
    }

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("subject", null);
        input.put("description", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("response"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("response"));
        assertNotNull(result.getOutputData().get("suggestedDocs"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of("subject", "Test", "description", "Test"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("response"));
        assertTrue(result.getOutputData().containsKey("suggestedDocs"));
    }

    @Test
    void responseIncludesContactInfo() {
        Task task = taskWith(Map.of("subject", "Help", "description", "Need assistance"));
        TaskResult result = worker.execute(task);

        String response = (String) result.getOutputData().get("response");
        assertTrue(response.contains("live chat") || response.contains("contact"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
