package ragcode.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmbedCodeQueryWorkerTest {

    private final EmbedCodeQueryWorker worker = new EmbedCodeQueryWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_embed_code_query", worker.getTaskDefName());
    }

    @Test
    void returnsEmbedding() {
        Task task = taskWith(new HashMap<>(Map.of(
                "parsedIntent", "find_function_usage",
                "keywords", List.of("function", "usage"),
                "language", "java"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Double> embedding = (List<Double>) result.getOutputData().get("embedding");
        assertNotNull(embedding);
        assertFalse(embedding.isEmpty());
    }

    @Test
    void returnsCodeFilterWithLanguage() {
        Task task = taskWith(new HashMap<>(Map.of(
                "parsedIntent", "find_function_usage",
                "keywords", List.of("function"),
                "language", "python"
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> codeFilter = (Map<String, Object>) result.getOutputData().get("codeFilter");
        assertNotNull(codeFilter);
        assertEquals("python", codeFilter.get("language"));

        @SuppressWarnings("unchecked")
        List<String> nodeTypes = (List<String>) codeFilter.get("nodeTypes");
        assertNotNull(nodeTypes);
        assertEquals(3, nodeTypes.size());
        assertTrue(nodeTypes.contains("function_declaration"));
        assertTrue(nodeTypes.contains("method_definition"));
        assertTrue(nodeTypes.contains("call_expression"));
    }

    @Test
    void defaultsLanguageToJava() {
        Task task = taskWith(new HashMap<>(Map.of(
                "parsedIntent", "find_function_usage"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> codeFilter = (Map<String, Object>) result.getOutputData().get("codeFilter");
        assertEquals("java", codeFilter.get("language"));
    }

    @Test
    void handlesNullKeywords() {
        Task task = taskWith(new HashMap<>(Map.of(
                "parsedIntent", "find_function_usage",
                "language", "java"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("embedding"));
        assertNotNull(result.getOutputData().get("codeFilter"));
    }

    @Test
    void handlesMissingAllInputs() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("embedding"));
        assertNotNull(result.getOutputData().get("codeFilter"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
