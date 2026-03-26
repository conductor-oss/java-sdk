package ragcode.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseQueryWorkerTest {

    private final ParseQueryWorker worker = new ParseQueryWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_parse_query", worker.getTaskDefName());
    }

    @Test
    void returnsFixedIntent() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How do I look up a user by ID?",
                "language", "java"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("find_function_usage", result.getOutputData().get("intent"));
    }

    @Test
    void returnsKeywordsIncludingLanguage() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How do I look up a user by ID?",
                "language", "python"
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) result.getOutputData().get("keywords");
        assertNotNull(keywords);
        assertTrue(keywords.contains("function"));
        assertTrue(keywords.contains("usage"));
        assertTrue(keywords.contains("example"));
        assertTrue(keywords.contains("python"));
    }

    @Test
    void returnsLanguageFromInput() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "language", "typescript"
        )));
        TaskResult result = worker.execute(task);

        assertEquals("typescript", result.getOutputData().get("language"));
    }

    @Test
    void defaultsLanguageToJava() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("java", result.getOutputData().get("language"));

        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) result.getOutputData().get("keywords");
        assertTrue(keywords.contains("java"));
    }

    @Test
    void handlesMissingQuestion() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("intent"));
        assertNotNull(result.getOutputData().get("keywords"));
        assertNotNull(result.getOutputData().get("language"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
