package runbookautomation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteStepWorkerTest {

    private final ExecuteStepWorker worker = new ExecuteStepWorker();

    @Test
    void taskDefName() {
        assertEquals("ra_execute_step", worker.getTaskDefName());
    }

    @Test
    void executesRealShellCommand() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("runbookId", "RB-TEST");
        input.put("command", "echo hello-world");
        input.put("timeoutSeconds", 5);
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("executed"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        assertNotNull(results);
        assertEquals(1, results.size());

        Map<String, Object> stepResult = results.get(0);
        assertEquals(0, stepResult.get("exitCode"));
        assertTrue(((String) stepResult.get("stdout")).contains("hello-world"));
    }

    @Test
    void capturesStdoutAndStderr() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("runbookId", "RB-TEST");
        input.put("command", "echo stdout-output && echo stderr-output >&2");
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        Map<String, Object> stepResult = results.get(0);

        assertTrue(((String) stepResult.get("stdout")).contains("stdout-output"));
        assertTrue(((String) stepResult.get("stderr")).contains("stderr-output"));
    }

    @Test
    void reportsNonZeroExitCode() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("runbookId", "RB-TEST");
        input.put("command", "exit 42");
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(42, results.get(0).get("exitCode"));
        assertEquals(1, ((Number) result.getOutputData().get("failureCount")).intValue());
    }

    @Test
    void executesMultipleSteps() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("runbookId", "RB-TEST");
        input.put("steps", List.of(
                Map.of("name", "step1", "command", "echo first", "timeoutSeconds", 5),
                Map.of("name", "step2", "command", "echo second", "timeoutSeconds", 5)
        ));
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        assertEquals(2, ((Number) result.getOutputData().get("stepCount")).intValue());
        assertEquals(2, ((Number) result.getOutputData().get("successCount")).intValue());
        assertEquals(0, ((Number) result.getOutputData().get("failureCount")).intValue());
    }

    @Test
    void reportsTotalDurationMs() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("runbookId", "RB-TEST");
        input.put("command", "echo fast");
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("totalDurationMs"));
        assertTrue(((Number) result.getOutputData().get("totalDurationMs")).longValue() >= 0);
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("executed"));
    }

    @Test
    void supportsPipeCommands() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("runbookId", "RB-TEST");
        input.put("command", "echo 'line1\nline2\nline3' | wc -l");
        task.setInputData(input);

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> results = (List<Map<String, Object>>) result.getOutputData().get("results");
        assertEquals(0, results.get(0).get("exitCode"));
        String stdout = ((String) results.get(0).get("stdout")).trim();
        assertFalse(stdout.isBlank(), "Should have output from pipe command");
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
