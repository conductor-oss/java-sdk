package selfcorrection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Runs tests against the provided code.
 * Returns a deterministic failure result indicating the code lacks negative input handling.
 */
public class RunTestsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sc_run_tests";
    }

    @Override
    public TaskResult execute(Task task) {
        String code = (String) task.getInputData().get("code");
        if (code == null || code.isBlank()) {
            code = "";
        }

        System.out.println("  [sc_run_tests] Running tests on code...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("testResult", "fail");
        result.getOutputData().put("errors", List.of("TypeError: Maximum call stack exceeded for negative input"));
        result.getOutputData().put("testsPassed", 0);
        result.getOutputData().put("testsFailed", 1);
        return result;
    }
}
