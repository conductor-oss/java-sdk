package dataaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Emits the final aggregation summary.
 * Input: report (list of strings), groupCount (int)
 * Output: summary ("Aggregation complete: N groups reported")
 */
public class EmitResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "agg_emit_results";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<String> report = (List<String>) task.getInputData().get("report");
        if (report == null) {
            report = List.of();
        }

        Object groupCountObj = task.getInputData().get("groupCount");
        int groupCount = 0;
        if (groupCountObj instanceof Number) {
            groupCount = ((Number) groupCountObj).intValue();
        }

        String summary = "Aggregation complete: " + groupCount + " groups reported";

        System.out.println("  [agg_emit_results] " + summary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
        return result;
    }
}
