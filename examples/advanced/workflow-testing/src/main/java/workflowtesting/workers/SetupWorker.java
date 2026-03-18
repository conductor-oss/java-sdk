package workflowtesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Prepares test fixtures for the test suite.
 * Input: testSuite, workflowUnderTest
 * Output: fixtures (mockDb, mockApi, testData)
 */
public class SetupWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wft_setup";
    }

    @Override
    public TaskResult execute(Task task) {
        String testSuite = (String) task.getInputData().get("testSuite");
        if (testSuite == null) {
            testSuite = "default-suite";
        }

        System.out.println("  [setup] Preparing fixtures for suite \"" + testSuite + "\"");

        Map<String, Object> mockDb = Map.of(
                "host", "localhost",
                "port", 5432,
                "dbName", "test_db"
        );
        Map<String, Object> mockApi = Map.of(
                "url", "http://localhost:9090",
                "ready", true
        );
        List<Map<String, Object>> testData = List.of(
                Map.of("id", 1, "value", "alpha"),
                Map.of("id", 2, "value", "beta")
        );

        Map<String, Object> fixtures = Map.of(
                "mockDb", mockDb,
                "mockApi", mockApi,
                "testData", testData
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("fixtures", fixtures);
        return result;
    }
}
