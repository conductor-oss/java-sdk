package workflowtesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes the workflow under test using the provided fixtures.
 * Processes the testData from the fixtures and produces actual output
 * based on the input data rather than hardcoded values.
 *
 * Input: fixtures (map with mockDb, mockApi, testData), workflowUnderTest
 * Output: actualOutput (processed count, status, results list), executionTimeMs
 */
public class ExecuteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wft_execute";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String workflowUnderTest = (String) task.getInputData().get("workflowUnderTest");
        if (workflowUnderTest == null) {
            workflowUnderTest = "unknown_workflow";
        }

        long startNanos = System.nanoTime();

        // Extract fixtures and process the testData
        Object fixturesObj = task.getInputData().get("fixtures");
        List<Map<String, Object>> testData = List.of();
        if (fixturesObj instanceof Map) {
            Object tdObj = ((Map<String, Object>) fixturesObj).get("testData");
            if (tdObj instanceof List) {
                testData = (List<Map<String, Object>>) tdObj;
            }
        }

        // Process each test data item by appending "_processed" to its value
        List<String> results = new ArrayList<>();
        for (Map<String, Object> item : testData) {
            Object value = item.get("value");
            if (value != null) {
                results.add(value + "_processed");
            }
        }

        long executionTimeMs = (System.nanoTime() - startNanos) / 1_000_000;

        String status = results.isEmpty() ? "NO_DATA" : "SUCCESS";

        System.out.println("  [execute] Running workflow \"" + workflowUnderTest
                + "\" -- processed " + results.size() + " items in " + executionTimeMs + "ms");

        Map<String, Object> actualOutput = new HashMap<>();
        actualOutput.put("processed", results.size());
        actualOutput.put("status", status);
        actualOutput.put("results", results);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("actualOutput", actualOutput);
        result.getOutputData().put("executionTimeMs", executionTimeMs);
        return result;
    }
}
