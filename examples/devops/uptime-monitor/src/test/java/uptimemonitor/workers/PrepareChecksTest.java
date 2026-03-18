package uptimemonitor.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PrepareChecksTest {

    private final PrepareChecks worker = new PrepareChecks();

    @Test
    void taskDefName() {
        assertEquals("uptime_prepare_checks", worker.getTaskDefName());
    }

    @Test
    void generatesOneTaskPerEndpoint() {
        Task task = taskWith(List.of(
                endpoint("https://google.com", "Google", 200),
                endpoint("https://github.com", "GitHub", 200),
                endpoint("https://example.com", "Example", 200)
        ));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        var dynamicTasks = (List<Map<String, Object>>) result.getOutputData().get("dynamicTasks");
        @SuppressWarnings("unchecked")
        var dynamicTasksInput = (Map<String, Object>) result.getOutputData().get("dynamicTasksInput");

        assertEquals(3, dynamicTasks.size());
        assertEquals(3, dynamicTasksInput.size());
    }

    @Test
    void taskReferencesAreUnique() {
        Task task = taskWith(List.of(
                endpoint("https://a.com", "A", 200),
                endpoint("https://b.com", "B", 200)
        ));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var dynamicTasks = (List<Map<String, Object>>) result.getOutputData().get("dynamicTasks");

        Set<String> refs = new HashSet<>();
        for (var dt : dynamicTasks) {
            refs.add((String) dt.get("taskReferenceName"));
        }
        assertEquals(2, refs.size(), "Task reference names must be unique");
    }

    @Test
    void allTasksUseCorrectTaskName() {
        Task task = taskWith(List.of(endpoint("https://a.com", "A", 200)));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var dynamicTasks = (List<Map<String, Object>>) result.getOutputData().get("dynamicTasks");

        for (var dt : dynamicTasks) {
            assertEquals("uptime_check_endpoint", dt.get("name"));
            assertEquals("SIMPLE", dt.get("type"));
        }
    }

    @Test
    void inputPassedThroughCorrectly() {
        Task task = taskWith(List.of(
                endpoint("https://google.com", "Google", 200)
        ));

        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        var dynamicTasksInput = (Map<String, Object>) result.getOutputData().get("dynamicTasksInput");
        @SuppressWarnings("unchecked")
        var input = (Map<String, Object>) dynamicTasksInput.get("check_ep_0_ref");

        assertEquals("https://google.com", input.get("url"));
        assertEquals("Google", input.get("name"));
        assertEquals(200, input.get("expectedStatus"));
        assertEquals(5000, input.get("timeout"));
    }

    @Test
    void emptyEndpointsReturnsEmptyOutput() {
        Task task = taskWith(List.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        var dynamicTasks = (List<?>) result.getOutputData().get("dynamicTasks");
        assertTrue(dynamicTasks.isEmpty());
    }

    @Test
    void nullEndpointsReturnsEmptyOutput() {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of());

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        var dynamicTasks = (List<?>) result.getOutputData().get("dynamicTasks");
        assertTrue(dynamicTasks.isEmpty());
    }

    private Task taskWith(List<Map<String, Object>> endpoints) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of("endpoints", endpoints)));
        return task;
    }

    private Map<String, Object> endpoint(String url, String name, int expectedStatus) {
        return Map.of("url", url, "name", name, "expectedStatus", expectedStatus);
    }
}
