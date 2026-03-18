package llmchain.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChainPromptWorkerTest {

    private final ChainPromptWorker worker = new ChainPromptWorker();

    @Test
    void taskDefName() {
        assertEquals("chain_prompt", worker.getTaskDefName());
    }

    @Test
    void buildsFormattedPromptWithEmailAndCatalog() {
        Task task = taskWith(new HashMap<>(Map.of(
                "customerEmail", "I need help with my order",
                "productCatalog", "PROD-A,PROD-B"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String prompt = (String) result.getOutputData().get("formattedPrompt");
        assertNotNull(prompt);
        assertTrue(prompt.contains("I need help with my order"));
        assertTrue(prompt.contains("PROD-A,PROD-B"));
    }

    @Test
    void outputContainsModel() {
        Task task = taskWith(new HashMap<>(Map.of(
                "customerEmail", "test",
                "productCatalog", "PROD-1"
        )));
        TaskResult result = worker.execute(task);

        assertEquals("gpt-4", result.getOutputData().get("model"));
    }

    @Test
    void outputContainsExpectedFormat() {
        Task task = taskWith(new HashMap<>(Map.of(
                "customerEmail", "test",
                "productCatalog", "PROD-1"
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> expectedFormat =
                (Map<String, Object>) result.getOutputData().get("expectedFormat");
        assertNotNull(expectedFormat);
        assertTrue(expectedFormat.containsKey("intent"));
        assertTrue(expectedFormat.containsKey("sentiment"));
        assertTrue(expectedFormat.containsKey("suggestedProducts"));
        assertTrue(expectedFormat.containsKey("draftReply"));
    }

    @Test
    void promptIncludesFewShotExamples() {
        Task task = taskWith(new HashMap<>(Map.of(
                "customerEmail", "test email",
                "productCatalog", "PROD-1"
        )));
        TaskResult result = worker.execute(task);

        String prompt = (String) result.getOutputData().get("formattedPrompt");
        assertTrue(prompt.contains("Few-shot examples"));
        assertTrue(prompt.contains("Example 1"));
        assertTrue(prompt.contains("Example 2"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
