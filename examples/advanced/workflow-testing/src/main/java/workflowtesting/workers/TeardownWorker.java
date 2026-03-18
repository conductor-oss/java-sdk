package workflowtesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Cleans up test fixtures after test execution.
 * Input: fixtures
 * Output: cleanedUp (boolean), resourcesReleased (list)
 */
public class TeardownWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wft_teardown";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [teardown] Cleaning up test fixtures");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("cleanedUp", true);
        result.getOutputData().put("resourcesReleased", List.of("mockDb", "mockApi", "testData"));
        return result;
    }
}
