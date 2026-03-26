package codeinterpreter.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteSandboxWorkerTest {

    private final ExecuteSandboxWorker worker = new ExecuteSandboxWorker();

    @Test
    void taskDefName() {
        assertEquals("ci_execute_sandbox", worker.getTaskDefName());
    }

    @Test
    void returnsSuccessStatus() {
        Task task = taskWith(Map.of(
                "code", "print('hello')",
                "language", "python"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("status"));
    }

    @Test
    void resultContainsStdoutWithRegionData() {
        Task task = taskWith(Map.of(
                "code", "import pandas as pd\ndf.groupby('region')['sales'].mean()",
                "language", "python"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> execResult = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(execResult);

        String stdout = (String) execResult.get("stdout");
        assertTrue(stdout.contains("West"));
        assertTrue(stdout.contains("45200.50"));
        assertTrue(stdout.contains("Southwest"));
    }

    @Test
    void resultContainsEmptyStderr() {
        Task task = taskWith(Map.of("code", "print(1)", "language", "python"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> execResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals("", execResult.get("stderr"));
    }

    @Test
    void resultContainsZeroExitCode() {
        Task task = taskWith(Map.of("code", "x = 1", "language", "python"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> execResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals(0, execResult.get("exitCode"));
    }

    @Test
    void resultContainsExecutionMetrics() {
        Task task = taskWith(Map.of("code", "x = 1", "language", "python"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> execResult = (Map<String, Object>) result.getOutputData().get("result");
        assertEquals(230, execResult.get("executionTimeMs"));
        assertEquals(45, execResult.get("memoryUsedMB"));
    }

    @Test
    void handlesNullCode() {
        Map<String, Object> input = new HashMap<>();
        input.put("code", null);
        input.put("language", "python");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("success", result.getOutputData().get("status"));
    }

    @Test
    void handlesMissingLanguageDefaultsToPython() {
        Task task = taskWith(Map.of("code", "print(1)"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("result"));
    }

    @Test
    void stdoutContainsAllFiveRegions() {
        Task task = taskWith(Map.of("code", "analysis code", "language", "python"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> execResult = (Map<String, Object>) result.getOutputData().get("result");
        String stdout = (String) execResult.get("stdout");
        assertTrue(stdout.contains("West"));
        assertTrue(stdout.contains("Northeast"));
        assertTrue(stdout.contains("Southeast"));
        assertTrue(stdout.contains("Midwest"));
        assertTrue(stdout.contains("Southwest"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
