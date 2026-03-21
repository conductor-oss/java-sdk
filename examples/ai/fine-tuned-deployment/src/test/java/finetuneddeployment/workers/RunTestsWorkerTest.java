package finetuneddeployment.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RunTestsWorkerTest {

    private final RunTestsWorker worker = new RunTestsWorker();

    @Test
    void taskDefName() {
        assertEquals("ftd_run_tests", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void allTestsPass() {
        Task task = taskWith(new HashMap<>(Map.of(
                "endpoint", "https://staging.models.internal/support-bot-v3",
                "modelId", "support-bot-v3")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("true", result.getOutputData().get("allPassed"));

        List<Map<String, Object>> tests = (List<Map<String, Object>>) result.getOutputData().get("tests");
        assertEquals(4, tests.size());

        List<Map<String, Object>> failures = (List<Map<String, Object>>) result.getOutputData().get("failures");
        assertTrue(failures.isEmpty());
    }

    @Test
    void allPassedIsStringNotBoolean() {
        Task task = taskWith(new HashMap<>(Map.of(
                "endpoint", "https://staging.models.internal/test",
                "modelId", "test")));
        TaskResult result = worker.execute(task);

        Object allPassed = result.getOutputData().get("allPassed");
        assertInstanceOf(String.class, allPassed);
        assertEquals("true", allPassed);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testResultsContainExpectedChecks() {
        Task task = taskWith(new HashMap<>(Map.of(
                "endpoint", "https://staging.models.internal/test",
                "modelId", "test")));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> tests = (List<Map<String, Object>>) result.getOutputData().get("tests");
        assertEquals("latency_p99", tests.get(0).get("name"));
        assertTrue(tests.get(0).get("value").toString().contains("120ms"));
        assertEquals("accuracy_benchmark", tests.get(1).get("name"));
        assertEquals("94.2%", tests.get(1).get("value"));
        assertEquals("toxicity_check", tests.get(2).get("name"));
        assertEquals("0.01%", tests.get(2).get("value"));
        assertEquals("regression_suite", tests.get(3).get("name"));
        assertEquals("48/48 passed", tests.get(3).get("value"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
