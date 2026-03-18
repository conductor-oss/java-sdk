package functioncalling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Extracts a structured function call (name + arguments) from the LLM output
 * and validates it against the available function definitions.
 */
public class ExtractFunctionCallWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fc_extract_function_call";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> llmOutput = (Map<String, Object>) task.getInputData().get("llmOutput");
        Object functionDefinitions = task.getInputData().get("functionDefinitions");

        System.out.println("  [fc_extract_function_call] Extracting function call from LLM output");

        TaskResult result = new TaskResult(task);

        if (llmOutput == null || !llmOutput.containsKey("functionCall")) {
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("functionName", "unknown");
            result.getOutputData().put("arguments", Map.of());
            result.getOutputData().put("validated", false);
            return result;
        }

        Map<String, Object> functionCall = (Map<String, Object>) llmOutput.get("functionCall");
        String functionName = (String) functionCall.get("name");
        Object arguments = functionCall.get("arguments");

        if (functionName == null || functionName.isBlank()) {
            functionName = "unknown";
        }
        if (arguments == null) {
            arguments = Map.of();
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("functionName", functionName);
        result.getOutputData().put("arguments", arguments);
        result.getOutputData().put("validated", true);
        return result;
    }
}
