package streamprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

public class EmitResultsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "st_emit_results";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> aggregates = (List<Map<String, Object>>) task.getInputData().get("aggregates");
        if (aggregates == null) aggregates = List.of();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> anomalies = (List<Map<String, Object>>) task.getInputData().get("anomalies");
        if (anomalies == null) anomalies = List.of();

        String summary = "Stream processed: " + aggregates.size() + " windows, " + anomalies.size() + " anomalies detected";
        System.out.println("  [emit] " + summary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
        return result;
    }
}
