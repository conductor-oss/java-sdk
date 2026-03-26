package selfcorrection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Fixes code based on a diagnosis.
 * Returns deterministic fixed code with a negative input guard added.
 */
public class FixWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sc_fix";
    }

    @Override
    public TaskResult execute(Task task) {
        String code = (String) task.getInputData().get("code");
        if (code == null || code.isBlank()) {
            code = "";
        }

        String diagnosis = (String) task.getInputData().get("diagnosis");
        if (diagnosis == null || diagnosis.isBlank()) {
            diagnosis = "";
        }

        System.out.println("  [sc_fix] Applying fix based on diagnosis...");

        String fixedCode = "function fibonacci(n) { if (n < 0) throw new Error(\"Negative input\"); if (n <= 1) return n; return fibonacci(n-1) + fibonacci(n-2); }";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("fixedCode", fixedCode);
        result.getOutputData().put("changesMade", List.of("Added negative input guard"));
        return result;
    }
}
