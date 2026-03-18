package workflowtesting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TeardownWorkerTest {

    private final TeardownWorker worker = new TeardownWorker();

    @Test
    void taskDefName() {
        assertEquals("wft_teardown", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("fixtures", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void cleanedUpIsTrue() {
        Task task = taskWith(Map.of("fixtures", Map.of()));
        TaskResult result = worker.execute(task);
        assertEquals(true, result.getOutputData().get("cleanedUp"));
    }

    @Test
    void resourcesReleasedContainsMockDb() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        List<String> released = (List<String>) result.getOutputData().get("resourcesReleased");
        assertTrue(released.contains("mockDb"));
    }

    @Test
    void resourcesReleasedContainsMockApi() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        List<String> released = (List<String>) result.getOutputData().get("resourcesReleased");
        assertTrue(released.contains("mockApi"));
    }

    @Test
    void resourcesReleasedContainsTestData() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        List<String> released = (List<String>) result.getOutputData().get("resourcesReleased");
        assertTrue(released.contains("testData"));
    }

    @Test
    void threeResourcesReleased() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        List<String> released = (List<String>) result.getOutputData().get("resourcesReleased");
        assertEquals(3, released.size());
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
