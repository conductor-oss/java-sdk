package githubintegration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

/**
 * Runs CI checks on a pull request.
 * Input: repo, prNumber, sha
 * Output: checks, allPassed
 *
 * This worker is requires external setup — CI check execution requires
 * integration with GitHub Actions, Jenkins, or another CI system.
 */
public class RunChecksWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gh_run_checks";
    }

    @Override
    public TaskResult execute(Task task) {
        Object prNumber = task.getInputData().get("prNumber");
        System.out.println("  [checks] PR #" + prNumber + ": lint=passed, unit-tests=passed, build=passed");

        List<Map<String, Object>> checks = List.of(
                Map.of("name", "lint", "status", "passed"),
                Map.of("name", "unit-tests", "status", "passed"),
                Map.of("name", "build", "status", "passed"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("checks", checks);
        result.getOutputData().put("allPassed", true);
        return result;
    }
}
