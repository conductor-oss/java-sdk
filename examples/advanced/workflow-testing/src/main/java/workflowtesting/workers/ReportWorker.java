package workflowtesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Generates the test report summarizing the test suite run.
 * Input: testSuite, assertions, allPassed, teardownClean
 * Output: report (suite, result, totalAssertions, passedAssertions, teardownClean)
 */
public class ReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wft_report";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String testSuite = (String) task.getInputData().get("testSuite");
        if (testSuite == null) {
            testSuite = "unknown-suite";
        }

        Object allPassedRaw = task.getInputData().get("allPassed");
        boolean passed = Boolean.TRUE.equals(allPassedRaw)
                || "true".equals(String.valueOf(allPassedRaw));

        // Compute assertion counts from the actual assertions list instead of hardcoding
        int totalAssertions = 0;
        int passedAssertions = 0;
        Object assertionsRaw = task.getInputData().get("assertions");
        if (assertionsRaw instanceof List) {
            List<Map<String, Object>> assertions = (List<Map<String, Object>>) assertionsRaw;
            totalAssertions = assertions.size();
            passedAssertions = (int) assertions.stream()
                    .filter(a -> Boolean.TRUE.equals(a.get("passed"))
                            || "true".equals(String.valueOf(a.get("passed"))))
                    .count();
        }

        Object teardownCleanRaw = task.getInputData().get("teardownClean");
        boolean teardownClean = Boolean.TRUE.equals(teardownCleanRaw)
                || "true".equals(String.valueOf(teardownCleanRaw));

        System.out.println("  [report] Generating report -- suite \"" + testSuite + "\" "
                + (passed ? "PASSED" : "FAILED"));
        System.out.println("    Assertions: " + passedAssertions + "/" + totalAssertions + " passed");
        System.out.println("    Teardown clean: " + teardownClean);

        Map<String, Object> report = Map.of(
                "suite", testSuite,
                "result", passed ? "PASSED" : "FAILED",
                "totalAssertions", totalAssertions,
                "passedAssertions", passedAssertions,
                "teardownClean", teardownClean
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("report", report);
        return result;
    }
}
