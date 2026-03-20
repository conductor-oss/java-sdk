package mapreduce.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapWorkerTest {

    private final MapWorker worker = new MapWorker();

    @Test
    void taskDefName() {
        assertEquals("mr_map", worker.getTaskDefName());
    }

    @Test
    void preparesTasksForMultipleLogFiles() {
        Task task = taskWith(Map.of("logFiles", List.of(
                Map.of("name", "api.log", "lineCount", 50000),
                Map.of("name", "auth.log", "lineCount", 30000),
                Map.of("name", "payment.log", "lineCount", 75000)
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dynamicTasks =
                (List<Map<String, Object>>) result.getOutputData().get("dynamicTasks");
        assertNotNull(dynamicTasks);
        assertEquals(3, dynamicTasks.size());

        // Verify first task
        assertEquals("mr_analyze_log", dynamicTasks.get(0).get("name"));
        assertEquals("log_0_ref", dynamicTasks.get(0).get("taskReferenceName"));
        assertEquals("SIMPLE", dynamicTasks.get(0).get("type"));

        // Verify second task
        assertEquals("mr_analyze_log", dynamicTasks.get(1).get("name"));
        assertEquals("log_1_ref", dynamicTasks.get(1).get("taskReferenceName"));

        // Verify third task
        assertEquals("log_2_ref", dynamicTasks.get(2).get("taskReferenceName"));
    }

    @Test
    void preparesInputMapForLogFiles() {
        Task task = taskWith(Map.of("logFiles", List.of(
                Map.of("name", "api.log", "lineCount", 50000),
                Map.of("name", "auth.log", "lineCount", 30000)
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> dynamicTasksInput =
                (Map<String, Map<String, Object>>) result.getOutputData().get("dynamicTasksInput");
        assertNotNull(dynamicTasksInput);
        assertEquals(2, dynamicTasksInput.size());

        // Verify input for first task
        Map<String, Object> input0 = dynamicTasksInput.get("log_0_ref");
        assertNotNull(input0);
        @SuppressWarnings("unchecked")
        Map<String, Object> logFile0 = (Map<String, Object>) input0.get("logFile");
        assertEquals("api.log", logFile0.get("name"));
        assertEquals(50000, logFile0.get("lineCount"));
        assertEquals(0, input0.get("index"));

        // Verify input for second task
        Map<String, Object> input1 = dynamicTasksInput.get("log_1_ref");
        assertNotNull(input1);
        @SuppressWarnings("unchecked")
        Map<String, Object> logFile1 = (Map<String, Object>) input1.get("logFile");
        assertEquals("auth.log", logFile1.get("name"));
        assertEquals(1, input1.get("index"));
    }

    @Test
    void preparesSingleLogFile() {
        Task task = taskWith(Map.of("logFiles", List.of(
                Map.of("name", "only.log", "lineCount", 10000)
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dynamicTasks =
                (List<Map<String, Object>>) result.getOutputData().get("dynamicTasks");
        assertEquals(1, dynamicTasks.size());
        assertEquals("log_0_ref", dynamicTasks.get(0).get("taskReferenceName"));

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> dynamicTasksInput =
                (Map<String, Map<String, Object>>) result.getOutputData().get("dynamicTasksInput");
        assertEquals(1, dynamicTasksInput.size());
        assertNotNull(dynamicTasksInput.get("log_0_ref"));
    }

    @Test
    void handlesEmptyLogFilesList() {
        Task task = taskWith(Map.of("logFiles", List.of()));
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
    void handlesNullLogFiles() {
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
        Task task1 = taskWith(Map.of("logFiles", List.of(
                Map.of("name", "a.log", "lineCount", 1000),
                Map.of("name", "b.log", "lineCount", 2000)
        )));
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of("logFiles", List.of(
                Map.of("name", "a.log", "lineCount", 1000),
                Map.of("name", "b.log", "lineCount", 2000)
        )));
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
