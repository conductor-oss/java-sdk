package cicdpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class UnitTestTest {

    private final UnitTest worker = new UnitTest();

    @Test
    void taskDefName() {
        assertEquals("cicd_unit_test", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith("BLD-100001");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsPassed() {
        Task task = taskWith("BLD-100001");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("passed"));
        assertTrue(((Number) result.getOutputData().get("passed")).intValue() >= 0);
    }

    @Test
    void outputContainsFailed() {
        Task task = taskWith("BLD-100001");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("failed"));
        assertTrue(((Number) result.getOutputData().get("failed")).intValue() >= 0);
    }

    @Test
    void javaVersionCheckPasses() {
        // Without a build directory, the worker runs "java -version" as a smoke test
        Task task = taskWith("BLD-100001");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("java-version", result.getOutputData().get("tool"));
        // java -version should succeed on any machine with JDK installed
        assertEquals(1, ((Number) result.getOutputData().get("passed")).intValue());
        assertEquals(0, ((Number) result.getOutputData().get("failed")).intValue());
    }

    @Test
    void outputContainsDurationMs() {
        Task task = taskWith("BLD-100001");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("durationMs"));
        assertTrue(((Number) result.getOutputData().get("durationMs")).longValue() >= 0);
    }

    @Test
    void outputContainsTestOutput() {
        Task task = taskWith("BLD-100001");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("testOutput"));
    }

    private Task taskWith(String buildId) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("buildId", buildId);
        task.setInputData(input);
        return task;
    }
}
