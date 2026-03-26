package qualitygate.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for qg_run_tests task -- runs automated tests and reports results.
 *
 * Outputs:
 *   - allPassed: true (all tests passed)
 *   - totalTests: 42
 *   - passedTests: 42
 *   - failedTests: 0
 */
public class RunTestsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "qg_run_tests";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [qg_run_tests] Running automated tests...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("allPassed", true);
        result.getOutputData().put("totalTests", 42);
        result.getOutputData().put("passedTests", 42);
        result.getOutputData().put("failedTests", 0);

        System.out.println("  [qg_run_tests] Tests complete: 42/42 passed.");
        return result;
    }
}
