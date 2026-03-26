package eventreplaytesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Compares the actual replay result against the expected result.
 * Input: actual, expected, iteration
 * Output: match (boolean), actual, expected
 */
public class CompareResultWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rt_compare_result";
    }

    @Override
    public TaskResult execute(Task task) {
        String actual = (String) task.getInputData().get("actual");
        if (actual == null) {
            actual = "unknown";
        }

        String expected = (String) task.getInputData().get("expected");
        if (expected == null) {
            expected = "unknown";
        }

        int iteration = 0;
        Object iterObj = task.getInputData().get("iteration");
        if (iterObj instanceof Number) {
            iteration = ((Number) iterObj).intValue();
        }

        boolean match = actual.equals(expected);

        System.out.println("  [rt_compare_result] Iteration " + iteration
                + ": actual=" + actual + " expected=" + expected + " match=" + match);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("match", match);
        result.getOutputData().put("actual", actual);
        result.getOutputData().put("expected", expected);
        return result;
    }
}
