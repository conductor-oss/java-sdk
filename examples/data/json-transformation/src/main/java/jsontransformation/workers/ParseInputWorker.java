package jsontransformation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Parses the incoming source JSON and counts its top-level fields.
 * Input: sourceJson (map)
 * Output: parsed (the sourceJson as-is), fieldCount (number of top-level keys)
 */
public class ParseInputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "jt_parse_input";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        Map<String, Object> sourceJson = (Map<String, Object>) task.getInputData().get("sourceJson");
        if (sourceJson == null) {
            sourceJson = Map.of();
        }

        int fieldCount = sourceJson.size();
        System.out.println("  [jt_parse_input] Parsed source JSON with " + fieldCount + " top-level fields");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("parsed", sourceJson);
        result.getOutputData().put("fieldCount", fieldCount);
        return result;
    }
}
