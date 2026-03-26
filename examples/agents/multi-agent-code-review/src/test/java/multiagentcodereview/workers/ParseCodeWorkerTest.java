package multiagentcodereview.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ParseCodeWorkerTest {

    private final ParseCodeWorker worker = new ParseCodeWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_parse_code", worker.getTaskDefName());
    }

    @Test
    void returnsAstWithExpectedFields() {
        Task task = taskWith(Map.of("code", "const x = 1;", "language", "javascript"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> ast = (Map<String, Object>) result.getOutputData().get("ast");
        assertNotNull(ast);
        assertTrue(ast.containsKey("functions"));
        assertTrue(ast.containsKey("imports"));
        assertTrue(ast.containsKey("lines"));
        assertTrue(ast.containsKey("complexity"));
    }

    @Test
    void astContainsCorrectValues() {
        Task task = taskWith(Map.of("code", "function test() {}", "language", "javascript"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> ast = (Map<String, Object>) result.getOutputData().get("ast");
        assertEquals(142, ast.get("lines"));
        assertEquals(18, ast.get("complexity"));

        @SuppressWarnings("unchecked")
        List<String> functions = (List<String>) ast.get("functions");
        assertEquals(4, functions.size());
        assertTrue(functions.contains("handleRequest"));
        assertTrue(functions.contains("processData"));

        @SuppressWarnings("unchecked")
        List<String> imports = (List<String>) ast.get("imports");
        assertEquals(4, imports.size());
        assertTrue(imports.contains("express"));
    }

    @Test
    void outputIncludesLanguage() {
        Task task = taskWith(Map.of("code", "x = 1", "language", "python"));
        TaskResult result = worker.execute(task);

        assertEquals("python", result.getOutputData().get("language"));
    }

    @Test
    void handlesNullCode() {
        Map<String, Object> input = new HashMap<>();
        input.put("code", null);
        input.put("language", "javascript");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("ast"));
    }

    @Test
    void handlesMissingCode() {
        Task task = taskWith(Map.of("language", "javascript"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("ast"));
    }

    @Test
    void handlesBlankLanguage() {
        Task task = taskWith(Map.of("code", "x = 1", "language", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("javascript", result.getOutputData().get("language"));
    }

    @Test
    void handlesMissingLanguage() {
        Task task = taskWith(Map.of("code", "x = 1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("javascript", result.getOutputData().get("language"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
