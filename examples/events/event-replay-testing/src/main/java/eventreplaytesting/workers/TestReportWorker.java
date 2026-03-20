package eventreplaytesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Generates a final test report after all replay iterations.
 * Input: testSuiteId, totalReplayed
 * Output: status, passCount, failCount
 */
public class TestReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rt_test_report";
    }

    @Override
    public TaskResult execute(Task task) {
        String testSuiteId = (String) task.getInputData().get("testSuiteId");
        if (testSuiteId == null) {
            testSuiteId = "unknown";
        }

        int totalReplayed = 0;
        Object totalObj = task.getInputData().get("totalReplayed");
        if (totalObj instanceof Number) {
            totalReplayed = ((Number) totalObj).intValue();
        }

        System.out.println("  [rt_test_report] Generating report for suite: " + testSuiteId
                + " (" + totalReplayed + " events replayed)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "all_passed");
        result.getOutputData().put("passCount", totalReplayed);
        result.getOutputData().put("failCount", 0);
        return result;
    }
}
