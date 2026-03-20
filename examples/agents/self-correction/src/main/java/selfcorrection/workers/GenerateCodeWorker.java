package selfcorrection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Generates code from a requirement description.
 * Returns a deterministic fibonacci function implementation.
 */
public class GenerateCodeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sc_generate_code";
    }

    @Override
    public TaskResult execute(Task task) {
        String requirement = (String) task.getInputData().get("requirement");
        if (requirement == null || requirement.isBlank()) {
            requirement = "";
        }

        System.out.println("  [sc_generate_code] Generating code for: " + requirement);

        String code = "function fibonacci(n) { if (n <= 1) return n; return fibonacci(n-1) + fibonacci(n-2); }";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("code", code);
        return result;
    }
}
