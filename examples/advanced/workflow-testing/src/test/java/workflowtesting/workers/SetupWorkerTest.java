package workflowtesting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SetupWorkerTest {

    private final SetupWorker worker = new SetupWorker();

    @Test
    void taskDefName() {
        assertEquals("wft_setup", worker.getTaskDefName());
    }

    @Test
    void completesSuccessfully() {
        Task task = taskWith(Map.of("testSuite", "my-suite", "workflowUnderTest", "wf1"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void fixturesContainMockDb() {
        Task task = taskWith(Map.of("testSuite", "suite-a"));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> fixtures = (Map<String, Object>) result.getOutputData().get("fixtures");
        assertNotNull(fixtures.get("mockDb"));
        @SuppressWarnings("unchecked")
        Map<String, Object> mockDb = (Map<String, Object>) fixtures.get("mockDb");
        assertEquals("localhost", mockDb.get("host"));
        assertEquals(5432, mockDb.get("port"));
        assertEquals("test_db", mockDb.get("dbName"));
    }

    @Test
    void fixturesContainMockApi() {
        Task task = taskWith(Map.of("testSuite", "suite-b"));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> fixtures = (Map<String, Object>) result.getOutputData().get("fixtures");
        @SuppressWarnings("unchecked")
        Map<String, Object> mockApi = (Map<String, Object>) fixtures.get("mockApi");
        assertEquals("http://localhost:9090", mockApi.get("url"));
        assertEquals(true, mockApi.get("ready"));
    }

    @Test
    void fixturesContainTestData() {
        Task task = taskWith(Map.of("testSuite", "suite-c"));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> fixtures = (Map<String, Object>) result.getOutputData().get("fixtures");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> testData = (List<Map<String, Object>>) fixtures.get("testData");
        assertEquals(2, testData.size());
        assertEquals("alpha", testData.get(0).get("value"));
        assertEquals("beta", testData.get(1).get("value"));
    }

    @Test
    void handlesNullTestSuite() {
        Map<String, Object> input = new HashMap<>();
        input.put("testSuite", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("fixtures"));
    }

    @Test
    void handlesEmptyInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("fixtures"));
    }

    @Test
    void fixturesStructureIsComplete() {
        Task task = taskWith(Map.of("testSuite", "full-check"));
        TaskResult result = worker.execute(task);
        @SuppressWarnings("unchecked")
        Map<String, Object> fixtures = (Map<String, Object>) result.getOutputData().get("fixtures");
        assertEquals(3, fixtures.size());
        assertTrue(fixtures.containsKey("mockDb"));
        assertTrue(fixtures.containsKey("mockApi"));
        assertTrue(fixtures.containsKey("testData"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
