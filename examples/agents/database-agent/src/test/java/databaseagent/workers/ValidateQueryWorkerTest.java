package databaseagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidateQueryWorkerTest {

    private final ValidateQueryWorker worker = new ValidateQueryWorker();

    @Test
    void taskDefName() {
        assertEquals("db_validate_query", worker.getTaskDefName());
    }

    @Test
    void returnsValidatedQueryMatchingInput() {
        String inputQuery = "SELECT * FROM employees";
        Task task = taskWith(Map.of("query", inputQuery, "queryType", "SELECT"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(inputQuery, result.getOutputData().get("validatedQuery"));
    }

    @Test
    void returnsIsReadOnlyTrue() {
        Task task = taskWith(Map.of("query", "SELECT 1", "queryType", "SELECT"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("isReadOnly"));
    }

    @Test
    void returnsFiveValidationChecks() {
        Task task = taskWith(Map.of("query", "SELECT 1", "queryType", "SELECT"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks =
                (List<Map<String, Object>>) result.getOutputData().get("validationChecks");
        assertNotNull(checks);
        assertEquals(5, checks.size());
    }

    @Test
    void allValidationChecksPassed() {
        Task task = taskWith(Map.of("query", "SELECT 1", "queryType", "SELECT"));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("allPassed"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks =
                (List<Map<String, Object>>) result.getOutputData().get("validationChecks");
        for (Map<String, Object> check : checks) {
            assertEquals(true, check.get("passed"));
        }
    }

    @Test
    void validationChecksHaveExpectedNames() {
        Task task = taskWith(Map.of("query", "SELECT 1", "queryType", "SELECT"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks =
                (List<Map<String, Object>>) result.getOutputData().get("validationChecks");
        List<String> checkNames = checks.stream()
                .map(c -> (String) c.get("check"))
                .toList();
        assertTrue(checkNames.contains("syntax_valid"));
        assertTrue(checkNames.contains("tables_exist"));
        assertTrue(checkNames.contains("no_mutation"));
        assertTrue(checkNames.contains("no_injection"));
        assertTrue(checkNames.contains("performance"));
    }

    @Test
    void handlesNullQuery() {
        Map<String, Object> input = new HashMap<>();
        input.put("query", null);
        input.put("queryType", "SELECT");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SELECT 1", result.getOutputData().get("validatedQuery"));
    }

    @Test
    void handlesMissingQueryType() {
        Task task = taskWith(Map.of("query", "SELECT 1"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("validationChecks"));
    }

    @Test
    void handlesBlankQuery() {
        Task task = taskWith(Map.of("query", "  ", "queryType", "SELECT"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("SELECT 1", result.getOutputData().get("validatedQuery"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
