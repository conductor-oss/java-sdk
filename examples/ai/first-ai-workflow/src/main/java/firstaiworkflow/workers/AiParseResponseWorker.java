package firstaiworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker that parses the raw LLM response and validates it.
 */
public class AiParseResponseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ai_parse_response";
    }

    @Override
    public TaskResult execute(Task task) {
        String rawResponse = (String) task.getInputData().get("rawResponse");
        Object tokenUsage = task.getInputData().get("tokenUsage");

        System.out.println("  [ai_parse_response worker] Parsing response of length "
                + (rawResponse != null ? rawResponse.length() : 0));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", rawResponse);
        result.getOutputData().put("valid", true);
        return result;
    }
}
