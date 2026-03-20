package tooluseconditional.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Handles code-category queries by performing a code interpreter tool.
 * Returns an answer with generated code, the code string, executionOutput,
 * language, and the toolUsed identifier.
 */
public class InterpreterWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tc_interpreter";
    }

    @Override
    public TaskResult execute(Task task) {
        String codeRequest = (String) task.getInputData().get("codeRequest");
        if (codeRequest == null || codeRequest.isBlank()) {
            codeRequest = "unknown code request";
        }

        String language = (String) task.getInputData().get("language");
        if (language == null || language.isBlank()) {
            language = "python";
        }

        String userQuery = (String) task.getInputData().get("userQuery");
        if (userQuery == null || userQuery.isBlank()) {
            userQuery = codeRequest;
        }

        System.out.println("  [tc_interpreter] Interpreting code request: " + codeRequest + " (language: " + language + ")");

        String code;
        String executionOutput;

        if ("python".equalsIgnoreCase(language)) {
            code = "def fibonacci(n):\n"
                    + "    if n <= 1:\n"
                    + "        return n\n"
                    + "    a, b = 0, 1\n"
                    + "    for _ in range(2, n + 1):\n"
                    + "        a, b = b, a + b\n"
                    + "    return b\n"
                    + "\n"
                    + "print(fibonacci(10))";
            executionOutput = "fibonacci(10) = 55";
        } else {
            code = "function fibonacci(n) {\n"
                    + "    if (n <= 1) return n;\n"
                    + "    let a = 0, b = 1;\n"
                    + "    for (let i = 2; i <= n; i++) {\n"
                    + "        [a, b] = [b, a + b];\n"
                    + "    }\n"
                    + "    return b;\n"
                    + "}\n"
                    + "\n"
                    + "console.log(fibonacci(10));";
            executionOutput = "fibonacci(10) = 55";
        }

        String answer = "Here is the generated " + language + " code for: " + codeRequest
                + "\n\n" + code + "\n\nOutput: " + executionOutput;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        result.getOutputData().put("code", code);
        result.getOutputData().put("executionOutput", executionOutput);
        result.getOutputData().put("language", language);
        result.getOutputData().put("toolUsed", "interpreter");
        return result;
    }
}
