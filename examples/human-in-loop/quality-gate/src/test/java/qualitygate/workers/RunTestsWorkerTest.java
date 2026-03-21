package qualitygate.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RunTestsWorkerTest {

    @Test
    void taskDefName() {
        RunTestsWorker worker = new RunTestsWorker();
        assertEquals("qg_run_tests", worker.getTaskDefName());
    }

    @Test
    void returnsAllPassedTrue() {
        RunTestsWorker worker = new RunTestsWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("allPassed"));
    }

    @Test
    void returnsTestCounts() {
        RunTestsWorker worker = new RunTestsWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertEquals(42, result.getOutputData().get("totalTests"));
        assertEquals(42, result.getOutputData().get("passedTests"));
        assertEquals(0, result.getOutputData().get("failedTests"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        RunTestsWorker worker = new RunTestsWorker();
        Task task = taskWith(Map.of());

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("allPassed"));
        assertTrue(result.getOutputData().containsKey("totalTests"));
        assertTrue(result.getOutputData().containsKey("passedTests"));
        assertTrue(result.getOutputData().containsKey("failedTests"));
    }

    @Test
    void alwaysCompletes() {
        RunTestsWorker worker = new RunTestsWorker();
        Task task = taskWith(Map.of("extra", "data"));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("allPassed"));
    }

    @Test
    void isDeterministic() {
        RunTestsWorker worker = new RunTestsWorker();

        TaskResult result1 = worker.execute(taskWith(Map.of()));
        TaskResult result2 = worker.execute(taskWith(Map.of()));

        assertEquals(result1.getOutputData().get("allPassed"), result2.getOutputData().get("allPassed"));
        assertEquals(result1.getOutputData().get("totalTests"), result2.getOutputData().get("totalTests"));
        assertEquals(result1.getOutputData().get("passedTests"), result2.getOutputData().get("passedTests"));
        assertEquals(result1.getOutputData().get("failedTests"), result2.getOutputData().get("failedTests"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
