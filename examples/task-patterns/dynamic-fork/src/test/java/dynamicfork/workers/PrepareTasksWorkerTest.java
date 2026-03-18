package dynamicfork.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrepareTasksWorkerTest {

    private final PrepareTasksWorker worker = new PrepareTasksWorker();

    @Test
    void taskDefName() {
        assertEquals("df_prepare_tasks", worker.getTaskDefName());
    }

    @Test
    void preparesTasksForMultipleUrls() {
        Task task = taskWith(Map.of("urls", List.of("https://a.com", "https://b.com", "https://c.com")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dynamicTasks =
                (List<Map<String, Object>>) result.getOutputData().get("dynamicTasks");
        assertNotNull(dynamicTasks);
        assertEquals(3, dynamicTasks.size());

        // Verify first task
        assertEquals("df_fetch_url", dynamicTasks.get(0).get("name"));
        assertEquals("fetch_0_ref", dynamicTasks.get(0).get("taskReferenceName"));
        assertEquals("SIMPLE", dynamicTasks.get(0).get("type"));

        // Verify second task
        assertEquals("df_fetch_url", dynamicTasks.get(1).get("name"));
        assertEquals("fetch_1_ref", dynamicTasks.get(1).get("taskReferenceName"));

        // Verify third task
        assertEquals("fetch_2_ref", dynamicTasks.get(2).get("taskReferenceName"));
    }

    @Test
    void preparesInputMapForMultipleUrls() {
        Task task = taskWith(Map.of("urls", List.of("https://a.com", "https://b.com")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> dynamicTasksInput =
                (Map<String, Map<String, Object>>) result.getOutputData().get("dynamicTasksInput");
        assertNotNull(dynamicTasksInput);
        assertEquals(2, dynamicTasksInput.size());

        // Verify input for first task
        Map<String, Object> input0 = dynamicTasksInput.get("fetch_0_ref");
        assertNotNull(input0);
        assertEquals("https://a.com", input0.get("url"));
        assertEquals(0, input0.get("index"));

        // Verify input for second task
        Map<String, Object> input1 = dynamicTasksInput.get("fetch_1_ref");
        assertNotNull(input1);
        assertEquals("https://b.com", input1.get("url"));
        assertEquals(1, input1.get("index"));
    }

    @Test
    void preparesSingleUrl() {
        Task task = taskWith(Map.of("urls", List.of("https://only.com")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dynamicTasks =
                (List<Map<String, Object>>) result.getOutputData().get("dynamicTasks");
        assertEquals(1, dynamicTasks.size());
        assertEquals("fetch_0_ref", dynamicTasks.get(0).get("taskReferenceName"));

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> dynamicTasksInput =
                (Map<String, Map<String, Object>>) result.getOutputData().get("dynamicTasksInput");
        assertEquals(1, dynamicTasksInput.size());
        assertEquals("https://only.com", dynamicTasksInput.get("fetch_0_ref").get("url"));
    }

    @Test
    void handlesEmptyUrlsList() {
        Task task = taskWith(Map.of("urls", List.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dynamicTasks =
                (List<Map<String, Object>>) result.getOutputData().get("dynamicTasks");
        assertNotNull(dynamicTasks);
        assertTrue(dynamicTasks.isEmpty());

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> dynamicTasksInput =
                (Map<String, Map<String, Object>>) result.getOutputData().get("dynamicTasksInput");
        assertNotNull(dynamicTasksInput);
        assertTrue(dynamicTasksInput.isEmpty());
    }

    @Test
    void handlesNullUrls() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dynamicTasks =
                (List<Map<String, Object>>) result.getOutputData().get("dynamicTasks");
        assertNotNull(dynamicTasks);
        assertTrue(dynamicTasks.isEmpty());
    }

    @Test
    void taskReferencesAreDeterministic() {
        Task task1 = taskWith(Map.of("urls", List.of("https://a.com", "https://b.com")));
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of("urls", List.of("https://a.com", "https://b.com")));
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("dynamicTasks"),
                result2.getOutputData().get("dynamicTasks"));
        assertEquals(result1.getOutputData().get("dynamicTasksInput"),
                result2.getOutputData().get("dynamicTasksInput"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
