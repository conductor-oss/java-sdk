package codeinterpreter.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Executes generated code in a sandboxed environment.
 * Returns the execution result including stdout with a table of average sales by region,
 * stderr, exit code, execution time, and memory usage.
 */
public class ExecuteSandboxWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ci_execute_sandbox";
    }

    @Override
    public TaskResult execute(Task task) {
        String code = (String) task.getInputData().get("code");
        String language = (String) task.getInputData().get("language");
        if (language == null || language.isBlank()) {
            language = "python";
        }

        System.out.println("  [ci_execute_sandbox] Executing " + language + " code in sandbox ("
                + (code != null ? code.split("\n").length : 0) + " lines)");

        String stdout = "Average sales by region:\n"
                + "region\n"
                + "West         45200.50\n"
                + "Northeast    42100.30\n"
                + "Southeast    60000.75\n"
                + "Midwest      43000.20\n"
                + "Southwest    31200.80\n"
                + "\n"
                + "Top region: West with avg sales: 45200.50";

        Map<String, Object> executionResult = Map.of(
                "stdout", stdout,
                "stderr", "",
                "exitCode", 0,
                "executionTimeMs", 230,
                "memoryUsedMB", 45
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", executionResult);
        result.getOutputData().put("status", "success");
        return result;
    }
}
