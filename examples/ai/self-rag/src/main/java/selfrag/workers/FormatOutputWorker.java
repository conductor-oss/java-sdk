package selfrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Formats the final output when quality gate passes.
 * Returns {answer, sourceCount}.
 */
public class FormatOutputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sr_format_output";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [format] Packaging final response...");

        String answer = (String) task.getInputData().get("answer");
        List<Map<String, Object>> sources =
                (List<Map<String, Object>>) task.getInputData().get("sources");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer);
        result.getOutputData().put("sourceCount", sources.size());
        return result;
    }
}
