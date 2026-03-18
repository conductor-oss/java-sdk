package agentsupervisor.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DocumenterAgentWorkerTest {

    private final DocumenterAgentWorker worker = new DocumenterAgentWorker();

    @Test
    void taskDefName() {
        assertEquals("sup_documenter_agent", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsDocumentationResult() {
        Task task = taskWith(new HashMap<>(Map.of(
                "task", "Write documentation for user-authentication",
                "feature", "user-authentication",
                "systemPrompt", "You are a documenter agent."
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> docResult = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(docResult);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsThreeDocuments() {
        Task task = taskWith(new HashMap<>(Map.of("feature", "user-authentication")));
        TaskResult result = worker.execute(task);

        Map<String, Object> docResult = (Map<String, Object>) result.getOutputData().get("result");
        List<String> documentsCreated = (List<String>) docResult.get("documentsCreated");
        assertNotNull(documentsCreated);
        assertEquals(3, documentsCreated.size());
        assertTrue(documentsCreated.contains("API_REFERENCE.md"));
        assertTrue(documentsCreated.contains("SETUP_GUIDE.md"));
        assertTrue(documentsCreated.contains("EXAMPLES.md"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void containsSectionsIncludingOverview() {
        Task task = taskWith(new HashMap<>(Map.of("feature", "user-authentication")));
        TaskResult result = worker.execute(task);

        Map<String, Object> docResult = (Map<String, Object>) result.getOutputData().get("result");
        List<String> sections = (List<String>) docResult.get("sections");
        assertNotNull(sections);
        assertTrue(sections.size() >= 3);
        assertTrue(sections.contains("Overview"));
        assertTrue(sections.contains("Configuration"));
        assertTrue(sections.contains("Troubleshooting"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsWordCount() {
        Task task = taskWith(new HashMap<>(Map.of("feature", "user-authentication")));
        TaskResult result = worker.execute(task);

        Map<String, Object> docResult = (Map<String, Object>) result.getOutputData().get("result");
        int wordCount = ((Number) docResult.get("wordCount")).intValue();
        assertTrue(wordCount > 0, "Word count should be positive");
    }

    @Test
    @SuppressWarnings("unchecked")
    void statusIsComplete() {
        Task task = taskWith(new HashMap<>(Map.of("feature", "user-authentication")));
        TaskResult result = worker.execute(task);

        Map<String, Object> docResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("complete", docResult.get("status"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void documentationContainsFeatureName() {
        Task task = taskWith(new HashMap<>(Map.of("feature", "user-authentication")));
        TaskResult result = worker.execute(task);

        Map<String, Object> docResult = (Map<String, Object>) result.getOutputData().get("result");
        String documentation = (String) docResult.get("documentation");
        assertNotNull(documentation);
        assertTrue(documentation.contains("user-authentication") || documentation.contains("User Authentication"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesDefaultFeatureWhenMissing() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> docResult = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(docResult);
        assertEquals("complete", docResult.get("status"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesDefaultFeatureWhenNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("feature", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> docResult = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(docResult);
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(new HashMap<>(Map.of("feature", "test-feature")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        @SuppressWarnings("unchecked")
        Map<String, Object> docResult = (Map<String, Object>) result.getOutputData().get("result");
        assertTrue(docResult.containsKey("documentsCreated"));
        assertTrue(docResult.containsKey("sections"));
        assertTrue(docResult.containsKey("wordCount"));
        assertTrue(docResult.containsKey("status"));
        assertTrue(docResult.containsKey("documentation"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void generatesDocFromCodeInput() {
        String code = """
                public class OrderService {
                    public Map<String, Object> createOrder(Map<String, Object> data) {
                        return Map.of();
                    }
                    public Map<String, Object> getOrder(String id) {
                        return Map.of();
                    }
                }
                """;
        Task task = taskWith(new HashMap<>(Map.of("feature", "orders", "code", code)));
        TaskResult result = worker.execute(task);

        Map<String, Object> docResult = (Map<String, Object>) result.getOutputData().get("result");
        String documentation = (String) docResult.get("documentation");
        assertNotNull(documentation);
        assertTrue(documentation.contains("OrderService"));
        assertTrue(documentation.contains("createOrder"));
        assertTrue(documentation.contains("@param"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
